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

package org.datalift.core.util.web;


import java.io.IOException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.datalift.fwk.Configuration;


/**
 * A servlet filter that retrieves all
 * {@link Configuration#getBeans(Class) configured}
 * {@link RequestLifecycleListener}s from Datalift {@link Configuration}
 * to notify them when a new request is being
 * {@link RequestLifecycleListener#requestReceived() processed},
 * pass control on to the next filter in chain and finally notifies
 * listeners when the response is being
 * {@link RequestLifecycleListener#responseSent() sent}.
 *
 * @author lbihanic
 */
public class RequestLifecycleFilter implements Filter
{
    //-------------------------------------------------------------------------
    // Filter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response, FilterChain chain)
                                        throws IOException, ServletException {
        // Get all request lifecycle listeners registered in Datalift config.
        Collection<RequestLifecycleListener> listeners =
                        Configuration.getDefault()
                                     .getBeans(RequestLifecycleListener.class);
        // Notify each listener that a new request is being processed.
        for (RequestLifecycleListener l : listeners) {
            l.requestReceived();
        }
        // Pass control on to the next filter.
        chain.doFilter(request, response);
        // Notify each listener that request processing is complete.
        for (RequestLifecycleListener l : listeners) {
            l.responseSent();
        }
    }

    //-------------------------------------------------------------------------
    // RequestLifecycleListener interface definition
    //-------------------------------------------------------------------------

    /**
     * Objects interested in being notified of the occurrence of new
     * user requests shall implement this interface and be registered
     * as such in the Datalift {@link Configuration}.
     */
    public static interface RequestLifecycleListener
    {
        /** Notifies this listener that a new request has been received. */
        public void requestReceived();
        /** Notifies this listener that request processing is terminating. */
        public void responseSent();
    }
}
