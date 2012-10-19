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

package org.datalift.fwk.sparql;


import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.StringUtils;


/**
 * The Java interface for in-VM invocation of the SPARQL endpoint.
 * <p>
 * An implementation of this interface shall be provided by every
 * framework implementation. Other modules access it after retrieving
 * it, as any Datalift module, using the DataLift runtime
 * {@link Configuration#getBean(Class) configuration}, during or after
 * the post-initialization step:</p>
 * <blockquote><pre>
 *   SparqlEndpoint endpoint = Configuration.getDefault().getBean(SparqlEndpoint.class);
 * </pre></blockquote>
 * <p>
 * In addition to the methods below, every SPARQL endpoint
 * implementation shall provide REST web services on the following
 * URLs:</p>
 * <dl>
 *  <dt><code>/sparql</code></dt>
 *  <dd>the standard HTTP REST SPARQL endpoint, as defined by the
 *   <a href="http://www.w3.org/TR/rdf-sparql-protocol/#query-bindings-http">
 *   SPARQL Protocol for RDF W3C recommendation</a>.</dd>
 *  <dt><code>/sparql/describe</code></dt>
 *  <dd>a DataLift-specific service to return the description of an RDF
 *   object. This service, available through both GET and POST methods
 *   takes the following input arguments:
 *   <dl>
 *    <dt><code>uri</code></dt>
 *    <dd>the object to describe</dd>
 *    <dt><code>type</code></dt>
 *    <dd>the object type (see {@link DescribeType})</dd>
 *    <dt><code>default-graph</code></dt>
 *    <dd>the RDF store to read the object description from</dd>
 *   </dl></dd>
 * </dl>
 *
 * @author lbihanic
 */
public interface SparqlEndpoint
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /**
     * The type of object to
     * {@link SparqlEndpoint#describe(String,DescribeType,UriInfo,Request,String) describe}.
     */
    public enum DescribeType {
        Object,
        Predicate,
        Graph,
        RdfType;

        /**
         * Return the enumeration value corresponding to the specified
         * string, ignoring case.
         * @param  s   the description type, as a string.
         *
         * @return the description type value or <code>null</code> if
         *         the specified string was not recognized.
         */
        public static DescribeType fromString(String s) {
            DescribeType v = null;
            if (StringUtils.isSet(s)) {
                for (DescribeType t : values()) {
                    if (t.name().equalsIgnoreCase(s)) {
                        v = t;
                        break;
                    }
                }
            }
            return v;
        }
    }

    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------

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
     * @param  format             the expected response format,
     *                            overrides the HTTP Accept header.
     * @param  jsonCallback       the name of the JSONP callback to
     *                            wrap the JSON response.
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
                            String format, String jsonCallback,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException;

    /**
     * Provides the description of the specified object (node, predicate
     * or named graph) from the default public RDF store and returns the
     * results directly to the client.
     * <p>
     * Whenever known, the type of description shall be provided to
     * avoid the overhead of querying the RDF store to try to detect
     * the possible applicable description types.</p>
     * @param  uri            the URI of the object to describe.
     * @param  type           the type of the object or
     *                        <code>null</code> if unknown.
     * @param  uriInfo        the request URI data.
     * @param  request        the JAX-RS Request object, for content
     *                        negotiation.
     * @param  acceptHdr      the HTTP Accept header, for content
     *                        negotiation.
     *
     * @return a JAX-RS response with the object description, formatted
     *         according to the negotiated format.
     * @throws WebApplicationException if any error occurred processing
     *         the SPARQL query.
     * @throws SecurityException if the user is not allowed to
     *         perform the query.
     * 
     * @see    #describe(String, DescribeType, Repository, UriInfo, Request, String)
     */
    public ResponseBuilder describe(String uri, DescribeType type,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException;

    /**
     * Provides the description of the specified object (node, predicate
     * or named graph) from the specified RDF store and returns the
     * results directly to the client.
     * <p>
     * Whenever known, the type of description shall be provided to
     * avoid the overhead of querying the RDF store to try to detect
     * the possible applicable description types.</p>
     * @param  uri          the URI of the object to describe.
     * @param  type         the type of the object or
     *                      <code>null</code> if unknown.
     * @param  repository   the RDF store to read the object
     *                      description from.
     * @param  uriInfo      the request URI data.
     * @param  request      the JAX-RS Request object, for content
     *                      negotiation.
     * @param  acceptHdr    the HTTP Accept header, for content
     *                      negotiation.
     *
     * @return a JAX-RS response with the object description, formatted
     *         according to the negotiated format.
     * @throws WebApplicationException if any error occurred processing
     *         the SPARQL query.
     * @throws SecurityException if the user is not allowed to
     *         perform the query.
     */
    public ResponseBuilder describe(String uri, DescribeType type,
                                    Repository repository, UriInfo uriInfo,
                                    Request request, String acceptHdr)
                                                throws WebApplicationException;

    /**
     * Provides the description of the specified object (node, predicate
     * or named graph) from the specified RDF store and returns the
     * results directly to the client.
     * <p>
     * Whenever known, the type of description shall be provided to
     * avoid the overhead of querying the RDF store to try to detect
     * the possible applicable description types.</p>
     * @param  uri            the URI of the object to describe.
     * @param  type           the type of the object or
     *                        <code>null</code> if unknown.
     * @param  repository     the RDF store to read the object
     *                        description from.
     * @param  max            the maximum number of results to return.
     * @param  format         the expected response format, overrides
     *                        the HTTP Accept header.
     * @param  jsonCallback   the name of the JSONP callback to wrap
     *                        the JSON response.
     * @param  uriInfo        the request URI data.
     * @param  request        the JAX-RS Request object, for content
     *                        negotiation.
     * @param  acceptHdr      the HTTP Accept header, for content
     *                        negotiation.
     *
     * @return a JAX-RS response with the object description, formatted
     *         according to the negotiated format.
     * @throws WebApplicationException if any error occurred processing
     *         the SPARQL query.
     * @throws SecurityException if the user is not allowed to
     *         perform the query.
     */
    public ResponseBuilder describe(String uri, DescribeType type,
                                    Repository repository, int max, 
                                    String format, String jsonCallback,
                                    UriInfo uriInfo, Request request,
                                    String acceptHdr)
                                                throws WebApplicationException;
}
