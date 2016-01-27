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
