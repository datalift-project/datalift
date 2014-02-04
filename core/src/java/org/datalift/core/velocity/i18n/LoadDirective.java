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

package org.datalift.core.velocity.i18n;


import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import org.datalift.core.util.SimpleCache;
import org.datalift.core.velocity.i18n.BundleList.Bundle;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;


/**
 * Supplementary {@link Directive Velocity directive} for loading
 * internationalized messages definition resource bundles based on
 * the user's {@link PreferredLocales preferred locales} retrieved
 * from the HTTP <code>Accept-Language</code> header sent by the
 * Web browser.
 *
 * @author lbihanic
 */
public class LoadDirective extends Directive
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static int CACHE_DURATION = 3600 * 6;         // 6 hours.

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** A cache of loaded i18n resource bundles. */
    private final static SimpleCache<String,Properties> bundleCache =
                    new SimpleCache<String,Properties>(1000, CACHE_DURATION);

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Directive contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @return The name of this directive: "<code>load</code>".
     */
    @Override
    public String getName() {
        return "load";
    }

    /**
     * {@inheritDoc}
     * @return The directive type: {@link #LINE}.
     */
    @Override
    public int getType() {
        return LINE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node)
                        throws IOException, ResourceNotFoundException,
                               ParseErrorException, MethodInvocationException {
        // Get a local copy of the user locales (unmodifiable list).
        List<Locale> locales = new ArrayList<Locale>(PreferredLocales.get());
        // Reverse locale list to get least wanted locales first.
        Collections.reverse(locales);
        // Get existing bundle list, to add new bundles.
        BundleList bundles = (BundleList)(context.get(BundleList.KEY));
        if (bundles == null) {
            bundles = new BundleList();
            context.put(BundleList.KEY, bundles);
        }

        // Load requested bundles to add them to the bundle list. The first
        // loaded bundles are the ones with the least priority so that they
        // be parent the the ones with higher priority as they are only queried
        // if the key is not found.
        for (int i=0; i<node.jjtGetNumChildren(); i++) {
            String name = String.valueOf(node.jjtGetChild(i).value(context));

            Bundle b = null;
            for (Locale locale : locales) {
                // Build properties resource bundle name for locale.
                StringBuilder buf = new StringBuilder(name);
                if (locale != Locale.ROOT) {
                    buf.append('_').append(locale);
                }
                String propName = buf.append(".properties").toString();

                Properties props = bundleCache.get(propName);
                if (props == null) {
                    // Load resource bundle.
                    if (this.rsvc.getLoaderNameForResource(propName) != null) {
                        try {
                            // Force encoding as requested by Java Properties.
                            Object o = this.rsvc.getContent(propName,
                                                        "ISO-8859-1").getData();
                            Properties p = new Properties();
                            p.load(new StringReader((String)o));
                            props = p;
                            bundleCache.put(propName, p);
                        }
                        catch (Exception e) {
                            log.error("Failed to load resource bundle {}", e,
                                      propName);
                        }
                    }
                    // Else: Properties resource bundle not found. => Ignore...
                }
                if (props != null) {
                    b = BundleList.newBundle(props, b);
                }
            }
            if (b != null) {
                bundles.add(b);
            }
            else {
                log.warn("Can't find bundle {}", name);
            }
        }
        return true;
    }
}
