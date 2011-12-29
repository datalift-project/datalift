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

package org.datalift.fwk.util;


import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;


/**
 * A set of utility methods for manipulating strings.
 *
 * @author lbihanic
 */
public final class StringUtils
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor, private on purpose.
     * @throws UnsupportedOperationException always.
     */
    private StringUtils() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // StringUtils contract definition
    //-------------------------------------------------------------------------

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
    public static String trim(String s) {
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
        s = trim(s);
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
     * @throws IllegalArgumentException if <code>s</code> is
     *         <code>null</code> or no valid URL can be extracted.
     */
    public static String urlify(String s) {
        return urlify(s, -1);
    }

    private static final Pattern DIACRITICS_AND_FRIENDS = Pattern.compile(
                    "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");
    private final static String CHARS_TO_REPLACE = "ÆŒæœßø";
    private final static String[] REPLACEMENT_CHARS =
                                        { "Ae", "Oe", "ae", "oe", "ss", "o" };
    /**
     * Concert the specified string so that is can be used as a path
     * element in a URL, truncating it if need be.
     * @param  s           the string to convert.
     * @param  maxLength   the maximum length allowed for the converted
     *                     string.
     *
     * @return a string suitable for being included in a URL.
     * @throws IllegalArgumentException if <code>s</code> is
     *         <code>null</code> or no valid URL can be extracted.
     */
    public static String urlify(String s, int maxLength) {
        if (! isSet(s)) {
            throw new IllegalArgumentException("s");
        }
        // Replace special characters.
        StringBuilder t = new StringBuilder(s.length() * 2);
        for (char c : s.toCharArray()) {
            int k = CHARS_TO_REPLACE.indexOf(c);
            if (k == -1) {
                t.append(c);
            }
            else {
                t.append(REPLACEMENT_CHARS[k]);
            }
        }
        // Remove accents.
        String u = Normalizer.normalize(t.toString(), Form.NFKD);
        u = DIACRITICS_AND_FRIENDS.matcher(u).replaceAll("");
        // Convert to lower cases.
        u = u.toLowerCase();
        // Remove punctuation characters and convert multiple spaces/hyphens
        // into a single space.
        u = u.replaceAll("[\\s-./'\"_]+", " ").trim()
             .replaceAll("[\\p{Punct}§()]", "");
        // Cut if requested.
        if (maxLength > 0) {
            u = u.substring(0, Math.min(u.length(), maxLength)).trim();
        }
        // Replace spaces with hyphens and clean up the remaining mess!
        t.setLength(0);
        for (int i=0, max=u.length(); i<max; i++) {
            char c = u.charAt(i);
            if (c == ' ') {
                t.append('-');
            }
            else if (Character.isLetterOrDigit(c)) {
                t.append(c);
            }
            // Else: ignore character.
        }
        // Remove heading and trailing dashes.
        int i = 0;
        while (t.charAt(i) == '-') i++;
        int j = t.length() - 1;
        while (t.charAt(j) == '-') j--;
        u = (j > i)? t.substring(i, j + 1): "";
        // Check there's something left with all those characters removed!
        if (u.length() == 0) {
            throw new IllegalArgumentException(s);
        }
        return u;
    }

    /**
     * Returns whether two strings are equals, taking care of null
     * references.
     * @param  s1   the first string to compare, may be
     *              <code>null</code>.
     * @param  s2   the second string to compare, may be
     *              <code>null</code>.
     *
     * @return <code>true</code> if both strings are <code>null</code>
               or equal; <code>false</code> otherwise.
     */
    public static boolean equals(String s1, String s2) {
        return (s1 == null)? (s2 == null): s1.equals(s2);
    }

    private static final String HEX_CHARS = "0123456789abcdef";

    /**
     * Returns the hexadecimal string representation of the specified
     * byte array.
     * @param  raw   a byte array, may be <code>null</code>.
     *
     * @return the hexadecimal string representation of the byte array
     *         content or <code>null</code>.
     */
    public static String toString(byte[] raw) {
        String v = null;
        if (raw != null) {
            final StringBuilder hex = new StringBuilder(2 * raw.length);
            for (final byte b : raw) {
                hex.append(HEX_CHARS.charAt((b & 0xF0) >> 4))
                   .append(HEX_CHARS.charAt((b & 0x0F)));
            }
            v = hex.toString();
        }
        return v;
    }
}
