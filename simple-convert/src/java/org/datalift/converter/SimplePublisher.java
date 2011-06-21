package org.datalift.converter;


import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.core.rdf.RdfUtils;
import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;


public class SimplePublisher extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String MODULE_NAME = "simple-publisher";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private ProjectManager projectManager = null;
    private SparqlEndpoint sparqlEndpoint = null;
    private Repository internRepository = null;
    private Repository publicRepository = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public SimplePublisher() {
        super(MODULE_NAME, true);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        super.init(configuration);

        this.internRepository = configuration.getInternalRepository();
        this.publicRepository = configuration.getDataRepository();

        this.projectManager = configuration.getBean(ProjectManager.class);
        if (this.projectManager == null) {
            throw new RuntimeException("Could not retrieve Project Manager");
        }
        this.sparqlEndpoint = configuration.getBean(SparqlEndpoint.class);
        if (this.sparqlEndpoint == null) {
            throw new RuntimeException("Could not retrieve SPARQL endpoint");
        }
    }

    //-------------------------------------------------------------------------
    // ProjectModule contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @return <code>true</code> if the project contains at least one
     *         source of type {@link TransformedRdfSource}.
     */
    @Override
    public URI canHandle(Project p) {
        boolean hasRdfSource = false;
        for (Source s : p.getSources()) {
            if (s instanceof TransformedRdfSource) {
                hasRdfSource = true;
                break;
            }
        }
        URI projectPage = null;
        if (hasRdfSource) {
            try {
                projectPage = new URI(this.getName() + "?project=" + p.getUri());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return projectPage;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public Response publishProject(
                        @QueryParam("project") URI projectId,
                        @Context UriInfo uriInfo,
                        @Context Request request,
                        @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        TransformedRdfSource src = null;
        Project p = this.projectManager.findProject(projectId);
        if (p != null) {
            for (Source s : p.getSources()) {
                if (s instanceof TransformedRdfSource) {
                    src = (TransformedRdfSource)s;
                    // Continue to find last RDF source available...
                }
            }
        }
        if (src == null) {
            throw new EntityNotFoundException("No RDF source found");
        }
        Response response = null;
        try {
            List<String> constructs = Arrays.asList(
                            "CONSTRUCT ?s ?p ?o WHERE { GRAPH <"
                                + src.getTargetGraph() + "> { ?s ?p ?o . } }");
            RdfUtils.convert(this.internRepository, constructs,
                             this.publicRepository, projectId);

            response = this.sparqlEndpoint.executeQuery(
                                        "SELECT * WHERE { GRAPH <"
                                            + projectId + "> { ?s ?p ?o . } }",
                                        uriInfo, request, acceptHdr).build();
        }
        catch (Exception e) {
            response = Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity(e.getLocalizedMessage())
                               .type(MediaType.TEXT_PLAIN_TYPE)
                               .build();
        }
        return response;
    }
}
