package org.datalift.fwk.sparql;


import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.datalift.fwk.Module;


/**
 * The Java interface for in-VM invocation of the SPARQL endpoint.
 * <p>
 * An implementation of this interface shall be provided by the
 * framework implementation and made available via the DataLift
 * {@link org.datalift.fwk.Configuration#getBean(Class) configuration}.</p>
 *
 * @author lbihanic
 */
public interface SparqlEndpoint extends Module
{
    /** The SPARQL endpoint module name. */
    public final static String MODULE_NAME = "sparql";

    /**
     * Executes a SPARQL query and returns the results directly to
     * the client.
     * @param  query       the SPARQL query string.
     * @param  uriInfo     the request URI data.
     * @param  request     the JAX-RS Request object, for content
     *                     negotiation.
     * @param  acceptHdr   the HTTP Accept header, for content
     *                     negotiation.
     *
     * @return a JAX-RS response with the SPARQL result formatted
     *         according to the negotiated format.
     * @throws WebApplicationException if any error occurred processing
     *         the SPARQL query.
     */
    public ResponseBuilder executeQuery(String query,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException;

    /**
     * Executes a SPARQL query and returns the results directly to
     * the client.
     * @param  defaultGraphUris   the target default graphs, or
     *                            <code>null</code>.
     * @param  namedGraphUris     named graph definitions for query.
     * @param  query              the SPARQL query string.
     * @param  uriInfo            the request URI data.
     * @param  request            the JAX-RS Request object, for content
     *                            negotiation.
     * @param  acceptHdr          the HTTP Accept header, for content
     *                            negotiation.
     *
     * @return a JAX-RS response with the SPARQL result formatted
     *         according to the negotiated format.
     * @throws WebApplicationException if any error occurred processing
     *         the SPARQL query.
     * @throws SecurityException if the user is not allowed to
     *         perform the query.
     */
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException;
    
  
    /**
     * Enhanced entry point for executing a SPARQL query and sending
     * paginated results directly to the client.
     * @param  defaultGraphUris   the target default graphs, or
     *                            <code>null</code>.
     * @param  namedGraphUris     named graph definitions for query.
     * @param  query              the SPARQL query string.
     * @param  startOffset        the index of the first expected result
     *                            (inclusive).
     * @param  endOffset          the index of the last expected result
     *                            (exclusive).
     * @param  gridJson           whether to return data grid optimized
     *                            JSON result. Only applicable if the
     *                            requested result MIME type is
     *                            "application/json" or
     *                            "application/sparql-results+json".
     * @param  uriInfo            the request URI data.
     * @param  request            the JAX-RS Request object, for content
     *                            negotiation.
     * @param  acceptHdr          the HTTP Accept header, for content
     *                            negotiation.
     *
     * @return a JAX-RS response with the SPARQL result formatted
     *         according to the negotiated format.
     * @throws WebApplicationException if any error occurred processing
     *         the SPARQL query.
     * @throws SecurityException if the user is not allowed to
     *         perform the query.
     */
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset, boolean gridJson,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException;
}
