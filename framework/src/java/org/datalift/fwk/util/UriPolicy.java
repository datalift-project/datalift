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

package org.datalift.fwk.util;


import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.LifeCycle;


/**
 * URI policies allow modules to provide specific URI mapping
 * and/or resolution policies for RDF resources, such as distinguishing
 * subject URIs from representation (RDF, HTML...) URLs or providing
 * custom rendering for known RDF types, e.g. data filtering according
 * to the user profile, inclusion of licensing data as a set of
 * supplementary triples...
 * <p>
 * Modules relying on specific URI policies shall include a
 * <i>provider-configuration file</i> named
 * <code>META-INF/services/org.datalift.fwk.util.UriPolicy</code> that
 * contains a list of fully-qualified implementation class names.</p>
 * <p>
 * <code>UriPolicy</code> classes are loaded using the class loader of
 * the owning module.</p>
 *
 * @author lbihanic
 */
public interface UriPolicy extends LifeCycle
{
    /**
     * Returns whether this policy can handle the requested URI.
     * <p>
     * If several policies are configured, DataLift queries them in
     * turn until a non-null handler object is provided.
     * @param  uriInfo   the requested URI.
     *
     * @return a handler object for processing the request or
     *         <code>null</code> if this policy does not apply to the
     *         requested URI.
     */
    public ResourceHandler canHandle(UriInfo uriInfo,
                                     Request request, String acceptHdr);

    //-------------------------------------------------------------------------
    // ResourceHandler interface
    //-------------------------------------------------------------------------

    /**
     * An object to which DataLift delegates URI resolution and
     * representation construction of RDF resources.
     */
    public interface ResourceHandler
    {
        /**
         * Resolves the requested URI and returns the actual URI for
         * getting the appropriate representation of the resource.
         * @return the URI the client shall be redirected to the get
         *         the expected resource representation or
         *         <code>null</code> if the representation can be served
         *         from the requested URI.
         * @throws WebApplicationException if any error occurred while
         *         resolving the URI, e.g. 404 (not found), 400 (bad
         *         request)...
         */
        public URI resolve() throws WebApplicationException;

        /**
         * Returns the resource representation to be forwarded to the
         * client.
         * @return the resource representation.
         * @throws WebApplicationException if any error occurred while
         *         building the representation.
         */
        public Response getRepresentation() throws WebApplicationException;
    }
}
