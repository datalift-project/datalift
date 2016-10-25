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

package org.datalift.sparql2viz;


import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.sparql.AbstractSparqlEndpoint;
import org.datalift.sparql.SesameSparqlEndpoint;


@Path(AbstractSparqlEndpoint.MODULE_NAME)
public class Sparql2VizEndpoint extends SesameSparqlEndpoint {

    /** The name of the template for the endpoint welcome page. */
    private final static String WELCOME_TEMPLATE = "vizEndpoint.vm";

    /** Default constructor. */
	public Sparql2VizEndpoint() {
        super(WELCOME_TEMPLATE);
	}

    /**
     * <i>[Resource method]</i> Returns a static resource associated to
     * this module.
     * @param  path        the path of the requested static resource.
     * @param  uriInfo     the request URI data.
     * @param  request     the JAX-RS Request object, for content
     *                     negotiation.
     * @param  acceptHdr   the HTTP Accept header, for content
     *                     negotiation.
     *
     * @return a JAX-RS {@link Response} wrapping the input stream
     *         on the requested resource content.
     * @throws WebApplicationException if any error occurred while
     *         accessing the requested resource.
     */
    @GET
    @Path("viz/{path: .*$}")
    public Response getStaticResource(@PathParam("path") String path,
                                      @Context UriInfo uriInfo,
                                      @Context Request request,
                                      @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        log.trace("Reading static resource: {}", path);
        return Configuration.getDefault()
                            .getBean(ResourceResolver.class)
                            .resolveModuleResource(this.getName(),
                                                   uriInfo, request, acceptHdr);
    }
}
