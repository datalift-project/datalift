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
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.async.Operation;
import org.datalift.fwk.async.Parameter;
import org.datalift.fwk.async.ParameterType;
import org.datalift.fwk.async.Parameters;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.RegexUriMapper;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.UriMapper;
import org.datalift.fwk.util.web.UriParam;

import static org.datalift.fwk.MediaTypes.*;


/**
 * A {@link ProjectModule project module} that loads the RDF data
 * from a {@link RdfFileSource RDF file source} into the internal
 * RDF store.
 *
 * @author lbihanic
 */
@Path(RdfLoader.MODULE_NAME)
public class RdfLoader extends BaseConverterModule implements Operation
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "rdfloader";
    
    public final static String OPERATION_ID =
            "http://www.datalift.org/core/converter/operation/" + MODULE_NAME;

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public RdfLoader() {
        super(MODULE_NAME, 200, SourceType.RdfFileSource,
                                SourceType.SparqlSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Displays the module welcome page.
     * @param  projectId   the URI of the data-lifting project.
     *
     * @return a JAX-RS response with the page template and parameters.
     */
    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam(PROJECT_ID_PARAM) URI projectId) {
        return this.newProjectView("rdfLoader.vm", projectId);
    }

    /**
     * <i>[Resource method]</i> Loads the data from the specified RDF
     * source (file or SPARQL) into the internal store and creates a
     * new associated RDF source.
     * @param  projectId          the URI of the data-lifting project.
     * @param  sourceId           the URI of the source to convert.
     * @param  destTitle          the name of the RDF source to hold the
     *                            converted data.
     * @param  targetGraphParam   the URI of the named graph to hold the
     *                            converted data, which will also be the
     *                            URI of the created RDF source.
     * @param  uriPattern         an optional regular expression to
     *                            apply to the URIs found in the RDF
     *                            data to alter them (see next
     *                            parameter).
     * @param  uriReplacement     an optional replacement string,
     *                            possibly including replacement patterns
     *                            from the above regular expression.
     *
     * @return a JAX-RS response redirecting the user browser to the
     *         created RDF source.
     * @throws WebApplicationException if any error occurred during the
     *         data conversion from SQL to RDF.
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response loadRdfData(
                        @FormParam(PROJECT_ID_PARAM)  UriParam projectId,
                        @FormParam(SOURCE_ID_PARAM)   UriParam sourceId,
                        @FormParam(TARGET_SRC_NAME)   String destTitle,
                        @FormParam(GRAPH_URI_PARAM)   UriParam targetGraphParam,
                        @FormParam(SRC_PATTERN_PARAM) String uriPattern,
                        @FormParam(DST_PATTERN_PARAM) String uriReplacement)
                                                throws WebApplicationException {
        if (! UriParam.isSet(projectId)) {
            this.throwInvalidParamError(PROJECT_ID_PARAM, null);
        }
        if (! UriParam.isSet(sourceId)) {
            this.throwInvalidParamError(SOURCE_ID_PARAM, null);
        }
        if (! UriParam.isSet(targetGraphParam)) {
            this.throwInvalidParamError(GRAPH_URI_PARAM, null);
        }
        Project p = this.getProject(projectId.toUri(PROJECT_ID_PARAM));
        Parameters params = this.getBlankParameters();
        params.setValue("project", projectId.toUri().toString());
        params.setValue("source", sourceId.toUri().toString());
        params.setValue("destTitle", destTitle);
        params.setValue("targetGraph", targetGraphParam.toUri().toString());
        params.setValue("uriPattern", uriPattern);
        params.setValue("uriReplacement", uriReplacement);
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
        try {
            Date start = new Date();
            URI projectId = URI.create(params.getProjectValue());
            URI sourceId = URI.create(params.getValue("source"));
            String destTitle = params.getValue("destTitle");
            if (destTitle == null) {
                destTitle = Double.toString(Math.random()).replace(".", "");
            }
            String targetGraphStr = params.getValue("targetGraph");
            if(targetGraphStr == null) {
                targetGraphStr = sourceId.toString() + "/" + destTitle;
            }
            URI targetGraph = URI.create(targetGraphStr);
            String uriPattern = params.getValue("uriPattern");
            String uriReplacement = params.getValue("uriReplacement");
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            RdfSource in = (RdfSource)
                            (p.getSource(sourceId));
            if (in == null) {
                throw new ObjectNotFoundException("project.source.not.found",
                                                  projectId, sourceId);
            }
            // Extract target named graph. It shall NOT conflict with
            // existing objects (sources, projects) otherwise it would not
            // be accessible afterwards (e.g. display, removal...).
            this.checkUriConflict(targetGraph, GRAPH_URI_PARAM);
            // Check for URI mapping.
            UriMapper mapper = null;
            if (! StringUtils.isBlank(uriPattern)) {
                try {
                    mapper = new RegexUriMapper(Pattern.compile(uriPattern),
                                                uriReplacement);
                }
                catch (IllegalArgumentException e) {
                    this.throwInvalidParamError(
                                            "uri_translation_src", uriPattern);
                }
            }
            // Load RDF data into target named graph.
            Repository internal = Configuration.getDefault()
                                               .getInternalRepository();
            log.debug("Loading RDF data from \"{}\" into graph \"{}\"",
                                                        sourceId, targetGraph);
            RdfUtils.upload(in, internal, targetGraph, mapper);
            // Register new transformed RDF source.
            URI baseUri = null;
            if (in.getBaseUri() != null) {
                baseUri = URI.create(in.getBaseUri());
            }
            this.addResultSource(p, in, destTitle,
                    targetGraph, baseUri, this.getOperationId(), params.getValues(),
                    start);
            log.info("RDF data from \"{}\" successfully loaded into \"{}\"",
                                                        sourceId, targetGraph);
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
        paramList.add(new Parameter("destTitle",
                "ws.param.destTitle", ParameterType.hidden));
        paramList.add(new Parameter("targetGraph",
                "ws.param.targetGraph", ParameterType.hidden));
        paramList.add(new Parameter("uriPattern",
                "ws.param.uriPattern", ParameterType.visible));
        paramList.add(new Parameter("uriReplacement",
                "ws.param.uriReplacement", ParameterType.visible));
        return new Parameters(paramList);
    }
}
