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

package org.datalift.core.i18n.web;


import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * A {@link Filter servlet filter} that extracts the user's preferred
 * locales from the HTTP request <code>Accept-Language</code> header
 * and make them available through the {@link PreferredLocales} class.
 *
 * @author lbihanic
 */
public class PreferredLocalesFilter implements Filter
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private List<Locale> defaultLocales = null;

    //-------------------------------------------------------------------------
    // Filter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Read filter configuration.
        String p = filterConfig.getInitParameter("default-locales");
        if (! isBlank(p)) {
            // Create list of default locales.
            List<Locale> l = new LinkedList<Locale>();
            for (String s : p.trim().split("\\s*,\\s*")) {
                String[] elts = s.split("_");
                String country = "";
                String variant = "";
                if (elts.length > 1) {
                    country = elts[1];
                    if (elts.length > 2) {
                        variant = elts[2];
                    }
                }
                l.add(new Locale(elts[0], country, variant));
            }
            if (! l.isEmpty()) {
                defaultLocales = Collections.unmodifiableList(l);
                log.info("Application default locales set to: {}", l);
            }
        }
        // Else: Let defaultLocales be null to use JVM default locale.
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
        List<Locale> l = this.defaultLocales;
        // Check for presence of Accept-Language HTTP header from request as
        // the Servlet API mandates the container to provide the JVM locale
        // as default if this header is not set.
        if ((request instanceof HttpServletRequest) &&
            (((HttpServletRequest)request).getHeader("Accept-Language") != null)) {
            // Extract and install user's preferred locales from HTTP request.
            l = new LinkedList<Locale>();
            for (Enumeration<?> e = request.getLocales(); e.hasMoreElements(); ) {
                l.add((Locale)(e.nextElement()));
            }
        }
        // Install user's preferred locales.
        PreferredLocales.set(l);
        // Forward request.
        chain.doFilter(request, response);
        // Remove preferred locales from worker thread context.
        PreferredLocales.reset();
    }
}
