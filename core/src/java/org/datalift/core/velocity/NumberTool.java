package org.datalift.core.velocity;

import java.util.regex.Pattern;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;


/**
 * An extended version to Velocity NumberTool.
 *
 * @author lbihanic
 */
public class NumberTool extends org.apache.velocity.tools.generic.NumberTool
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Regex to identify integer XML schema data types. */
    private final static Pattern INTEGER_TYPES_PATTERN = Pattern.compile(
                                    "(int|.*[iI]nteger|.*[lL]ong|.*[sS]hort)$");
    /** Regex to identify double XML schema data types. */
    private final static Pattern DOUBLE_TYPES_PATTERN = Pattern.compile(
                                    "(float|decimal|double)$");

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public NumberTool() {
        super();
    }

    // ------------------------------------------------------------------------
    // NumberTool contract support
    // ------------------------------------------------------------------------

    /** {@inheritDoc} */ 
    @Override
    public Number toNumber(Object obj) {
        Number n = null;
        if (obj instanceof Literal) {
            Literal l = (Literal)obj;
            URI dataType = l.getDatatype();
            if (dataType != null) {
                String dt = dataType.getLocalName();
                if (DOUBLE_TYPES_PATTERN.matcher(dt).find()) {
                    n = Double.valueOf(l.doubleValue());
                }
                else if (INTEGER_TYPES_PATTERN.matcher(dt).find()) {
                    n = Long.valueOf(l.longValue());
                }
                // Else: not a number.
            }
        }
        else {
            n = super.toNumber(obj);
        }
        return n;
    }
}
