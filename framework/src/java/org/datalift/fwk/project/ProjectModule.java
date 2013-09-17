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

package org.datalift.fwk.project;


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.datalift.fwk.Module;


/**
 * A project module is a specific type of module that participates in
 * the data lifting process by providing an interface allowing the user
 * to interact with the data transformation process.
 *
 * @author lbihanic
 */
public interface ProjectModule extends Module
{
    /**
     * Returns whether this module applies to the specified project,
     * i.e. the project current state allows data manipulation (e.g.
     * transformation, conversion, data completion...) by this module.
     * @param  p   a data-lifting project.
     *
     * @return the description of the module entry page applicable to
     *         the specified project or <code>null</code> if this
     *         module can not handle the project in its current state.
     */
    public abstract UriDesc canHandle(Project p);

    /**
     * The HTTP methods supported for accessing the module entry pages.
     * <p>
     * As the data-lifting process is usually user-driven, modules
     * should avoid using HTTP methods other than {@link #GET} or
     * {@link #POST} as only these two are supported by web
     * browsers.</p>
     */
    public enum HttpMethod { GET, POST, PUT, DELETE; }

    //-------------------------------------------------------------------------
    // UriDesc nested class
    //-------------------------------------------------------------------------

    /**
     * A description of the access to a module page.
     */
    public class UriDesc
    {
        private final URI uri;
        private final HttpMethod method;
        private final String label;
        private URI icon;
        private int position = 5000;

        /**
         * Creates the description of a URI accessible using the HTTP
         * {@link HttpMethod#GET} method.
         * @param  uri     the page (relative) URI as a string.
         * @param  label   the page description to display to the user.
         *
         * @throws URISyntaxException if the specified URI is not valid.
         *
         * @see    #UriDesc(URI, String)
         */
        public UriDesc(String uri, String label) throws URISyntaxException {
            this(new URI(uri), HttpMethod.GET, label);
        }

        /**
         * Creates the description of a URI accessible using the HTTP
         * {@link HttpMethod#GET} method.
         * @param  uri     the page (relative) URI.
         * @param  label   the page description to display to the user.
         *
         * @see    #UriDesc(URI, HttpMethod, String)
         */
        public UriDesc(URI uri, String label) {
            this(uri, HttpMethod.GET, label);
        }

        /**
         * Creates the description of a URI accessible using the
         * specified HTTP method.
         * @param  uri      the page (relative) URI as a string.
         * @param  method   the HTTP method to access the URI.
         * @param  label    the page description to display to the user.
         *
         * @throws URISyntaxException if the specified URI is not valid.
         *
         * @see    #UriDesc(URI, HttpMethod, String)
         */
        public UriDesc(String uri, HttpMethod method, String label)
                                                    throws URISyntaxException {
            this(new URI(uri), method, label);
        }

        /**
         * Creates the description of a URI accessible using the
         * specified HTTP method.
         * @param  uri      the page (relative) URI as a string.
         * @param  method   the HTTP method to access the URI.
         * @param  label    the page description to display to the user.
         */
        public UriDesc(URI uri, HttpMethod method, String label) {
            if (uri == null) {
                throw new IllegalArgumentException("uri");
            }
            if (method == null) {
                throw new IllegalArgumentException("method");
            }
            if ((label == null) || (label.length() == 0)) {
                throw new IllegalArgumentException("label");
            }
            this.uri = uri;
            this.method = method;
            this.label = label;
        }

        /**
         * Returns the page (relative) URI.
         * @return the page URI.
         */
        public URI getUri() {
            return this.uri;
        }

        /**
         * Returns the page URL, resolved using the specified
         * (absolute) base URI.
         * @param  baseUri   the base URI to resolve the page URI, if
         *                   relative.
         *
         * @return the page URL.
         * @throws MalformedURLException if no valid URL can be built
         *         from the page URI of if the base URI is invalid.
         */
        public String getUrl(String baseUri) throws MalformedURLException {
            return this.toUrl(baseUri, this.getUri());
        }

        /**
         * Returns the HTTP method to access the page.
         * @return the HTTP method.
         */
        public HttpMethod getMethod() {
            return this.method;
        }

        /**
         * Returns the description of the page to display to the user.
         * @return the description of the page.
         */
        public String getLabel() {
            return this.label;
        }

        /**
         * Sets the position where the module page shall appear in
         * the list of modules applicable to a project.
         * <p>
         * There's no hard-coded rule for the position value. Yet the
         * following informal rules are recommended:</p>
         * <ul>
         *  <li>From 0 to 999 for modules transforming non RDF data
         *   into RDF data</li>
         *  <li>From 1000 to 9999 for modules transforming RDF data
         *   (ontology alignment, interlinking...)</li>
         *  <li>Above 10000 for modules publishing data</li>
         * </ul>
         * @param  position   the position as a positive integer.
         */
        public void setPosition(int position) {
            if (position < 0) {
                throw new IllegalArgumentException("position ("
                                    + position + ") shall not be negative");
            }
            this.position = position;
        }

        /**
         * Returns the position at which the module page shall appear
         * in the list of modules applicable to a project.
         * <p>
         * Default value is 5000.</p>
         * @return the module page position.
         */
        public int getPosition() {
            return this.position;
        }

        /**
         * <i>Reserved for future use</i>.
         * @param  icon   the module icon or <code>null</code>.
         */
        public void setIcon(URI icon) {
            this.icon = icon;
        }

        /**
         * <i>Reserved for future use</i>.
         * @return the module icon or <code>null</code>.
         */
        public URI getIcon() {
            return this.icon;
        }

        /**
         * <i>Reserved for future use</i>.
         * @param  baseUri   the application base URI.
         *
         * @return the absolute URL of the module icon file.
         * @throws MalformedURLException if an error occurred building
         *         the icon file URL.
         */
        public String getIcon(String baseUri) throws MalformedURLException {
            return this.toUrl(baseUri, this.getIcon());
        }

        //---------------------------------------------------------------------
        // Object contract support
        //---------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return this.label + " -> " + this.method + ' ' + this.uri;
        }

        //---------------------------------------------------------------------
        // Specific implementation
        //---------------------------------------------------------------------

        private String toUrl(String baseUri, URI uri)
                                                throws MalformedURLException {
            String url = null;
            if (uri != null) {
                if (baseUri != null) {
                    if (! baseUri.endsWith("/")) {
                        baseUri += '/';
                    }
                    url = new URL(new URL(baseUri), uri.toString()).toString();
                }
                else {
                    url = this.getUri().toString();
                }
            }
            return url;
        }
    }
}
