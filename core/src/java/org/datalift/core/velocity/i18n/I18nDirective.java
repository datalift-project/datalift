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
import java.text.MessageFormat;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import org.datalift.fwk.log.Logger;


/**
 * Supplementary {@link Directive Velocity directive} for substituting
 * an internationalized message identifier with the best available
 * translation {@link LoadDirective loaded from resource bundles}
 * based on the user's preferred locales retrieved from the HTTP
 * <code>Accept-Language</code> header sent by the Web browser.
 *
 * @author lbihanic
 */
public class I18nDirective extends Directive
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Directive contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @return The name of this directive: "<code>i18n</code>".
     */
    public String getName() {
        return "i18n";
    }

    /**
     * {@inheritDoc}
     * @return The directive type: {@link #LINE}.
     */
    public int getType() {
        return LINE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Call syntax: #i18n(message_key, arg1, arg2, ... argN)</p>
     * @return <code>true</code> if message_key exists in the resource
     *         bundles {@link LoadDirective associated to the context};
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node)
                        throws IOException, ResourceNotFoundException,
                               ParseErrorException, MethodInvocationException {
        String key = String.valueOf(node.jjtGetChild(0).value(context));
        // Get bundles from context.
        BundleList b = (BundleList)(context.get(BundleList.KEY));
        if (b != null) {
            // Get value from bundles
            String msg = b.getValue(key);
            int params = node.jjtGetNumChildren();
            if ((params > 1) && (msg.indexOf('{') != -1)) {
                // Replaces message format parameters with arguments.
                Object[] args = new Object[params - 1];
                for (int i=1; i<params; i++) {
                    args[i-1] = node.jjtGetChild(i).value(context);
                }
                msg = MessageFormat.format(msg, args);
            }
            log.trace("{}: Render {} -> {}", this.getName(), key, msg);
            writer.write(msg);
            return true;
        }
        else {
            log.warn("{}: Failed to resolved key \"{}\"" +
                    ": no bundle defined in template {}",
                    this.getName(), key, node.getTemplateName());
            return false;
        }
    }
}
