package org.datalift.converter;


import java.net.URI;
import java.util.Arrays;
import java.util.List;

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

import com.sun.jersey.api.NotFoundException;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;


public class SimplePublisher extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String MODULE_NAME = "simple-publisher";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

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
            throw new TechnicalException("project.manager.not.available");
        }
        this.sparqlEndpoint = configuration.getBean(SparqlEndpoint.class);
        if (this.sparqlEndpoint == null) {
            throw new TechnicalException("sparql.endpoint.not.available");
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
                throw new TechnicalException(e);
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
            throw new NotFoundException("No RDF source found", projectId);
        }
        Response response = null;
        try {
            Source origin = null;
            TransformedRdfSource current = src;
            while (current != null) {
                origin = current.getParent();
                current = (origin instanceof TransformedRdfSource)?
                                            (TransformedRdfSource)origin: null;
            }
            URI targetGraph = (origin != null)? new URI(origin.getUri()):
                                                projectId;
            List<String> constructs = Arrays.asList(
                            "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <"
                                + src.getTargetGraph() + "> { ?s ?p ?o . } }");
            RdfUtils.convert(this.internRepository, constructs,
                             this.publicRepository, targetGraph);

            response = this.sparqlEndpoint.executeQuery(
                                        "SELECT * WHERE { GRAPH <"
                                            + projectId + "> { ?s ?p ?o . } }",
                                        uriInfo, request, acceptHdr).build();
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
            log.error(error.getMessage(), e);
            throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaType.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
        }
        return response;
    }
}
