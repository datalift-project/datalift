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
import java.io.Writer;
import java.util.List;
import java.util.Locale;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.resource.Resource;

import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;


/**
 * Supplementary {@link Directive Velocity directive} for including
 * internationalized resources based on the user's
 * {@link PreferredLocales preferred locales} retrieved from the HTTP
 * <code>Accept-Language</code> header sent by the Web browser.
 *
 * @author lbihanic
 */
public class I18ncludeDirective extends InputBase
{
    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** The name for this directive in Velocity templates. */
    public static final String NAME = "i18nclude";

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Directive contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @return the name of this directive: "<code>i18nclude</code>".
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     * @return the directive type: {@link #LINE}.
     */
    @Override
    public int getType() {
        return LINE;
    }

    /**
     * Since there is no processing of content,
     * there is never a need for an internal scope.
     * @return <code>false</code>.
     */
    @Override
    public boolean isScopeProvided() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node)
                        throws IOException, ResourceNotFoundException,
                               MethodInvocationException {
        // Get the user's preferred locales, most wanted first.
        final List<Locale> locales = PreferredLocales.get();
        // Include all requested resources.
        for (int i=0; i<node.jjtGetNumChildren(); i++) {
            Object o = node.jjtGetChild(i).value(context);
            if (o != null) {
                // Parse resource name to prepare insert locale.
                String rawName = String.valueOf(o);
                int n = rawName.lastIndexOf('.');
                String prefix = (n != -1)? rawName.substring(0, n): rawName;
                String suffix = (n != -1)? rawName.substring(n): "";

                Resource r = null;
                for (Locale l : locales) {
                    // Compute resource name for locale.
                    StringBuilder buf = new StringBuilder(prefix);
                    if (l != Locale.ROOT) {
                        buf.append('_').append(l.toString());
                    }
                    String name = buf.append(suffix).toString();
                    // Load resource using Velocity resource loaders.
                    if (this.rsvc.getLoaderNameForResource(name) != null) {
                        // Localized version found. => Render it.
                        try {
                            r = rsvc.getContent(name,
                                                this.getInputEncoding(context));
                            writer.write((String)(r.getData()));
                        }
                        catch (Exception e) {
                            log.error("Failed to load I18N resource {}", e,
                                      name);
                        }
                        // Resource found. => Break out of Locale loop.
                        break;
                    }
                    // Else: No resource available for this locale. Try next.
                }
                if (r == null) {
                    // No localized version of the resource was found that
                    // matched the user locales (not even the default one).
                    log.warn("Can't find I18N resource {}", rawName);
                }
            }
        }
        return true;
    }
}
