/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.converter;


import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import com.sun.jersey.api.view.Viewable;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;


public class RdfConverter extends BaseModule implements ProjectModule
{
    private final static String MODULE_NAME = "rdfconverter";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private ProjectManager projectManager = null;
    private SparqlEndpoint sparqlEndpoint = null;
    private Repository internRepository = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public RdfConverter() {
        super(MODULE_NAME, true);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);

        this.internRepository = configuration.getInternalRepository();
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
                return new URI(this.getName() + "?project=" + p.getUri());
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
    public Response getIndexPage(@QueryParam("project") String projectId,
                                 @Context UriInfo uriInfo) {
        Project p;
        try {
            p = this.projectManager.findProject(new URL(projectId).toURI());
        } 
        catch (Exception e) {
            throw new RuntimeException("Could not find project with URI " + projectId, e);
        }
        TransformedRdfSource src = null;
        for (Source s : p.getSources()) {
            if (s instanceof TransformedRdfSource) {
                src = (TransformedRdfSource)s;
            }
        }
        if (src == null) {
            throw new RuntimeException("Could not find rdf source in project with URI " + projectId);
        }
        return Response.ok(this.newViewable("/constructQueries.vm", p)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response convertRdfSource(@FormParam("project") URI projectId,
                                     @FormParam("query[]") List<String> query,
                                     @Context UriInfo uriInfo,
                                     @Context Request request,
                                     @HeaderParam(ACCEPT) String acceptHdr) {
        if ((query == null) || (query.size() == 0)) {
            TechnicalException error =
                            new TechnicalException("ws.missing.param", "query");
            throw new WebApplicationException(error,
                            Response.status(Status.PRECONDITION_FAILED)
                                    .entity(error.getLocalizedMessage())
                                    .type(MediaType.TEXT_HTML).build());
        }
        TransformedRdfSource src = null;
        Project p = this.projectManager.findProject(projectId);
        if (p != null) {
            for (Source s : p.getSources()) {
                if (s instanceof CsvSource) {
                    src = (TransformedRdfSource)s;
                    // Continue to get last RDF source in project...
                }
            }
        }
        if (src == null) {
            throw new EntityNotFoundException("No RDF source found");
        }
        Response response = null;
        try {
            String srcName = " (RDF #" + p.getSources().size() + ')';
            URI targetGraph = new URI(src.getUri()
                                      + '/' + StringUtils.urlify(srcName));
            RdfUtils.convert(this.internRepository, query,
                             this.internRepository, targetGraph);
            p.addSource(this.projectManager.newTransformedRdfSource(
                                    targetGraph, src.getTitle() + srcName,
                                    targetGraph, src));
            this.projectManager.saveProject(p);

            response = this.sparqlEndpoint.executeQuery(
                                "SELECT * WHERE { GRAPH <"
                                    + targetGraph + "> { ?s ?p ?o . } }",
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

    /**
     * Return a viewable for the specified template, populated with the
     * specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated viewable.
     */
    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }	
}
