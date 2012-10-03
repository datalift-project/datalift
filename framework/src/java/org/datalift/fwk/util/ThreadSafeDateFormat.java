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


import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * A {@Link SimpleDateFormat date format} that can be shared among
 * threads.
 *
 * @author lbihanic
 */
abstract public class ThreadSafeDateFormat extends SimpleDateFormat
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final boolean utc;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructs a <code>DateFormat</code> with the specified
     * format and using the default locale and timezone.
     * @param  pattern   the date format pattern.
     */
    public ThreadSafeDateFormat(String pattern) {
        this(pattern, null, false);
    }

    /**
     * Constructs a <code>DateFormat</code> with the specified
     * format and locale, using the local timezone.
     * @param  pattern   the date format pattern.
     */
    public ThreadSafeDateFormat(String pattern, Locale locale) {
        this(pattern, locale, false);
    }

    /**
     * Constructs a <code>DateFormat</code> with the specified
     * format, using the default locale.
     * @param  pattern   the date format pattern.
     * @param  utc       whether to use UTC or local timezone.
     */
    public ThreadSafeDateFormat(final String pattern, final boolean utc) {
        this(pattern, null, utc);
    }

    /**
     * Constructs a <code>DateFormat</code> with the specified
     * format.
     * @param  pattern   the date format pattern.
     * @param  utc       whether to use UTC or local timezone.
     */
    public ThreadSafeDateFormat(final String pattern,
                                final Locale locale, final boolean utc) {
        super(pattern, (locale != null)? locale: Locale.getDefault());
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

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Formats a date expressed as a number of milliseconds
     * (since midnight, January 1, 1970 UTC) into a date/time string.
     * @param  millis   the date/time to be formatted into a string.
     *
     * @return the formatted date/time string.
     */
    public final String format(long millis) {
        return this.format(new Date(millis));
    }
}
