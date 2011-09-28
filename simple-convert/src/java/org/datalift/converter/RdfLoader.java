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


import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.util.RegexUriMapper;
import org.datalift.fwk.util.UriMapper;


public class RdfLoader extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String MODULE_NAME = "rdfloader";

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public RdfLoader() {
        super(MODULE_NAME, SourceType.RdfFileSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public Response indexPa(@QueryParam("project") URI projectId,
                                   @Context UriInfo uriInfo,
                                   @Context Request request,
                                   @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        // Retrieve project.
        Project p = this.getProject(projectId);
        response = Response.ok(
                    this.newViewable("/rdfLoader.vm", p)).build();
        return response;
    }
    
    @POST
    public Response publishProject(@QueryParam("project") URI projectId,
    						@QueryParam("source") URI sourceId,
    						@FormParam("dest_title") String destTitle,
                            @FormParam("dest_graph_uri") URI targetGraph,
                            @FormParam("uri_translation_src") String uriTranslationSrc,
                            @FormParam("uri_translation_dest") String uriTranslationDest,
    						@Context UriInfo uriInfo,
    						@Context Request request,
    						@HeaderParam(ACCEPT) String acceptHdr)
                         		throws WebApplicationException {
    	Response response = null;
		// Retrieve project.
		Project p = this.getProject(projectId);
		try {
			RegexUriMapper mapper = new RegexUriMapper(Pattern.compile(uriTranslationSrc), uriTranslationDest);
			
			// Load input source.
			RdfFileSource src = (RdfFileSource) p.getSource(sourceId);
			
			RdfUtils.upload(new File(configuration.getPublicStorage() + "/" + src.getFilePath()), 
					RdfUtils.parseMimeType(src.getMimeType()), 
					this.internalRepository, targetGraph, 
					(UriMapper)mapper, src.getSource());
			// Register new transformed RDF source.
			this.addResultSource(p, src, destTitle, targetGraph);
			 String uri = projectId.toString() + "#source";
	            response = Response.created(projectId)
	                               .entity(this.newViewable("/redirect.vm", uri))
	                               .type(TEXT_HTML)
	                               .build();
		}
		catch (Exception e) {
			this.handleInternalError(e);
		}
		return response;
	}
}
