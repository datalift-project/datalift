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

package org.datalift.fwk;


import java.net.URI;

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
     * @return a {@link Response JAX-RS response} pointing to the
     *         resolved static resource or <code>null</code> if no
     *         matching resource was found.
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
     * @return a {@link Response JAX-RS response} with the result of
     *         the SPARQL DESCRIBE query on the RDF resource or
     *         <code>null</code> if the request resource was not found
     *         in the RDF store.
     * @throws WebApplicationException if any error occurred accessing
     *         the RDF resource.
     */
    public Response resolveRdfResource(UriInfo uriInfo, Request request,
                                       String acceptHdr)
                                                throws WebApplicationException;

    /**
     * Attempts to resolve a request as a RDF resource from the default
     * (lifted data) triple store.
     * @param  uri         the URI of the RDF resource.
     * @param  uriInfo     the requested URI data.
     * @param  request     the JAX-RS request object.
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response JAX-RS response} with the result of
     *         the SPARQL DESCRIBE query on the RDF resource or
     *         <code>null</code> if the request resource was not found
     *         in the RDF store.
     * @throws WebApplicationException if any error occurred accessing
     *         the RDF resource.
     */
    public Response resolveRdfResource(URI uri, UriInfo uriInfo,
                                       Request request, String acceptHdr)
                                                throws WebApplicationException;

    /**
     * Attempts to resolve a request as a module public resource.
     * @param  module      the target module.
     * @param  uriInfo     the requested URI.
     * @param  request     the JAX-RS request object.
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response JAX-RS response} to download the
     *         content of the specified public resource.
     * @throws WebApplicationException complete with status code and
     *         plain-text error message if any error occurred while
     *         accessing the requested resource.
     */
    public Response resolveModuleResource(String module,
                                          UriInfo uriInfo, Request request,
                                          String acceptHdr)
                                                throws WebApplicationException;
}
