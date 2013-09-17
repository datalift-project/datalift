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

package org.datalift.core.log.web;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.datalift.core.log.LogContext;
import org.datalift.core.log.TimerContext;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.security.SecurityContext;


/**
 * A servlet filter that initializes the
 * {@link Logger#setContext(Object, Object) log diagnostic contexts}
 * from the HTTP request context.
 * <p>
 * Configuration of this filter is based on the following initialization
 * parameters:</p>
 * <dl>
 *  <dt><code>allow-force-debug</code></dt>
 *  <dd>If set to "true", the filter checks for the
 *   <code>force-debug</code> parameter in incoming HTTP requests and,
 *   if set to <code>true</code>, forces the debug log traces for the
 *   processing of the request.<br />
 *   By default, this parameter is set to "true".</dd>
 * </dl>
 *
 * @author lbihanic
 */
public class LogContextFilter implements Filter
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /**
     * The filter configuration parameter to allow ignoring the
     * {@link LogContextFilter#FORCE_DEBUG_REQUEST_PARAMETER} in
     * requests.
     * <p>
     * Defaults to <code>true</code>.</p>
     */
    public final static String ALLOW_FORCE_DEBUG_CONFIG_PARAMETER =
                                                            "allow-force-debug";
    /** The request parameter to force debug log for a given request. */
    public final static String FORCE_DEBUG_REQUEST_PARAMETER = "force-debug";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Should the force-debug request parameter be honored. */
    private boolean allowForceDebug = true;

    //-------------------------------------------------------------------------
    // Filter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Read filter configuration.
        String value = filterConfig.getInitParameter(
                                            ALLOW_FORCE_DEBUG_CONFIG_PARAMETER);
        if ((value != null) && (value.length() != 0)) {
            value = value.trim();
            this.allowForceDebug = (! ((value.equalsIgnoreCase("false")) ||
                                       (value.equalsIgnoreCase("no"))));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response, FilterChain chain)
                                        throws IOException, ServletException {
        // Reset diagnostic contexts.
        Logger.clearContexts();
        // Add timer context to trace request execution duration.
        Logger.setContext(LogContext.Timer, new TimerContext());
        // Add user context.
        Logger.setContext(LogContext.User, new UserContext());
        // Add request path context.
        if (request instanceof HttpServletRequest) {
            Logger.setContext(LogContext.Path,
                              ((HttpServletRequest)request).getRequestURI());
        }
        // Check for forced debug traces.
        if (this.allowForceDebug) {
            String forceDebug = request.getParameter(
                                                FORCE_DEBUG_REQUEST_PARAMETER);
            if (forceDebug != null) {
                Logger.promoteDebugTraces(true);
            }
        }
        // Forward request.
        chain.doFilter(request, response);
        // Clean diagnostic contexts up.
        Logger.clearContexts();
    }


    //-------------------------------------------------------------------------
    // UserContext nested class
    //-------------------------------------------------------------------------

    /**
     * A {@link Logger#setContext(Object, Object) diagnostic context} to
     * trace the currently logged user.
     */
    private static final class UserContext
    {
        @Override
        public String toString() {
            String loggedUser = SecurityContext.getUserPrincipal();
            return (loggedUser != null)? loggedUser: "";
        }
    }
}
