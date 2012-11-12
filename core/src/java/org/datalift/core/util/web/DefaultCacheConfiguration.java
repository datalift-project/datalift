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

package org.datalift.core.util.web;


import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Calendar.*;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response.ResponseBuilder;

import static java.util.Calendar.HOUR_OF_DAY;

import static java.util.Calendar.DAY_OF_YEAR;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.web.CacheConfiguration;

import static org.datalift.fwk.util.StringUtils.*;


public class DefaultCacheConfiguration implements CacheConfiguration
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The default cache duration for static & RDF resources. */
    public final static String CACHE_DURATION_PROPERTY =
                                                "datalift.cache.duration";
    /** The business day opening hours. */
    public final static String BUSINESS_DAY_PROPERTY =
                                                "datalift.cache.businessDay";

    /** The default cache duration: 2 hours in seconds. */
    private final static int DEFAULT_CACHE_DURATION = 2 * 3600;
    /** The default business day: updates may occur from 8 A.M. to 8 P.M. */
    private final static BusinessDay DEFAULT_BUSINESS_DAY =
                                                    new BusinessDay(800, 2000);

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Cache management informations. */
    private final int cacheDuration;
    private final BusinessDay businessDay;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public DefaultCacheConfiguration(Configuration configuration) {
        this(configuration.getProperty(CACHE_DURATION_PROPERTY),
             configuration.getProperty(BUSINESS_DAY_PROPERTY), true);
    }

    public DefaultCacheConfiguration(String durationSpec,
                                     String businessDaySpec, boolean failsafe) {
        super();
        this.cacheDuration = parseCacheDuration(durationSpec,  failsafe);
        this.businessDay   = parseBusinessDay(businessDaySpec, failsafe);
    }

    public DefaultCacheConfiguration(int cacheDuration,
                                     int businessOpening, int businessClosing) {
        super();
        if (cacheDuration < 0) {
            throw new IllegalArgumentException("cacheDuration");
        }
        this.cacheDuration = cacheDuration;
        this.businessDay   = new BusinessDay(businessOpening, businessClosing);
    }

    //-------------------------------------------------------------------------
    // CacheConfiguration contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder addCacheDirectives(ResponseBuilder response,
                                              Date lastModified) {
        return this.addCacheDirectives(response, lastModified, false, false);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder addCacheDirectives(ResponseBuilder response,
                                              Date lastModified,
                                              boolean _private,
                                              boolean revalidate) {
        if (response == null) {
            throw new IllegalArgumentException("response");
        }
        long now = System.currentTimeMillis();
        int  duration = 0;
        Date expiry = null;
        if (this.businessDay.isInBusinessDay(null)) {
            // Cache entries for specified duration during the business day.
            duration = this.cacheDuration;
            expiry   = new Date(now  + (duration * 1000L));
        }
        else {
            // No data updates occur between close and opening of business.
            // => Set expiry date to opening of next business day.
            expiry   = this.businessDay.getNextBusinessDay();
            duration = (int)((expiry.getTime() - now) / 1000L);
        }
        CacheControl cc = new CacheControl();
        if (duration > 0) {
            cc.setMaxAge(duration);
            cc.setPrivate(_private);
            cc.setMustRevalidate(revalidate);
            cc.setProxyRevalidate(revalidate);
        }
        else {
            // Caching disabled.
            cc.setNoCache(true);
        }
        response = response.cacheControl(cc)
                           .expires(expiry);
        // Set last modified date, if provided.
        if (lastModified != null) {
            response = response.lastModified(lastModified);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private static int parseCacheDuration(String durationSpec,
                                          boolean failSafe) {
        int cacheDuration = DEFAULT_CACHE_DURATION;
        if (! isBlank(durationSpec)) {
            try {
                cacheDuration = Integer.parseInt(durationSpec.trim());
            }
            catch (Exception e) {
                if (failSafe) {
                    log.warn(
                        "Invalid cache duration: {}. Using default value: {}",
                        durationSpec, Integer.valueOf(cacheDuration));
                }
                else {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        // Else: Empty spec. => Ignore and use default.

        return cacheDuration;
    }

    private static BusinessDay parseBusinessDay(String businessDaySpec,
                                                boolean failSafe) {
        BusinessDay businessDay = DEFAULT_BUSINESS_DAY;
        if (! isBlank(businessDaySpec)) {
            if ("-".equals(businessDaySpec.trim())) {
                // No business day hours specified. => No closing hours!
                businessDay = new BusinessDay(0, 2400);
            }
            else {
                // Parse business hours.
                String[] v = businessDaySpec.split("\\s*-\\s*", -1);
                try {
                    businessDay = new BusinessDay(parseHour(v[0]),
                                                  parseHour(v[1]));
                }
                catch (Exception e) {
                    if (failSafe) {
                        log.warn("Invalid business day hours: {}. "
                                 + "Using default value: {}", businessDaySpec,
                                 "" + businessDay.getOpeningTime() + '-'
                                    + businessDay.getClosingTime());
                    }
                    else {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        }
        // Else: Empty spec. => Ignore and use default.

        return businessDay;
    }

    private static int parseHour(String spec) {
        if (spec.indexOf(':') == -1) {
            if (spec.length() <= 2) {
                // Append minutes.
                spec += "00";
            }
        }
        else {
            spec = spec.replace(":", "");
        }
        int hour = Integer.parseInt(spec);
        if ((hour < 0) || (hour > 2400)) {
            throw new IllegalArgumentException(BUSINESS_DAY_PROPERTY);
        }
        return hour;
    }


    //-------------------------------------------------------------------------
    // BusinessDay nested class
    //-------------------------------------------------------------------------

    private final static class BusinessDay
    {
        private final int open;
        private final int close;
        private final boolean inverse;

        public BusinessDay(int businessOpening, int businessClosing) {
            int start = this.checkTime(businessOpening, "businessOpening");
            int end   = this.checkTime(businessClosing, "businessClosing");
            this.inverse = (start > end);
            this.open    = Math.min(start, end);
            this.close   = Math.max(start, end);
        }

        public boolean isInBusinessDay(Date time) {
            GregorianCalendar cal = new GregorianCalendar();
            if (time != null) {
                cal.setTime(time);
            }
            // Compute current time as an integer.
            int h = (cal.get(HOUR_OF_DAY) * 100) + cal.get(MINUTE);
            // Compare time with business day opening and closing
            // hours. Specified time must be within the business day except
            // when this latter is a business night (e.g. from 22:00 to
            // 06:00) in which case the XOR does the job.
            return (((h >= this.open) && (h <= this.close)) ^ this.inverse);
        }

        public Date getNextBusinessDay() {
            GregorianCalendar cal = new GregorianCalendar();
            int h = (cal.get(HOUR_OF_DAY) * 100) + cal.get(MINUTE);
            if ((h >= this.close) && (this.close > this.open)) {
                // Next day.
                cal.add(DAY_OF_YEAR, 1);
            }
            cal.set(HOUR_OF_DAY, this.open / 100);
            cal.set(MINUTE,      this.open % 100);
            return cal.getTime();
        }

        public String getOpeningTime() {
            return String.format("%02d:%02d", Integer.valueOf(this.open / 100),
                                              Integer.valueOf(this.open % 100));
        }

        public String getClosingTime() {
            return String.format("%02d:%02d", Integer.valueOf(this.close / 100),
                                              Integer.valueOf(this.close % 100));
        }

        private int checkTime(int spec, String name) {
            int h = spec / 100;
            int m = spec % 100;
            if ((h < 0) || (h > 24) || (m > 59)) {
                throw new IllegalArgumentException(name);
            }
            return spec;
        }
    }
}
