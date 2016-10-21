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

package org.datalift.fwk.util.web;


import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.datalift.fwk.util.ThreadSafeDateFormat;


/**
 * Formats and parses dates according to the
 * <a href="http://tools.ietf.org/html/rfc1123">RFC 1123</a> format,
 * in a thread-safe manner. Also supports parsing legacy date formats
 * such as <a href="http://tools.ietf.org/html/rfc1036">RFC 1036</a> and
 * <a href="http://www.cplusplus.com/reference/clibrary/ctime/asctime/">asctime()</a>.
 *
 * @author lbihanic
 */
public final class HttpDateFormat extends ThreadSafeDateFormat
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** RFC 1123 date format, the recommended format for HTTP header fields. */
    private static final String RFC_1123_DATE_PATTERN =
                                                "EEE, dd MMM yyyy HH:mm:ss zzz";
    /** RFC 1036 legacy date format. */
    private static final String RFC_1036_DATE_PATTERN =
                                                "EEEE, dd-MMM-yy HH:mm:ss zzz";
    /** ANSI C asctime() legacy date format. */
    private static final String ASCTIME_DATE_PATTERN =
                                                "EEE MMM d HH:mm:ss yyyy";

    /** RFC 1123 date format, the recommended format for HTTP header fields. */
    public final static HttpDateFormat RFC_1123 =
                                    new HttpDateFormat(RFC_1123_DATE_PATTERN);
    /** RFC 1036 legacy date format. */
    public final static HttpDateFormat RFC_1036 =
                                    new HttpDateFormat(RFC_1036_DATE_PATTERN);
    /** ANSI C asctime() legacy date format. */
    public final static HttpDateFormat ASCTIME =
                                    new HttpDateFormat(ASCTIME_DATE_PATTERN);

    /** The supported date formats as HttpDateFormat objects. */
    private final static HttpDateFormat formats[] =
                        new HttpDateFormat[] { RFC_1123, RFC_1036, ASCTIME};

    /** Default serialization version id. */
    private final static long serialVersionUID = 1L;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructs a <code>HttpDateFormat</code> for
     * outputting/parsing dates compliant with RFC 1123.
     */
    public HttpDateFormat() {
        super(RFC_1123_DATE_PATTERN, Locale.US, true);
    }

    /**
     * Constructs a <code>HttpDateFormat</code> for
     * outputting/parsing dates compliant with the specified format.
     * @param  pattern   the date format.
     */
    private HttpDateFormat(String pattern) {
        super(pattern, Locale.US, true);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Formats a date in a format suitable for being used as the value
     * of an HTTP header field.
     * @param  millis   the date to format, as a number of milliseconds
     *                  since midnight, January 1, 1970 UTC.
     *
     * @return the date, formatted in {@link #RFC_1123 RFC 1123} format.
     */
    public static String formatDate(long millis) {
        return formatDate(new Date(millis));
    }

    /**
     * Formats a date in a format suitable for being used as the value
     * of an HTTP header field.
     * @param  date   the date to format.
     *
     * @return the date, formatted in {@link #RFC_1123 RFC 1123} format.
     */
    public static String formatDate(Date date) {
        return RFC_1123.format(date);
    }

    /**
     * Parses a date string extracted from an HTTP header field,
     * attempting the supported date formats, starting with the
     * recommended one.
     * @param  date   the HTTP date string to parse.
     *
     * @return a Date object.
     * @throws ParseException if the date string does not match any
     *         of the supported date formats.
     */
    public static Date parseDate(String date) throws ParseException {
        ParseException error = null;
        for (HttpDateFormat f : formats) {
            try {
                return f.parse(date);
            }
            catch (ParseException e) {
                if (error == null) error = e;
            }
        }
        throw error;
    }
}
