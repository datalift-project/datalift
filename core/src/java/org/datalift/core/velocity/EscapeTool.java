package org.datalift.core.velocity;

import org.apache.commons.lang3.StringEscapeUtils;


/**
 * An extended version to Velocity EscapeTool.
 *
 * @author lbihanic
 */
public class EscapeTool extends org.apache.velocity.tools.generic.EscapeTool
{
    public EscapeTool() {
       super();
    }

    // ------------------------------------------------------------------------
    // EscapeTool contract definition
    // ------------------------------------------------------------------------

    /**
     * Escapes the characters in a <code>String</code> using
     * JavaScript String rules.
     * @param  string   the string to escape values, may be
     *                  <code>null</code>.
     * @return String with escaped values, empty if null string input.
     */
    public final String js(Object string) {
        return (string != null)? this.javascript(string): "";
    }

    /**
     * Escapes the characters in a <code>String</code> using
     * JavaScript String rules.
     * @param  string   the string to escape values, may be
     *                  <code>null</code>.
     * @return String with escaped values, empty if null string input.
     */
    public final String json(Object string) {
        return (string != null)?
                    StringEscapeUtils.escapeJson(String.valueOf(string)): null;
    }

    public final String asJson(Object o) {
        String json = "null";
        if (o instanceof Number) {
            json = String.valueOf(o);
        }
        else if (o instanceof Boolean) {
            json = ((Boolean)o).toString();
        }
        else if (o != null) {
            json = '"' + this.json(o) + '"';
        }
        return json;
    }
}
