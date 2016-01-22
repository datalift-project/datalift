package org.datalift.core.velocity;


public class EscapeTool extends org.apache.velocity.tools.generic.EscapeTool
{
    public EscapeTool() {
       super();
    }

    /**
     * Escapes the characters in a <code>String</code> using
     * JavaScript String rules.
     * @param  string   the string to escape values, may be
     *                  <code>null</code>.
     * @return String with escaped values, <code>null</code> if null
     *         string input.
     */
    public final String js(Object string) {
        return this.javascript(string);
    }

    /**
     * Escapes the characters in a <code>String</code> using
     * JavaScript String rules.
     * @param  string   the string to escape values, may be
     *                  <code>null</code>.
     * @return String with escaped values, <code>null</code> if null
     *         string input.
     */
    public final String json(Object string) {
        return this.js(string);
    }
}
