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


import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.RandomAccess;
import java.util.ResourceBundle;

import javax.ws.rs.core.Context;

import org.datalift.fwk.log.Logger;

import static org.datalift.fwk.util.StringUtils.isSet;


/**
 * The user's preferred locales, extracted from the HTTP request
 * "<code>Accept-Language</code>" header, as an
 * <strong>unmodifiable</strong> list.
 * <p>
 * Preferred locales are accessible:</p>
 * <ul>
 *  <li>Directly, using {@link #get() PreferredLocales.get()}, or</li>
 *  <li>Through injection, using the {@link Context @Context} JAX-RS
 *   annotation.</li>
 * </ul>
 *
 * @author lbihanic
 */
public final class PreferredLocales extends AbstractList<Locale>
                                    implements RandomAccess
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** The user's preferred locales for the current HTTP request. */
    private final static ThreadLocal<PreferredLocales> current =
                                        new ThreadLocal<PreferredLocales>();
    /** The JVM default locales. */
    private final static PreferredLocales defaultLocales;

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final List<Locale> locales;

    //-------------------------------------------------------------------------
    // Class initializer
    //-------------------------------------------------------------------------

    static {
        Collection<Locale> locales = new ArrayList<Locale>();
        // Build default locales from JVM default locale.
        addLocale(Locale.getDefault(), locales);
        // Add English default locales.
        addLocale(Locale.US, locales);
        // Add empty locale for accessing default bundle (no locale suffix).
        locales.add(Locale.ROOT);
        defaultLocales = new PreferredLocales(locales);
    }

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new PreferredLocales object for the specified list of
     * locales.
     * @param  c   the preferred locales, the most preferred first.
     */
    public PreferredLocales(Collection<? extends Locale> c) {
        super();
        if ((c == null) || (c.isEmpty())) {
            throw new IllegalArgumentException("c");
        }
        this.locales = new ArrayList<Locale>(c.size());
        this.locales.addAll(c);
    }

    //-------------------------------------------------------------------------
    // List contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Locale get(int index) {
        return this.locales.get(index);
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return this.locales.size();
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Gets a resource bundle using the specified base name, the user's
     * preferred locales, and the owner's class loader. Calling this
     * method is equivalent to calling
     * <blockquote>
     * <code>getBundle(baseName, owner.getClass())</code>
     * </blockquote>.
     * @param  baseName   the base name of the resource bundle or a
     *                    fully qualified class name.
     * @param  owner      the object on behalf of which loading the
     *                    resource bundle.
     *
     * @return a resource bundle for the given base name and the owner
     *         object.
     * @throws MissingResourceException if no resource bundle for the
     *         specified base name can be found.
     */
    public ResourceBundle getBundle(String baseName, Object owner) {
        return this.getBundle(baseName, owner.getClass());
    }

    /**
     * Gets a resource bundle using the specified base name, the user's
     * preferred locales, and the owner's class loader.
     * @param  baseName   the base name of the resource bundle or a
     *                    fully qualified class name.
     * @param  owner      the class on behalf of which loading the
     *                    resource bundle.
     *
     * @return a resource bundle for the given base name and the owner
     *         class.
     * @throws MissingResourceException if no resource bundle for the
     *         specified base name can be found.
     */
    public ResourceBundle getBundle(String baseName, Class<?> owner) {
        try {
            return ResourceBundle.getBundle(baseName, this.locales.get(0),
                                            owner.getClassLoader(),
                                            this.getBundleControl());
        }
        catch (MissingResourceException e) {
            log.fatal("Failed to resolved bundle \"{}\" for locales {}",
                                                        baseName, this.locales);
            throw e;
        }
    }

    /**
     * Returns a <code>ResourceBundle.Control</code> object capable of
     * loading the bundles corresponding to all the user's preferred
     * locales (instead of only loading the bundles corresponding to
     * the user's specified locale and the VM default locale).
     * @return a <code>ResourceBundle.Control</code>.
     */
    public ResourceBundle.Control getBundleControl() {
        return new ResourceBundle.Control() {
                @Override
                public List<Locale> getCandidateLocales(String baseName,
                                                        Locale locale) {
                    return locales;
                }

                @Override
                public Locale getFallbackLocale(String baseName,
                                                Locale locale) {
                    return (Locale.ENGLISH.equals(locale))? null: Locale.ENGLISH;
                }
            };
    }

    //-------------------------------------------------------------------------
    // Static methods
    //-------------------------------------------------------------------------

    /**
     * Returns the user's preferred locales for the being-processed
     * HTTP request (computed from the <code>Accept-Language</code>
     * header). If no request is being processed, the locales are read
     * from the {@link Locale#getDefault() JVM runtime environment}.
     * @return the user's preferred locales.
     */
    public static PreferredLocales get() {
        return get(true);
    }

    /**
     * Returns the user's preferred locales for the being-processed
     * HTTP request (computed from the <code>Accept-Language</code>
     * header). If no request is being processed and <code>create</code>
     * is set to <code>true</code>, the locales are read
     * from the {@link Locale#getDefault() JVM runtime environment}.
     * @param  create   whether to create default locales if none
     *                  were installed.
     *
     * @return the user's preferred locales.
     */
    public static PreferredLocales get(boolean create) {
        PreferredLocales l = current.get();
        if ((l == null) && (create)) {
            l = set(null);
        }
        return l;
    }

    /**
     * Sets the user's preferred locales for the being-processed
     * request to the specified locales, overriding those computed
     * from the HTTP request headers.
     * @param  locales   the user's preferred locales for the
     *                   request.
     *
     * @return the {@link PreferredLocales} object wrapping the
     *         specified locales.
     */
    public static PreferredLocales set(List<Locale> locales) {
        PreferredLocales prefs = defaultLocales;

        if ((locales != null) && (! locales.isEmpty())) {
            // Add the specified locales. A LinkedHashSet is used to both
            // respect ordering and a avoid duplicate locales.
            Collection<Locale> tmpLocales = new LinkedHashSet<Locale>(locales);
            // Add missing generic locales (without variant and country).
            for (Locale l : locales) {
                addLocale(l, tmpLocales);
            }
            // Add empty locale for accessing default bundle (no locale suffix).
            tmpLocales.add(Locale.ROOT);
            prefs = new PreferredLocales(tmpLocales);
        }
        current.set(prefs);
        log.trace("Preferred locales set to: {}", prefs);
        return prefs;
    }

    /**
     * Resets the user's preferred locales. Until the next HTTP request
     * arrives, the locales from the
     * {@link Locale#getDefault() JVM runtime environment} apply.
     */
    public static void reset() {
        log.trace("Preferred locales removed from current thread");
        current.set(null);
    }

    private static void addLocale(Locale l, Collection<Locale> locales) {
        // Add the specified locale.
        locales.add(l);
        // If a variant is present, add a locale without it.
        String language = l.getLanguage();
        String country  = l.getCountry();
        if (isSet(l.getVariant())) {
            locales.add(new Locale(language, country));
        }
        // If a country is present, add a locale without it.
        if (isSet(country)) {
            locales.add(new Locale(language));
        }
    }
}
