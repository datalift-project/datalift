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
 * A set of utility methods for converting time units.
 *
 * @author lbihanic
 */
public final class TimeUtils
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Number of milliseconds in one second. */
    public final static long ONE_SECOND = 1000L;

    /** Number of milliseconds in one minute. */
    public final static long ONE_MINUTE = 60 * ONE_SECOND;

    /** Number of milliseconds in one hour. */
    public final static long ONE_HOUR   = 60 * ONE_MINUTE;

    /** Number of milliseconds in one day. */
    public final static long ONE_DAY    = 24 * ONE_HOUR;

    /** Number of milliseconds in one week. */
    public final static long ONE_WEEK   = 7 * ONE_DAY;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor, private on purpose.
     * @throws UnsupportedOperationException always.
     */
    private TimeUtils() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // PrimitiveUtils contract definition
    //-------------------------------------------------------------------------

    /**
     * Converts a number of milliseconds into seconds.
     * @param  millis   a number of milliseconds.
     * @return the number of seconds corresponding to
     *         <code>millis</code>.
     */
    public static double asSeconds(long millis) {
       return millis / (double)ONE_SECOND;
    }

    /**
     * Converts a number of milliseconds into minutes.
     * @param  millis   a number of milliseconds.
     * @return the number of minutes corresponding to
     *         <code>millis</code>.
     */
    public static double asMinutes(long millis) {
       return millis / (double)ONE_MINUTE;
    }

    /**
     * Converts a number of seconds into milliseconds.
     * @param  seconds   the number of seconds.
     * @return the number of milliseconds corresponding to
     *         </code>seconds</code>.
     */
    public static long fromSeconds(long seconds) {
        return seconds * ONE_SECOND;
    }
}
