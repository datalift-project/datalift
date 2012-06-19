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

package org.datalift.projectmanager;


import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.datalift.fwk.i18n.PreferredLocales;


/**
 * Project license.
 *
 * @author lbihanic
 */
public class License
{
    /** The license identifier, as a URI. */
    public final URI uri;
    /** The license labels. */
    private final Map<String,String> labels = new HashMap<String,String>();

    /**
     * Default constructor.
     * @param  uri   the license URI.  
     */
    public License(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }
        this.uri = uri;
    }

    /**
     * Returns the license identifier, as a URI.
     * @return the license URI.
     */
    public URI getUri() {
        return this.uri;
    }

    /**
     * Returns the license label for the user's
     * {@link PreferredLocales preferred locale}.
     * @return the license label in the user's preferred locale if
     *         defined, the default license otherwise or, if no default
     *         label has been defined, the license URI.
     */
    public String getLabel() {
        String label = null;
        for (Locale l : PreferredLocales.get()) {
            label = this.getLabel(l.toString(), false);
            if (label != null) break;
        }
        if (label == null) {
            label = this.uri.toString();
        }
        return label;
    }

    /**
     * Returns the license label for the specified locale.
     * @param  locale   a locale.
     *
     * @return the license label for the locale country or language if
     *         defined, the default license otherwise or, if no default
     *         label has been defined, the license URI.
     */
    public String getLabel(Locale locale) {
        return this.getLabel(locale.toString(), true);
    }

    /**
     * Returns the license label for the specified language or
     * language and country.
     * @param  language   the language code or language and country
     *                    codes, separated with an hyphen ('-') or an
     *                    underscore ('_') character.
     *
     * @return the license label for the specified language if defined,
     *         the default license otherwise or, if no default label has
     *         been defined, the license URI.
     */
    public String getLabel(String language) {
        return this.getLabel(language, true);
    }

    private String getLabel(String language, boolean acceptUri) {
        String v = null;
        String[] elts = language.split("-|_");
        if ((elts.length > 1) && (elts[1].length() != 0)) {
            v = this.labels.get(elts[0] + '_' + elts[1].toUpperCase());
        }
        if (v == null) {
            v = this.labels.get(elts[0]);
        }
        if (v == null) {
            v = this.labels.get("");
        }
        if ((v == null) && (acceptUri)) {
            v = this.uri.toString();
        }
        return v;
    }

    /**
     * Sets the license label for the specified language or language
     * and country.
     * @param language
     * @param label
     */
    /* package */  void setLabel(String language, String label) {
        if (language == null) {
            language = "";
        }
        String[] elts = language.split("-|_");
        this.labels.put(elts[0], label);
        if ((elts.length > 1) && (elts[1].length() != 0)) {
            this.labels.put(elts[0] + '_' + elts[1].toUpperCase(), label);
        }
    }
}
