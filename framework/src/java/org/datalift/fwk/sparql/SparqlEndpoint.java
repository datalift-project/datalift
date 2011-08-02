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
