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
import java.util.List;

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

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;


public class RdfConverter extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String MODULE_NAME = "rdfconverter";

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public RdfConverter() {
        super(MODULE_NAME, SourceType.TransformedRdfSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public Response getIndexPage(@QueryParam("project") URI projectId,
                                 @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Check that a valid source is available.
            this.getLastSource(p);
            response = Response.ok(
                        this.newViewable("/constructQueries.vm", p)).build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response convertRdfSource(@FormParam("project") URI projectId,
                                     @FormParam("query[]") List<String> queries,
                                     @Context UriInfo uriInfo,
                                     @Context Request request,
                                     @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        try {
            if ((queries == null) || (queries.size() == 0)) {
                this.throwInvalidParamError("queries", null);
            }
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            TransformedRdfSource src =
                                (TransformedRdfSource)this.getLastSource(p);
            // Apply SPARQL CONSTRUCT queries to generate new RDF triples.
            String srcName  = this.nextSourceName(p);
            URI targetGraph = this.newGraphUri(src, srcName);
            RdfUtils.convert(this.internalRepository, queries,
                             this.internalRepository, targetGraph);
            // Register new transformed RDF source.
            this.addResultSource(p, src, srcName, targetGraph);
            // Display generated triples.
            response = this.displayGraph(this.internalRepository, targetGraph,
                                         uriInfo, request, acceptHdr);
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }
}
