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

package org.datalift.fwk.util.web;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * An object providing direct access to the being-processed
 * {@link HttpServletRequest HTTP request} and the associated
 * {@link HttpServletResponse response} objects.
 * <p>
 * The request context is accessible at any time during request
 * processing through {@link RequestContext#get() }.</p>
 *
 * @author lbihanic
 */
public final class RequestContext
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** The context for the being-processed HTTP request. */
    private final static ThreadLocal<RequestContext> context =
                                            new ThreadLocal<RequestContext>();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The being-processed HTTP request. */
    public final HttpServletRequest request;
    /** The HTTP response associated to the being-processed HTTP request. */
    public final HttpServletResponse response;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new request context.
     * @param  request    the {@link HttpServletRequest HTTP request}.
     * @param  response   the {@link HttpServletResponse HTTP response}.
     */
    private RequestContext(HttpServletRequest request,
                           HttpServletResponse response) {
        this.request  = request;
        this.response = response;
    }

    //-------------------------------------------------------------------------
    // Static methods
    //-------------------------------------------------------------------------

    /**
     * Returns the context for the being-processed HTTP request.
     * @return the current request context or <code>null</code> if no
     *         request is being processed at the time this method was
     *         called.
     */
    public static RequestContext get() {
        return context.get();
    }

    /**
     * Sets the context for the being-processed HTTP request.
     * @param  request    the {@link HttpServletRequest HTTP request}.
     * @param  response   the {@link HttpServletResponse HTTP response}.
     */
    public static void set(HttpServletRequest request,
                           HttpServletResponse response) {
        context.set(new RequestContext(request, response));
    }

    /**
     * Resets the current request context.
     */
    public static void reset() {
        context.remove();
    }
}
