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

package org.datalift.fwk.i18n;


import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Formats and parses dates according to the
 * <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO-8601</a> format,
 * in a thread-safe manner.
 *
 * @author lbihanic
 */
public class Iso8601DateFormat extends SimpleDateFormat
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** ISO-8601 date-only format {@link SimpleDateFormat pattern}. */
    public final static String DATE_PATTERN  = "yyyy-MM-dd";
    /**
     * ISO-8601 time-only format {@link SimpleDateFormat pattern},
     * with timezone.
     */
    public final static String TIME_TZ_PATTERN  = "HH:mm:ssZ";
    /** ISO-8601 UTC time-only format {@link SimpleDateFormat pattern}. */
    public final static String TIME_UTC_PATTERN = "HH:mm:ss'Z'";
    /**
     * ISO-8601 date and time format {@link SimpleDateFormat pattern},
     * with timezone.
     */
    public final static String DATETIME_TZ_PATTERN  =
                                        DATE_PATTERN + "'T'" + TIME_TZ_PATTERN;
    /** ISO-8601 UTC date and time format {@link SimpleDateFormat pattern}. */
    public final static String DATETIME_UTC_PATTERN =
                                        DATE_PATTERN + "'T'" + TIME_UTC_PATTERN;

    /** The supported ISO-8601 date formats. */
    public enum Format {
        /** Date-only format. */
        Date    (DATE_PATTERN),
        /** Time-only format. */
        Time    (TIME_TZ_PATTERN, TIME_UTC_PATTERN),
        /** Date and time format. */
        DateTime(DATETIME_TZ_PATTERN, DATETIME_UTC_PATTERN);

        private final String tzPattern;
        private final String utcPattern;

        Format(String pattern) {
            this(pattern, pattern);
        }

        Format(String tzPattern, String utcPattern) {
            this.tzPattern  = tzPattern;
            this.utcPattern = utcPattern;
        }

        public String getFormat(boolean utc) {
            return (utc)? this.utcPattern: this.tzPattern;
        }
    }

    /**
     * ISO-8601 date time format, with local timezone.
     */
    public final static Iso8601DateFormat DATETIME_TZ  =
                                new Iso8601DateFormat(Format.DateTime, false);
    /** ISO-8601 UTC date time format. */
    public final static Iso8601DateFormat DATETIME_UTC =
                                new Iso8601DateFormat(Format.DateTime, true);

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final boolean utc;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructs a <code>Iso8601DateFormat</code> for
     * outputting/parsing date and time using the local timezone.
     */
    public Iso8601DateFormat() {
        this(Format.DateTime);
    }

    /**
     * Constructs a <code>Iso8601DateFormat</code> with the specified
     * format and using the local timezone.
     * @param  format   the ISO-8601 date format to apply.
     */
    public Iso8601DateFormat(Format format) {
        this(format, false);
    }

    /**
     * Constructs a <code>Iso8601DateFormat</code> for
     * outputting/parsing date and time in UTC or the local timezone.
     * @param  utc   whether to use UTC or local timezone.
     */
    public Iso8601DateFormat(boolean utc) {
        this(Format.DateTime, utc);
    }

    /**
     * Constructs a <code>Iso8601DateFormat</code> with the specified
     * format.
     * @param  format   the ISO-8601 date format to apply.
     * @param  utc      whether to use UTC or local timezone.
     */
    public Iso8601DateFormat(final Format mode, final boolean utc) {
        super(mode.getFormat(utc));
        if (utc) {
            super.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        super.setLenient(false);
        this.utc = utc;
    }

    //-------------------------------------------------------------------------
    // DateFormat contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public synchronized StringBuffer format(Date date, StringBuffer toAppendTo,
                                            FieldPosition pos) {
        return super.format(date, toAppendTo, pos);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Date parse(String text, ParsePosition pos) {
        return super.parse(text, pos);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if mode is UTC. */
    @Override
    public synchronized void setTimeZone(TimeZone zone) {
        if (this.utc) {
            throw new IllegalStateException();
        }
        else {
            super.setTimeZone(zone);
        }
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void setCalendar(Calendar newCalendar) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void setLenient(boolean lenient) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void setNumberFormat(NumberFormat newNumberFormat) {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // SimpleDateFormat contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void applyPattern (String pattern) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void applyLocalizedPattern(String pattern) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void set2DigitYearStart(Date startDate) {
        super.set2DigitYearStart(startDate);
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        throw new UnsupportedOperationException();
    }
}
