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


public class LogContextFilter implements Filter
{
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // NOP
    }

    @Override
    public void destroy() {
        // NOP
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response, FilterChain chain)
                                        throws IOException, ServletException {
        // Reset diagnostic contexts.
        Logger.clearContexts();
        // Add timer context to trace request execution duration.
        Logger.setContext(LogContext.Timer, new TimerContext());

        // Forward request.
        chain.doFilter(request, response);
        // Clean diagnostic contexts up.
        Logger.clearContexts();
    }
}
