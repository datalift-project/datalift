package org.datalift.fwk.util.web;


import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.datalift.fwk.util.ThreadSafeDateFormat;


public class HttpDateFormat extends ThreadSafeDateFormat
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

    private final static HttpDateFormat formats[] =
                        new HttpDateFormat[] { RFC_1123, RFC_1036, ASCTIME};

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public HttpDateFormat() {
        super(RFC_1123_DATE_PATTERN, Locale.US, true);
    }

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
