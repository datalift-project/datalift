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
import java.util.LinkedList;
import java.util.List;

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
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.web.UriParam;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A {@link ProjectModule project module} that performs RDF to RDF
 * transformations by applying a set of SPARQL CONSTRUCT queries and
 * saving the generated triples into a new
 * {@link TransformedRdfSource source}.
 *
 * @author lbihanic
 */
@Path(RdfTransformer.MODULE_NAME)
public class RdfTransformer extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "rdftransformer";

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public RdfTransformer() {
        super(MODULE_NAME, 5000, SourceType.TransformedRdfSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam(PROJECT_ID_PARAM) URI projectId) {
        return this.newProjectView("constructQueries.vm", projectId);
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response convertRdfSource(
                        @FormParam(PROJECT_ID_PARAM) UriParam projectId,
                        @FormParam(SOURCE_ID_PARAM)  UriParam sourceId,
                        @FormParam(TARGET_SRC_NAME)  String destTitle,
                        @FormParam(GRAPH_URI_PARAM)  UriParam targetGraphParam,
                        @FormParam("query[]") List<String> queries,
                        @FormParam(OVERWRITE_GRAPH_PARAM) boolean overwrite)
                                                throws WebApplicationException {
        if (projectId == null) {
            this.throwInvalidParamError(PROJECT_ID_PARAM, null);
        }
        if (sourceId == null) {
            this.throwInvalidParamError(SOURCE_ID_PARAM, null);
        }
        if (targetGraphParam == null) {
            this.throwInvalidParamError(GRAPH_URI_PARAM, null);
        }
        Response response = null;

        URI targetGraph = targetGraphParam.toUri(GRAPH_URI_PARAM);
        Repository internal = Configuration.getDefault()
                                           .getInternalRepository();
        boolean graphUpdated = false;
        try {
            // Retrieve project and source.
            Project p = this.getProject(projectId.toUri(PROJECT_ID_PARAM));
            TransformedRdfSource in = (TransformedRdfSource)
                                (p.getSource(sourceId.toUri(SOURCE_ID_PARAM)));
            if (in == null) {
                throw new ObjectNotFoundException("project.source.not.found",
                                                  projectId, sourceId);
            }
            // Clean the query list to remove empty entries.
            if (queries != null) {
                List<String> l = new LinkedList<String>();
                for (String q : queries) {
                    if (! isBlank(q)) {
                        l.add(q);
                    }
                }
                queries = l;
            }
            // Check target named graph. It shall NOT conflict with
            // existing objects (sources, projects) otherwise it would not
            // be accessible afterwards (e.g. display, removal...).
            this.checkUriConflict(targetGraph, GRAPH_URI_PARAM);
            // Check that at least one query is present.
            if ((queries == null) || (queries.size() == 0)) {
                this.throwInvalidParamError("query", null);
            }
            // Execute SPARQL Construct queries.
            graphUpdated = true;
            RdfUtils.convert(internal, queries, internal, targetGraph,
                                       overwrite, URI.create(in.getUri()));
            // Register new transformed RDF source.
            Source out = this.addResultSource(p, in, destTitle, targetGraph);
            // Display project source tab, including the newly created source.
            response = this.created(out).build();
        }
        catch (Exception e) {
            if (graphUpdated) {
                try {
                    RdfUtils.clearGraph(internal, targetGraph);
                }
                catch (Exception e1) { /* Ignore... */ }
            }
            this.handleInternalError(e);
        }
        return response;
    }
}
