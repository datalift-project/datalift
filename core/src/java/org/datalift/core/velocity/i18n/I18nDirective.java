package org.datalift.core.velocity.i18n;


import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import org.datalift.fwk.log.Logger;


public class I18nDirective extends Directive
{
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
     * @return The directive type: {@link LINE}.
     */
    public int getType() {
        return LINE;
    }

    /** {@inheritDoc} */
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
            writer.write(b.get(key));
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
