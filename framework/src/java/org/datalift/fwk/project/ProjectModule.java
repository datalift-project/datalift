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


public interface ProjectModule extends Module
{
    public abstract UriDesc canHandle(Project p);

    public enum HttpMethod { GET, POST, PUT, DELETE; }

    public final class UriDesc
    {
        private final URI uri;
        private final HttpMethod method;
        private final String label;
        private URI icon;

        public UriDesc(String uri, String label) throws URISyntaxException {
            this(new URI(uri), HttpMethod.GET, label);
        }

        public UriDesc(URI uri, String label) {
            this(uri, HttpMethod.GET, label);
        }

        public UriDesc(String uri, HttpMethod method, String label)
                                                    throws URISyntaxException {
            this(new URI(uri), method, label);
        }

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

        public URI getUri() {
            return this.uri;
        }

        public String getUrl(String baseUri) throws MalformedURLException {
            return this.toUrl(baseUri, this.getUri());
        }

        public HttpMethod getMethod() {
            return this.method;
        }

        public String getLabel() {
            return this.label;
        }

        public void setIcon(URI icon) {
            this.icon = icon;
        }
        public URI getIcon() {
            return this.icon;
        }
        public String getIcon(String baseUri) throws MalformedURLException {
            return this.toUrl(baseUri, this.getIcon());
        }

        private String toUrl(String baseUri, URI uri)
                                                throws MalformedURLException {
            String url = null;
            if (baseUri != null) {
                if (! baseUri.endsWith("/")) {
                    baseUri += '/';
                }
                url = new URL(new URL(baseUri), uri.toString()).toString();
            }
            else {
                url = this.getUri().toString();
            }
            return url;
        }
    }
}
