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


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.datalift.fwk.util.StringUtils.isBlank;


public abstract class BaseLocalizedItem implements LocalizedItem
{
    /** The item labels. */
    private final Map<String,String> labels = new HashMap<String,String>();

    /**
     * Creates a new LocalizedItem.
     * @param  labels   the item labels for the supported languages.
     */
    public BaseLocalizedItem(Map<String,String> labels) {
        if (labels != null) {
            for (Map.Entry<String,String> e : labels.entrySet()) {
                String label = e.getValue();
                if (! isBlank(label)) {
                    this.setLabel(e.getKey(), label);
                }
                // Else: ignore...
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        String label = null;
        for (Locale l : PreferredLocales.get()) {
            label = this.getLabel(l.toString(), false);
            if (label != null) break;
        }
        if (label == null) {
            label = this.toString();
        }
        return label;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel(Locale locale) {
        return this.getLabel(locale.toString(), true);
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel(String language) {
        return this.getLabel(language, true);
    }

    /**
     * Returns the item label for the specified language or
     * language and country.
     * @param  language     the language code or language and country
     *                      codes, separated with an hyphen ('-') or an
     *                      underscore ('_') character.
     * @param  acceptDesc   whether the string representation of this
     *                      object shall be returned if no label is
     *                      defined for the specified language.
     *
     * @return the item label for the specified language if defined,
     *         the default label otherwise or, if no default label has
     *         been defined, the
     *         {@link #toString() string representation} of this object
     *         or <code>null</code>, depending on
     *         <code>acceptDesc</code>.
     */
    private String getLabel(String language, boolean acceptDesc) {
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
        if ((v == null) && (acceptDesc)) {
            v = this.toString();
        }
        return v;
    }

    /**
     * Sets the item label for the specified language or language
     * and country.
     * @param  language   the language code or language and country
     *                    codes, separated with an hyphen ('-') or an
     *                    underscore ('_') character.
     * @param  label      the label for the specified language or
     *                    language and country.
     */
    private void setLabel(String language, String label) {
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
