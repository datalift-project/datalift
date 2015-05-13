/*
 * Copyright / Copr. 2010-2015 Atos - Public Sector France -
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


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.util.web.MenuEntry.HttpMethod;


/**
 * Datalift GUI main menu.
 *
 * @author lbihanic
 */
public class MainMenu extends Menu
{
    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * The default name for resource bundles: "<code>resources</code>",
     * which corresponds to:
     * <code>&lt;module-name&gt;/resources_&lt;locale&gt;.properties</code>.
     * For example: <code>sparql/resources_fr.properties</code>
     */
    public final static String DEFAULT_BUNDLE_NAME = "resources";

    // ------------------------------------------------------------------------
    // Class members
    // ------------------------------------------------------------------------

    /** Singleton instance. */
    private final static MainMenu instance = new MainMenu();
    /** Whether the main menu has been registered in Datalift configuration. */
    private static boolean registered = false;

    private final static Logger log = Logger.getLogger();

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor, private on purpose.
     */
    private MainMenu() {
        super();
    }

    // ------------------------------------------------------------------------
    // Collection contract support
    // ------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Iterator<MenuEntry> iterator() {
        final Iterator<MenuEntry> i = super.iterator();
        final MenuEntry first = (i.hasNext())? i.next(): null;

        return new Iterator<MenuEntry>() {
            private MenuEntry next = first;

            @Override
            public boolean hasNext() {
                return (this.next != null);
            }

            @Override
            public MenuEntry next() {
                MenuEntry e = this.next;
                if (e == null) {
                    throw new NoSuchElementException();
                }
                this.next = null;
                while (i.hasNext()) {
                    MenuEntry n = i.next();
                    if (n.isAccessible()) {
                        this.next = n;
                        break;
                    }
                }
                return e;
            }

            @Override
            public void remove() {
                i.remove();
            }
        };
    }

    // ------------------------------------------------------------------------
    // Static utility methods
    // ------------------------------------------------------------------------

    /**
     * Retrieves the current main menu instance.
     * @return the current main menu instance.
     */
    public static MainMenu get() {
        if (! registered) {
            // Register the main menu in Datalift configuration.
            try {
                Configuration.getDefault().registerBean(instance);
                registered = true;
            }
            catch (Exception e) {
                log.warn("Failed to register MainMenu instance " +
                         "to Datalift configuration", e);
            }
        }
        return instance;
    }


    // ------------------------------------------------------------------------
    // EntryDesc nested class
    // ------------------------------------------------------------------------

    /**
     * A {@link MenuEntry menu entry} with fixed URL, method and
     * position but a label the translation of which is looked up
     * upon every access.
     */
    public static class EntryDesc extends MenuEntry
    {
        // --------------------------------------------------------------------
        // Instance members
        // --------------------------------------------------------------------

        private final URI uri;
        private final HttpMethod method;
        private final String label;
        private final String bundleName;
        private final Class<?> owner;
        private final int position;
        private final Collection<String> roles;
        private boolean bundleErrorLogged = false;

        // --------------------------------------------------------------------
        // Constructors
        // --------------------------------------------------------------------

        /**
         * Creates a new translatable menu entry accessible using the
         * HTTP {@link HttpMethod#GET} method.
         * @param uri          the target page URI.
         * @param label        the link or button label or key in label
         *                     translation resource bundle.
         * @param bundleName   the I18N resource bundle name to look
         *                     label translations up.
         * @param owner        the entry owner: the owning class allows
         *                     retrieving the classloader capable of
         *                     loading the resource bundle.
         * @param position     the entry position in the menu.
         * @param roles        the security roles required to access the
         *                     menu entry, <code>null</code> if entry is
         *                     public or an empty list if authentication
         *                     is mandated but no specific role is
         *                     required.
         *
         * @throws IllegalArgumentException if <code>label</code> is
         *         <code>null</code> or if <code>position</code> is
         *         negative.
         * @throws NullPointerException if <code>owner</code> or
         *         <code>uri</code> is <code>null</code>.
         */
        public EntryDesc(String uri, String label, String bundleName,
                         Object owner, int position, Collection<String> roles) {
            this(uri, null, label, bundleName, owner, position, roles);
        }

        /**
         * Creates a new translatable menu entry.
         * @param uri          the target page URI.
         * @param method       the HTTP method to access the URI.
         * @param label        the link or button label or key in label
         *                     translation resource bundle.
         * @param bundleName   the I18N resource bundle name to look
         *                     label translations up.
         * @param owner        the entry owner: the owning class allows
         *                     retrieving the classloader capable of
         *                     loading the resource bundle.
         * @param position     the entry position in the menu.
         * @param roles        the security roles required to access the
         *                     menu entry, <code>null</code> if entry is
         *                     public or an empty list if authentication
         *                     is mandated but no specific role is
         *                     required.
         *
         * @throws IllegalArgumentException if <code>label</code> is
         *         <code>null</code> or if <code>position</code> is
         *         negative.
         * @throws NullPointerException if <code>owner</code> or
         *         <code>uri</code> is <code>null</code>.
         */
        public EntryDesc(String uri, HttpMethod method, String label,
                         String bundleName, Object owner, int position,
                         Collection<String> roles) {
            this(URI.create(uri), method, label, bundleName, owner.getClass(),
                 position, roles);
        }

        /**
         * Creates a new translatable menu entry.
         * @param uri          the target page URI.
         * @param method       the HTTP method to access the URI.
         * @param label        the link or button label or key in label
         *                     translation resource bundle.
         * @param bundleName   the I18N resource bundle name to look
         *                     label translations up.
         * @param owner        the entry owning class. It allows
         *                     retrieving the classloader capable of
         *                     loading the resource bundle.
         * @param position     the entry position in the menu.
         * @param roles        the security roles required to access the
         *                     menu entry, <code>null</code> if entry is
         *                     public or an empty list if authentication
         *                     is mandated but no specific role is
         *                     required.
         *
         * @throws IllegalArgumentException if <code>label</code> or
         *         <code>uri</code> is <code>null</code> or if
         *         <code>position</code> is negative.
         */
        public EntryDesc(URI uri, HttpMethod method, String label,
                         String bundleName, Class<?> owner, int position,
                         Collection<String> roles) {
            super();
            if (uri == null) {
                throw new IllegalArgumentException("uri");
            }
            if ((label == null) || (label.length() == 0)) {
                throw new IllegalArgumentException("label");
            }
            if (position < 0) {
                throw new IllegalArgumentException("position ("
                                    + position + ") shall not be negative");
            }
            this.uri = uri;
            this.method = (method != null)? method: HttpMethod.GET;
            this.label = label;
            this.bundleName = bundleName;
            this.owner = (owner != null)? owner: this.getClass();
            this.position = position;
            this.roles = (roles == null)? null:
                            Collections.unmodifiableCollection(
                                                new ArrayList<String>(roles));
        }

        // --------------------------------------------------------------------
        // MenuEntry contract support
        // --------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        public URI getUri() {
            return this.uri;
        }

        /** {@inheritDoc} */
        @Override
        public HttpMethod getMethod() {
            return this.method;
        }

        /** {@inheritDoc} */
        @Override
        public String getLabel() {
            String l = this.label;
            // Lookup label translation, if a resource bundle name is available.
            if (this.bundleName != null) {
                try {
                    l = PreferredLocales.get()
                                        .getBundle(this.bundleName, this.owner)
                                        .getString(this.label);
                }
                catch (MissingResourceException e) {
                    // Log bundle lookup error only once.
                    if (! this.bundleErrorLogged) {
                        log.warn("Resource bundle {} not found", e,
                                 this.bundleName);
                        this.bundleErrorLogged = true;
                    }
                    // And return unresolved label name.
                }
            }
            return l;
        }

        /** {@inheritDoc} */
        @Override
        public int getPosition() {
            return this.position;
        }

        /**
         * {@inheritDoc}
         * @return <code>null</code> always.
         */
        @Override
        public URI getIcon() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isAccessible() {
            // No role list provided = entry is public.
            boolean accessible = (this.roles == null);
            if (! accessible) {
                if (roles.isEmpty()) {
                    // Empty role list => Just check user has logged in.
                    accessible = (SecurityContext.getUserPrincipal() != null);
                }
                else {
                    // Check that at least one of the user roles matches
                    // one of the required roles.
                    SecurityContext ctx = SecurityContext.getContext();
                    for (String role : this.roles) {
                        if (ctx.hasRole(role)) {
                            accessible = true;
                            break;
                        }
                    }
                }
            }
            return accessible;
        }
    }
}
