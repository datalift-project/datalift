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


/**
 * A set of utility methods for converting primitive types to object
 * wrappers and vice versa.
 * <p>
 * Some wrapping methods are missing on purpose as usage of some types
 * (e.g. short, float) is discouraged.</p>
 *
 * @author lbihanic
 */
public class PrimitiveUtils
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor, private on purpose.
     * @throws UnsupportedOperationException always.
     */
    private PrimitiveUtils() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // PrimitiveUtils contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  b   a boolean value.
     *
     * @return a <code>Boolean</code> object representing
     *         <code>b</code>.
     * @see    Boolean#valueOf(boolean)
     */
    public static Boolean wrap(boolean b) {
        return Boolean.valueOf(b);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  b   a <code>Boolean</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>false</code> if <code>b</code> is <code>null</code>.
     * @see    #booleanValue(Boolean)
     */
    public static boolean unwrap(Boolean b) {
        return booleanValue(b);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  b     a <code>Boolean</code> object.
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>b</code> is <code>null</code>.
     * @see    #booleanValue(Boolean, boolean)
     */
    public static boolean unwrap(Boolean b, boolean def) {
        return booleanValue(b, def);
    }

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  b   a byte value.
     *
     * @return a <code>Byte</code> instance representing <code>b</code>.
     * @see    Byte#valueOf(byte)
     */
    public static Byte wrap(byte b) {
        return Byte.valueOf(b);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  b   a <code>Byte</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>0x00</code> if <code>b</code> is <code>null</code>.
     * @see    #byteValue(Number)
     */
    public static byte unwrap(Byte b) {
        return byteValue(b);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  b     a <code>Byte</code> object.
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>b</code> is <code>null</code>.
     * @see    #byteValue(Number, int)
     */
    public static byte unwrap(Byte b, int def) {
        return byteValue(b, def);
    }

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  c   a character.
     *
     * @return a <code>Character</code> object representing
     *         <code>c</code>.
     * @see    Character#valueOf(char)
     */
    public static Character wrap(char c) {
        return Character.valueOf(c);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  c   a <code>Character</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>0x00</code> if <code>c</code> is <code>null</code>.
     * @see    #charValue(Character)
     */
    public static char unwrap(Character c) {
        return charValue(c);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  c     a <code>Character</code> object
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>c</code> is <code>null</code>.
     * @see    #charValue(Character, char)
     */
    public static char unwrap(Character c, char def) {
        return charValue(c, def);
    }

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  i   an integer value.
     *
     * @return an <code>Integer</code> instance representing
     *         <code>i</code>.
     * @see    Integer#valueOf(int)
     */
    public static Integer wrap(int i) {
        return Integer.valueOf(i);
    }

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  i     an integer value.
     * @param  def   the default value.
     *
     * @return an <code>Integer</code> instance representing
     *         <code>i</code> or <code>null</code> if <code>i</code>
     *         equals to <code>def</code>.
     * @see    #wrap(int)
     */
    public static Integer wrap(int i, int def) {
        return (i != def)? wrap(i): null;
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  i   an <code>Integer</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>0</code> if <code>i</code> is <code>null</code>.
     * @see    #intValue(Number)
     */
    public static int unwrap(Integer i) {
        return intValue(i);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  i     an <code>Integer</code> object.
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>i</code> is <code>null</code>.
     * @see    #intValue(Number, int)
     */
    public static int unwrap(Integer i, int def) {
        return intValue(i, def);
    }

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  l   a long value.
     *
     * @return a <code>Long</code> instance representing <code>l</code>.
     * @see    Long#valueOf(long)
     */
    public static Long wrap(long l) {
        return Long.valueOf(l);
    }

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  l     a long value.
     * @param  def   the default value.
     *
     * @return a <code>Long</code> instance representing <code>l</code>
     *         or <code>null</code> if <code>l</code> equals to
     *         <code>def</code>.
     * @see    #wrap(long)
     */
    public static Long wrap(long l, long def) {
        return (l != def)? wrap(l): null;
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  l   a <code>Long</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>0L</code> if <code>l</code> is <code>null</code>.
     * @see    #longValue(Number)
     */
    public static long unwrap(Long l) {
        return longValue(l);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  l     a <code>Long</code> object.
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>l</code> is <code>null</code>.
     * @see    #longValue(Number, long)
     */
    public static long unwrap(Long l, long def) {
        return longValue(l, def);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  s   a <code>Short</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>0</code> if <code>s</code> is <code>null</code>.
     * @see    #shortValue(Number)
     */
    public static short unwrap(Short s) {
        return shortValue(s);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  s     a <code>Short</code> object.
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>s</code> is <code>null</code>.
     * @see    #shortValue(Number, int)
     */
    public static short unwrap(Short s, int def) {
        return shortValue(s, def);
    }

    /**
     * Returns a wrapper object representing the specified primitive
     * value.
     * @param  d   a double value.
     *
     * @return a <code>Double</code> instance representing
     *         <code>d</code>.
     * @see    Double#valueOf(double)
     */
    public static Double wrap(double d) {
        return Double.valueOf(d);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  d   a <code>Double</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>0.0</code> if <code>d</code> is <code>null</code>.
     * @see    #doubleValue(Number)
     */
    public static double unwrap(Double d) {
        return doubleValue(d);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  d     a <code>Double</code> object.
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>d</code> is <code>null</code>.
     * @see    #doubleValue(Number, double)
     */
    public static double unwrap(Double d, double def) {
        return doubleValue(d, def);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  f   a <code>Float</code> object or <code>null</code>.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>0.0</code> if <code>f</code> is <code>null</code>.
     * @see    #floatValue(Number)
     */
    public static float unwrap(Float f) {
        return floatValue(f);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  f     a <code>Float</code> object.
     * @param  def   the default value.
     *
     * @return the primitive value wrapped by the specified object;
     *         <code>def</code> if <code>f</code> is <code>null</code>.
     * @see    #floatValue(Number, float)
     */
    public static float unwrap(Float f, float def) {
        return floatValue(f, def);
    }

    /**
     * Returns the boolean value wrapped by the specified
     * <code>Boolean</code> object.
     * @param  b   a <code>Boolean</code> object or <code>null</code>.
     *
     * @return the boolean value wrapped by the specified object;
     *         <code>false</code> if <code>b</code> is <code>null</code>.
     * @see    #booleanValue(Boolean, boolean)
     */
    public static boolean booleanValue(Boolean b) {
        return booleanValue(b, false);
    }

    /**
     * Returns the primitive value wrapped by the specified object.
     * @param  b     a <code>Boolean</code> object.
     * @param  def   the default value.
     *
     * @return the boolean value wrapped by the specified object;
     *         <code>def</code> if <code>b</code> is <code>null</code>.
     * @see    Boolean#booleanValue(Boolean)
     */
    public static boolean booleanValue(Boolean b, boolean def) {
        return (b != null)? b.booleanValue(): def;
    }

    /**
     * Returns the char value wrapped by the specified
     * <code>Character</code> object.
     * @param  c   a <code>Character</code> object or <code>null</code>.
     *
     * @return the value wrapped by the specified <code>Character</code>
     *         as a <code>char</code>; <code>0x00</code> if
     *         <code>c</code> is <code>null</code>.
     * @see    #charValue(Character, char)
     */
    public static char charValue(Character c) {
        return charValue(c, (char)0x00);
    }

    /**
     * Returns the char value wrapped by the specified
     * <code>Character</code> object.
     * @param  c     a <code>Character</code> object.
     * @param  def   the default value.
     *
     * @return the value wrapped by the specified <code>Character</code>
     *         as a <code>char</code>; <code>def</code> if
     *         <code>c</code> is <code>null</code>.
     * @see    Character#charValue()
     */
    public static char charValue(Character c, char def) {
        return (c != null)? c.charValue(): def;
    }

    /**
     * Returns the byte value wrapped by the specified
     * <code>Number</code> object.
     * @param  n   a <code>Number</code> object or <code>null</code>.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a byte; <code>0x00</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    #byteValue(Number, int)
     */
    public static byte byteValue(Number n) {
        return byteValue(n, 0x00);
    }

    /**
     * Returns the byte value wrapped by the specified
     * <code>Number</code> object.
     * @param  n     a <code>Number</code> object.
     * @param  def   the default value.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a byte; <code>def</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    Number#byteValue()
     */
    public static byte byteValue(Number n, int def) {
        return (n != null)? n.byteValue(): (byte)def;
    }

    /**
     * Returns the integer value wrapped by the specified
     * <code>Number</code> object.
     * @param  n   a <code>Number</code> object or <code>null</code>.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as an integer; <code>0</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    #intValue(Number, int)
     */
    public static int intValue(Number n) {
        return intValue(n, 0);
    }

    /**
     * Returns the integer value wrapped by the specified
     * <code>Number</code> object.
     * @param  n     a <code>Number</code> object.
     * @param  def   the default value.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as an integer; <code>def</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    Number#intValue()
     */
    public static int intValue(Number n, int def) {
        return (n != null)? n.intValue(): def;
    }

    /**
     * Returns the long value wrapped by the specified
     * <code>Number</code> object.
     * @param  n   a <code>Number</code> object or <code>null</code>.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a long; <code>0L</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    #longValue(Number, long)
     */
    public static long longValue(Number n) {
        return longValue(n, 0L);
    }

    /**
     * Returns the long value wrapped by the specified
     * <code>Number</code> object.
     * @param  n     a <code>Number</code> object.
     * @param  def   the default value.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a long; <code>def</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    Number#longValue()
     */
    public static long longValue(Number n, long def) {
        return (n != null)? n.longValue(): def;
    }

    /**
     * Returns the short value wrapped by the specified
     * <code>Number</code> object.
     * @param  n   a <code>Number</code> object or <code>null</code>.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a short; <code>0</code> if <code>b</code> is
     *         <code>null</code>.
     * @see    #shortbyteValue(Number, int)
     */
    public static short shortValue(Number n) {
        return shortValue(n, 0);
    }

    /**
     * Returns the short value wrapped by the specified
     * <code>Number</code> object.
     * @param  n     a <code>Number</code> object.
     * @param  def   the default value.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a short; <code>def</code> if <code>b</code> is
     *         <code>null</code>.
     * @see    Number#shortValue()
     */
    public static short shortValue(Number n, int def) {
        return (n != null)? n.shortValue(): (short)def;
    }

    /**
     * Returns the double value wrapped by the specified
     * <code>Number</code> object.
     * @param  n   a <code>Number</code> object or <code>null</code>.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a double; <code>0.0</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    #doubleValue(Number, double)
     */
    public static double doubleValue(Number n) {
        return doubleValue(n, 0.0);
    }

    /**
     * Returns the double value wrapped by the specified
     * <code>Number</code> object.
     * @param  n     a <code>Number</code> object.
     * @param  def   the default value.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a double; <code>def</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    Number#doubleValue()
     */
    public static double doubleValue(Number n, double def) {
        return (n != null)? n.doubleValue(): def;
    }

    /**
     * Returns the float value wrapped by the specified
     * <code>Number</code> object.
     * @param  n   a <code>Number</code> object or <code>null</code>.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a float; <code>0.0f</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    #floatValue(Number, float)
     */
    public static float floatValue(Number n) {
        return floatValue(n, 0.0f);
    }

    /**
     * Returns the float value wrapped by the specified
     * <code>Number</code> object.
     * @param  n     a <code>Number</code> object.
     * @param  def   the default value.
     *
     * @return the value wrapped by the specified <code>Number</code>
     *         as a float; <code>def</code> if <code>n</code> is
     *         <code>null</code>.
     * @see    Number#floatValue()
     */
    public static float floatValue(Number n, float def) {
        return (n != null)? n.floatValue(): def;
    }
}
