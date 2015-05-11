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


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;


/**
 * An entry in a {@link Menu GUI menu}.
 *
 * @author lbihanic
 */
abstract public class MenuEntry implements Comparable<MenuEntry>
{
    // ------------------------------------------------------------------------
    // HttpMethod enum definition
    // ------------------------------------------------------------------------

    /**
     * The HTTP methods supported for accessing the pages.
     * <p>
     * As the data-lifting process is usually user-driven, modules
     * should avoid using HTTP methods other than {@link #GET} or
     * {@link #POST} as only these two are supported by web
     * browsers.</p>
     */
    public enum HttpMethod { GET, POST, PUT, DELETE; }

    // ------------------------------------------------------------------------
    // MenuEntry contract definition
    // ------------------------------------------------------------------------

    /**
     * Returns the page (relative) URI.
     * @return the page URI.
     */
    abstract public URI getUri();

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
    abstract public HttpMethod getMethod();

    /**
     * Returns the description of the page to display to the user.
     * @return the description of the page.
     */
    abstract public String getLabel();

    /**
     * Returns the position at which the entry shall appear in the menu.
     * @return the preferred entry position.
     */
    abstract public int getPosition();

    /**
     * <i>Reserved for future use</i>.
     * @return the link icon or <code>null</code>.
     */
    abstract public URI getIcon();

    /**
     * <i>Reserved for future use</i>.
     * @param  baseUri   the application base URI.
     *
     * @return the absolute URL of the link icon file.
     * @throws MalformedURLException if an error occurred building
     *         the icon file URL.
     */
    public String getIcon(String baseUri) throws MalformedURLException {
        return this.toUrl(baseUri, this.getIcon());
    }

    // ------------------------------------------------------------------------
    // Comparable contract support
    // ------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public int compareTo(MenuEntry o) {
        int v = this.getPosition() - o.getPosition();
        // Use module label to disambiguate modules
        // requesting the same position in list.
        return (v != 0)? v: this.getLabel().compareToIgnoreCase(o.getLabel());
    }

    // ------------------------------------------------------------------------
    // Object contract support
    // ------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getLabel() + " -> " + this.getMethod() + ' ' + this.getUri();
    }

    // ------------------------------------------------------------------------
    // Specific implementation
    // ------------------------------------------------------------------------

    /**
     * Build a URL string.
     * @param baseUri   the application base URI (context).
     * @param uri       the page URI.
     * @return the stringified URL, built by concatenating the page URI
     *         to the base URI.
     * @throws MalformedURLException if the constructed URI is not a
     *         valid URL.
     */
    protected final String toUrl(String baseUri, URI uri)
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
                url = uri.toString();
            }
        }
        return url;
    }
}
