/*
 * Copyright 2004 The Apache Software Foundation Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.datalift.core.util.web;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * A {@link Filter servlet filter} that sets the character encoding
 * to be used in parsing the incoming request, either unconditionally
 * or only if the client did not specify a character encoding.
 * <p>
 * Configuration of this filter is based on the following initialization
 * parameters:</p>
 * <dl>
 *  <dt><code>encoding</code></dt>
 *  <dd>The character encoding to be configured for the requests, either
 *   conditionally or unconditionally based on the <code>ignore</code>
 *   initialization parameter.<br />
 *   By default, this parameter is set to "utf-8".</dd>
 *  <dt><code>ignore</code></dt>
 *  <dd>If set to "true", any character encoding specified by the client
 *   is ignored, and the
 *   {@link ServletRequest#getCharacterEncoding() request character encoding}
 *   is set to the value returned by {@link #selectEncoding}.  If set
 *   to "false", {@link #selectEncoding} is called
 *   <strong>only</strong> if the client has not specified an encoding.<br />
 *   By default, this parameter is set to "true".</dd>
 * </dl>
 * <p>
 * Although this filter can be used unchanged, it is also easy to
 * subclass it and make the {@link #selectEncoding} method more
 * intelligent about what encoding to choose, based on characteristics
 * of the incoming request (such as the values of the
 * <code>Accept-Language</code> and <code>User-Agent</code> headers,
 * or a value from the user session).</p>
 *
 * @author Craig McClanahan
 */
public class CharacterEncodingFilter implements Filter
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /**
     * The default character encoding to set for requests that pass
     * through this filter.
     */
    private String encoding = "utf-8";

    /** Should the character encoding specified by the client be ignored? */
    private boolean ignore = true;

    //-------------------------------------------------------------------------
    // Filter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Read filter configuration.
        String value = filterConfig.getInitParameter("encoding");
        if ((value != null) && (value.length() != 0)) {
            this.encoding = value;
        }
        value = filterConfig.getInitParameter("ignore");
        if ((value != null) && (value.length() != 0)) {
            this.ignore = (! ((value.equalsIgnoreCase("false")) ||
                              (value.equalsIgnoreCase("no"))));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        // NOP
    }

    /**
     * Selects and sets (if specified) the character encoding
     * to be used to interpret the request parameters.
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response, FilterChain chain)
                                        throws IOException, ServletException {
        // Conditionally select and set the request character encoding.
        if ((this.ignore) || (request.getCharacterEncoding() == null)) {
            String enc = this.selectEncoding(request);
            if (enc != null) {
                request.setCharacterEncoding(enc);
            }
        }
        // Pass control on to the next filter.
        chain.doFilter(request, response);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Selects an appropriate character encoding, based on the
     * characteristics of the current request and/or filter
     * initialization parameters.  If no character encoding should
     * be set, return <code>null</code>.
     * <p>
     * The default implementation unconditionally returns the value
     * configured by the <strong>encoding</strong> initialization
     * parameter for this filter.
     * @param  request   the request being processed.

     * @return the character encoding to use to interpret the request
     *         parameters or <code>null</code> to proceed with the one
     *         specified by the client, if any.
     */
    protected String selectEncoding(ServletRequest request) {
        return this.encoding;
    }
}
