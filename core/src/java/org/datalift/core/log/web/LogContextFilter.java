package org.datalift.core.log.web;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.datalift.core.log.LogContext;
import org.datalift.core.log.TimerContext;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.security.SecurityContext;


/**
 * A servlet filter that initializes the
 * {@link Logger#setContext(Object, Object) log diagnostic contexts}
 * from the HTTP request context.
 *
 * @author lbihanic
 */
public class LogContextFilter implements Filter
{
    //-------------------------------------------------------------------------
    // Filter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // NOP
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
        String loggedUser = SecurityContext.getUserPrincipal();
        if (loggedUser != null) {
            Logger.setContext(LogContext.User, loggedUser);
        }

        // Forward request.
        chain.doFilter(request, response);
        // Clean diagnostic contexts up.
        Logger.clearContexts();
    }
}
