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
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.RegexUriMapper;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.UriMapper;

import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.MediaTypes.*;


/**
 * A {@link ProjectModule project module} that loads the RDF data
 * from a {@link RdfFileSource RDF file source} into the internal
 * RDF store.
 *
 * @author lbihanic
 */
@Path(RdfLoader.MODULE_NAME)
public class RdfLoader extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "rdfloader";

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

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        return this.newProjectView("rdfLoader.vm", projectId);
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response loadRdfData(
                    @FormParam("project") URI projectId,
                    @FormParam("source") URI sourceId,
                    @FormParam("dest_title") String destTitle,
                    @FormParam("dest_graph_uri") URI targetGraph,
                    @FormParam("uri_translation_src") String uriPattern,
                    @FormParam("uri_translation_dest") String uriReplacement)
                                                throws WebApplicationException {
        Response response = null;
        try {
            log.debug("Loading RDF data from \"{}\" into graph \"{}\"",
                                                        sourceId, targetGraph);
            if (isBlank(targetGraph.toString())) {
                targetGraph = null;
            }
            // Retrieve project.
            Project p = this.getProject(projectId);
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
            Repository internal = Configuration.getDefault()
                                               .getInternalRepository();
            // Load input source.
            RdfSource in = (RdfSource)(p.getSource(sourceId));
            RdfUtils.upload(in, internal, targetGraph, mapper);
            // Register new transformed RDF source.
            Source out = this.addResultSource(p, in, destTitle, targetGraph);
            // Display project source tab, including the newly created source.
            response = this.created(out).build();

            log.info("RDF data successfully loaded into \"{}\"", targetGraph);
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }
}
