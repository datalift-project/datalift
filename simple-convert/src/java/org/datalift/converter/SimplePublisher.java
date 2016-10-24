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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.datalift.fwk.async.Operation;
import org.datalift.fwk.async.Parameter;
import org.datalift.fwk.async.ParameterType;
import org.datalift.fwk.async.Parameters;
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
public class SimplePublisher extends BaseConverterModule implements Operation
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
        if (! UriParam.isSet(projectId)) {
            this.throwInvalidParamError(PROJECT_ID_PARAM, null);
        }
        if (! UriParam.isSet(sourceId)) {
            this.throwInvalidParamError(SOURCE_ID_PARAM, null);
        }
        Project p = this.getProject(projectId.toUri(PROJECT_ID_PARAM));
        Parameters params = this.getBlankParameters();
        params.setValue("project", projectId.toUri().toString());
        params.setValue("source", sourceId.toUri().toString());
        params.setValue("targetGraph", targetGraphParam.toUri().toString());
        params.setValue("repository", repository);
        params.setValue("overwrite", Boolean.toString(overwrite));
        try {
            this.execute(params);
        } catch (Exception e) {
            this.handleInternalError(e);
        }
        return Response.seeOther(URI.create(p.getUri() + "#source")).build();
    }

    //-------------------------------------------------------------------------
    // Operation contract support
    //-------------------------------------------------------------------------
    
    @Override
    public URI getOperationId() {
        return URI.create(OPERATION_ID);
    }

    @Override
    public void execute(Parameters params) throws Exception {
        Date start = new Date();
        URI projectId = URI.create(params.getProjectValue());
        URI sourceId = URI.create(params.getValue("source"));
        URI targetGraph = URI.create(params.getValue("targetGraph"));
        String repository = params.getValue("repository");
        boolean overwrite = Boolean.parseBoolean(params.getValue("overwrite"));
        Configuration cfg = Configuration.getDefault();
        Repository pub = null;
        try {
            pub = (isSet(repository))? cfg.getRepository(repository):
                                       cfg.getDataRepository();
        }
        catch (Exception e) {
            this.throwInvalidParamError(REPOSITORY_PARAM, repository);
        }
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            TransformedRdfSource in = (TransformedRdfSource)
                            (p.getSource(sourceId));
            if (in == null) {
                throw new ObjectNotFoundException("project.source.not.found",
                                                  projectId, sourceId);
            }
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
                                                projectId;
            }
            // Publish input source triples in target repository.
            RdfUtils.upload(in, pub, targetGraph, null, overwrite);
            // Notify all access controllers (if any) that new graphs appeared.
            for (AccessController acs : cfg.getBeans(AccessController.class)) {
                try {
                    acs.refresh();
                }
                catch (Exception e) { /* Ignore... */ }
            }
            Date end = new Date();
            // Declare event
            this.projectManager.saveOutputEvent(p, this.getOperationId(),
                    params.getValues(), start, end, null, sourceId);
        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }
    
    @Override
    public Parameters getBlankParameters() {
        Collection<Parameter> paramList = new ArrayList<Parameter>();
        paramList.add(new Parameter("project",
                "ws.param.project", ParameterType.project));
        paramList.add(new Parameter("source",
                "ws.param.source", ParameterType.input_source));
        paramList.add(new Parameter("repository",
                "ws.param.repository", ParameterType.visible));
        paramList.add(new Parameter("targetGraph",
                "ws.param.targetGraph", ParameterType.visible));
        paramList.add(new Parameter("overwrite",
                "ws.param.overwrite", ParameterType.visible));
        return new Parameters(paramList);
    }
}
