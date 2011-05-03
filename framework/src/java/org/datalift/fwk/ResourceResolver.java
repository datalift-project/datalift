package org.datalift.fwk;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


/**
 * An object capable of resolving HTTP requests into local public static
 * or RDF resources.
 * <p>
 * An implementation of this interface shall be provided by the
 * framework implementation and made available via the
 * {@link Configuration#getBean(Class) DataLift configuration}.</p>
 *
 * @author lbihanic
 */
public interface ResourceResolver
{
    /**
     * Attempts to resolve a request as a local public static resource.
     * @param  path      the requested resource path.
     * @param  request   the HTTP request being processed.
     *
     * @return a JAX-RS response pointing to the resolved static
     *         resource of <code>null</code> if no matching resource
     *         was found.
     * @throws WebApplicationException if the resolved request path
     *         points outside of the specified document root directory.
     */
    public Response resolveStaticResource(String path, Request request)
                                                throws WebApplicationException;

    /**
     * Attempts to resolve a request as a RDF resource from the default
     * (lifted data) triple store.
     * @param  uriInfo     the requested URI.
     * @param  request     the JAX-RS request object.
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response service response} with the result of
     *         the SPARQL DESCRIBE query on the RDF resource or
     *         <code>null</code> if the request resource was not found
     *         in the RDF store.
     * @throws WebApplicationException if any error occurred accessing
     *         the RDF resource.
     */
    public Response resolveRdfResource(UriInfo uriInfo, Request request,
                                       String acceptHdr)
                                                throws WebApplicationException;
}
