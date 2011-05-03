package org.datalift.fwk.util;


import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collection;


public class StringUtils
{
    private StringUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if a string is neither empty ("") nor <code>null</code>.
     * @param  s   the string to check, may be <code>null</code>.
     *
     * @return <code>true</code> if the string is neither empty ("")
     *         nor <code>null</code>.
     */
    public static boolean isSet(String s) {
        return ((s != null) && (s.length() != 0));
    }

    /**
     * Check if a string is <code>null</code>, empty or contains only
     * whitespace characters.
     * @param  s   the string to check, may be <code>null</code>.
     *
     * @return <code>true</code> if the string is <code>null<code>,
     *         empty ("") or contains only whitespace characters.
     */
    public static boolean isBlank(String s) {
        return ((s == null) || (s.trim().length() == 0));
    }

    /**
     * Returns a copy of a string, with leading and trailing whitespace
     * characters removed, never <code>null</code>.
     * @param  s   the string to trim, may be <code>null</code>.
     *
     * @return A copy of the string with leading and trailing
     *         whitespaces removed. If the input string is
     *         <code>null</code>, an empty string is returned.
     */
    public static String trimToEmpty(String s) {
        return (s == null)? "": s.trim();
    }

    /**
     * Returns a copy of a string, with leading and trailing whitespace
     * characters removed, or <code>null</code> if the trim result is
     * an empty string.
     * @param  s   the string to trim, may be <code>null</code>.
     *
     * @return A copy of the string with leading and trailing
     *         whitespaces removed or <code>null</code> if the trim
     *         result is an empty string.
     */
    public static String trimToNull(String s) {
        s = trimToEmpty(s);
        return (s.length() == 0)? null: s;
    }
    
    /**
     * Join collection elements to build a string.
     * @param  c     a collection.
     * @param  sep   separator of collection elements.
     *
     * @return a string containing the string representation of each
     *         element of the collection, separated by the specified
     *         separator.
     * @throws IllegalArgumentException if <code>sep</code> is
     *         <code>null</code>.
     */
    public static String join(Collection<?> c, String sep) {
        if (sep == null) {
            throw new IllegalArgumentException("sep");
        }
        if ((c == null) || (c.isEmpty())) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object element : c) {
            String elementValue = (element == null)? "": element.toString();
            sb.append(elementValue).append(sep);
        }
        // Remove last separator
        sb.setLength(sb.length() - sep.length());
        return sb.toString();
    }

    /**
     * Split a string into a collection of strings
     * @param  s       the string to split.
     * @param  regex   the separator as a regular expression.
     *
     * @return the resulting collection, possibly empty.
     * @throws IllegalArgumentException if <code>s</code> is
     *         <code>null</code> or <code>regex</code> is
     *         <code>null</code> or empty.
     */
    public static Collection<String> split(String s, String regex) {
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        if (! isSet(regex)) {
            throw new IllegalArgumentException("regex");
        }
        return Arrays.asList(s.split(regex, -1));
    }

    /**
     * Reverse a string, i.e. &quot;le sac a sel&quot; -&gt;
     * &quot;les a cas el&quot;.
     * @param  s   the string to reverse.
     *
     * @return the reversed string.
     * @throws IllegalArgumentException if <code>s</code> is
     *         <code>null</code>.
     */
    public static String reverse(String s) {
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        char[] c = s.toCharArray();
        for (int i=0, max=c.length/2, last=c.length-1; i<max; i++) {
            int j = last - i;
            char n = c[i];
            c[i] = c[j];
            c[j] = n;
        }
        return new String(c);
    }

    /**
     * Concert the specified string so that is can be used as a path
     * element in a URL.
     * @param  s   the string to convert.
     *
     * @return a string suitable for being included in a URL.
     */
    public static String urlify(String s) {
        return urlify(s, -1);
    }

    private final static String CHARS_TO_REPLACE = "ÆŒæœß";
    private final static String[] REPLACEMENT_CHARS =
                                            { "Ae", "Oe", "ae", "oe", "ss" };
    /**
     * Concert the specified string so that is can be used as a path
     * element in a URL, truncating it if need be.
     * @param  s           the string to convert.
     * @param  maxLength   the maximum length allowed for the converted
     *                     string.
     *
     * @return a string suitable for being included in a URL.
     */
    public static String urlify(String s, int maxLength) {
        if (! isSet(s)) {
            throw new IllegalArgumentException("s");
        }
        // Replace special characters.
        StringBuilder buf = new StringBuilder(s.length() * 2);
        for (char c : s.toCharArray()) {
            int k = CHARS_TO_REPLACE.indexOf(c);
            if (k == -1) {
                buf.append(c);
            }
            else {
                buf.append(REPLACEMENT_CHARS[k]);
            }
        }
        // Remove accents.
        String u = Normalizer.normalize(buf.toString(), Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Convert to lower cases.
        u = u.toLowerCase();
        // Remove punctuation characters and convert multiple spaces/hyphens
        // into a single space.
        u = u.replaceAll("[\\s-./'\"]+", " ").trim()
             .replaceAll("[\\p{Punct}§()]", "");
        // Cut if requested.
        if (maxLength > 0) {
            u = u.substring(0, Math.min(u.length(), maxLength)).trim();
        }
        // Replace spaces with hyphens.
        u = u.replace(' ', '-');
        if ((u.length() == 0) || (u.charAt(0) == '-')) {
            throw new IllegalArgumentException(s);
        }
        return u;
    }
}
