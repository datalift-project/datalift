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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        // Retrieve project.
        Project p = this.getProject(projectId);
        // Display conversion configuration page.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p);
        args.put("converter", this);
        return Response.ok(this.newViewable("/publisher.vm", args))
                       .build();
    }

    @POST
    public Response publishRdfSource(
                                @FormParam("project") URI projectId,
                                @FormParam("source") URI sourceId,
                                @FormParam("dest_graph_uri") URI targetGraph,
                                @FormParam("overwrite") boolean overwrite,
                                @Context UriInfo uriInfo,
                                @Context Request request,
                                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            TransformedRdfSource in =
                                (TransformedRdfSource)(p.getSource(sourceId));
            if (targetGraph == null) {
                // Get the source (CSV, RDF/XML, database...) at the origin of the
                // transformations and use its name as target named graph.
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
            Configuration cfg = Configuration.getDefault();
            // Publish input source triples in public repository.
            List<String> constructs = Arrays.asList(
                            "CONSTRUCT { ?s ?p ?o . } WHERE { GRAPH <"
                                + in.getTargetGraph() + "> { ?s ?p ?o . } }");
            RdfUtils.convert(cfg.getInternalRepository(), constructs,
                             cfg.getDataRepository(), targetGraph, overwrite);
            // Display generated triples.
            response = this.displayGraph(null, targetGraph,
                                         uriInfo, request, acceptHdr);
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }
}
