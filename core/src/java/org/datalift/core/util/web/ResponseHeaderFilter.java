package org.datalift.core.util.web;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;


/**
 * A {@link Filter servlet filter} to add headers to the HTTP response
 * to work around Tomcat's lack of support for setting cache-control
 * HTTP headers when serving static resource (CSS, images...).
 * <p>
 * From
 * <a href="http://www.symphonious.net/2007/06/19/caching-in-tomcat/">Danny's ResponseHeaderFilter</a>.</p>
 */
public class ResponseHeaderFilter implements Filter
{
    private final Map<String,String> headers = new TreeMap<String,String>();

    /** {@inheritDoc} */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.headers.clear();
        for (Enumeration<?> e = filterConfig.getInitParameterNames();
                                                        e.hasMoreElements(); ) {
            String name = (String)(e.nextElement());
            this.headers.put(name, filterConfig.getInitParameter(name));
        }
    }

    /** {@inheritDoc} */
    public void doFilter(ServletRequest request,
                         ServletResponse response, FilterChain chain)
                                        throws IOException, ServletException {
        HttpServletResponse httpRsp = (HttpServletResponse)response;

        for (String header : this.headers.keySet()) {
            if (httpRsp.containsHeader(header) == false) {
                httpRsp.addHeader(header, this.headers.get(header));
            }
        }
        chain.doFilter(request, response);
    }

    /** {@inheritDoc} */
    public void destroy() {
        // NOP
    }
}
