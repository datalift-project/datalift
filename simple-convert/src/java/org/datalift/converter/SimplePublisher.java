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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.AccessController;
import org.datalift.fwk.util.web.UriParam;
import org.datalift.fwk.view.TemplateModel;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.isSet;


/**
 * A {@link ProjectModule project module} that copies the RDF triples
 * of a source from the internal repository to the public repository.
 *
 * @author lbihanic
 */
@Path(SimplePublisher.MODULE_NAME)
public class SimplePublisher extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "simple-publisher";
    
    public final static String OPERATION_ID =
            "http://www.datalift.org/core/converter/operation/" + MODULE_NAME;

    /* Web service parameter names. */
    protected final static String REPOSITORY_PARAM      = "store";

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public SimplePublisher() {
        super(MODULE_NAME, 10010, SourceType.TransformedRdfSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam(PROJECT_ID_PARAM) URI projectId) {
        TemplateModel view = this.getProjectView("publisher.vm", projectId);
        view.put("repositories", Configuration.getDefault()
                                              .getRepositories(true));
        return Response.ok(view, TEXT_HTML_UTF8).build();
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response publishRdfSource(
                        @FormParam(PROJECT_ID_PARAM)  UriParam projectId,
                        @FormParam(SOURCE_ID_PARAM)   UriParam sourceId,
                        @FormParam(REPOSITORY_PARAM)  String repository,
                        @FormParam(GRAPH_URI_PARAM)   UriParam targetGraphParam,
                        @FormParam(OVERWRITE_GRAPH_PARAM) boolean overwrite,
                        @Context UriInfo uriInfo,
                        @Context Request request,
                        @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Date eventStart = new Date();
        if (! UriParam.isSet(projectId)) {
            this.throwInvalidParamError(PROJECT_ID_PARAM, null);
        }
        if (! UriParam.isSet(sourceId)) {
            this.throwInvalidParamError(SOURCE_ID_PARAM, null);
        }
        Configuration cfg = Configuration.getDefault();
        Repository pub = null;
        try {
            pub = (isSet(repository))? cfg.getRepository(repository):
                                       cfg.getDataRepository();
        }
        catch (Exception e) {
            this.throwInvalidParamError(REPOSITORY_PARAM, repository);
        }
        Response response = null;

        try {
            // Retrieve project.
            Project p = this.getProject(projectId.toUri(PROJECT_ID_PARAM));
            // Load input source.
            TransformedRdfSource in = (TransformedRdfSource)
                            (p.getSource(sourceId.toUri(SOURCE_ID_PARAM)));
            if (in == null) {
                throw new ObjectNotFoundException("project.source.not.found",
                                                  projectId, sourceId);
            }
            URI targetGraph = targetGraphParam.toUri(GRAPH_URI_PARAM);
            if (targetGraph == null) {
                // Get the source (CSV, RDF/XML, database...) at the origin of
                // the transformations and use its name as target named graph.
                Source origin = null;
                TransformedRdfSource current = in;
                while (current != null) {
                    origin = current.getParent();
                    current = (origin instanceof TransformedRdfSource)?
                                                (TransformedRdfSource)origin: null;
                }
                targetGraph = (origin != null)? new URI(origin.getUri()):
                                                projectId.toUri();
            }
            // Publish input source triples in target repository.
            if (overwrite) {
                RdfUtils.clearGraph(pub, targetGraph);
            }
            RdfUtils.upload(in, pub, targetGraph, null, false);
            // Notify all access controllers (if any) that new graphs appeared.
            for (AccessController acs : cfg.getBeans(AccessController.class)) {
                try {
                    acs.refresh();
                }
                catch (Exception e) { /* Ignore... */ }
            }
            // Display generated triples.
            response = this.displayGraph(pub, targetGraph,
                                         uriInfo, request, acceptHdr);
            //save event
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("projectId", projectId.toUri().toString());
            parameters.put("sourceId", sourceId.toUri().toString());
            parameters.put("repository", repository);
            parameters.put("targetGraphParam", targetGraphParam.toUri().toString());
            parameters.put("overwrite", Boolean.toString(overwrite));
            URI operation = URI.create(OPERATION_ID);
            this.projectManager.saveOutputEvent(p, operation, parameters,
                    eventStart, new Date(), null, sourceId.toUri());
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }
}
