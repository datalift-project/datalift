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


import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.datalift.fwk.util.StringUtils.isBlank;


public abstract class BaseLocalizedItem implements LocalizedItem
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The item labels. */
    private final ConcurrentMap<String,Map<String,String>> labels =
                            new ConcurrentHashMap<String,Map<String,String>>();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new LocalizedItem.
     */
    protected BaseLocalizedItem() {
        super();
    }

    /**
     * Creates a new LocalizedItem.
     * @param  labels   the item labels for the supported languages.
     */
    protected BaseLocalizedItem(Map<String,String> labels) {
        super();
        this.setLabels(null, labels);
    }

    //-------------------------------------------------------------------------
    // LocalizedItem contract support
    //-------------------------------------------------------------------------

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
        if (locale == null) {
            throw new IllegalArgumentException("locale");
        }
        return this.getLabel(locale.toString(), false);
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel(String language) {
        return this.getLabel(language, false);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    protected String getTypeLabel(String type) {
        String label = null;
        for (Locale l : PreferredLocales.get()) {
            label = this.getLabel(type, l.toString(), false);
            if (label != null) break;
        }
        return label;
    }

    protected String getTypeLabel(String type, Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("locale");
        }
        return this.getLabel(type, locale.toString(), false);
    }

    protected String getTypeLabel(String type, String language) {
        return this.getLabel(type, language, false);
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
    protected String getLabel(String language, boolean acceptDesc) {
        return this.getLabel(null, language, acceptDesc);
    }

    /**
     * Returns the label of the specified type for the specified
     * language or language and country.
     * @param  type         the label type, <code>null</code> for the
     *                      {@link #getLabel() default label}.
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
    protected String getLabel(String type, String language,
                                           boolean acceptDesc) {
        if (language == null) {
            throw new IllegalArgumentException("language");
        }
        Map<String,String> typeLabels = this.getLabels(type);

        String v = null;
        String[] elts = language.split("-|_");
        if ((elts.length > 1) && (elts[1].length() != 0)) {
            v = typeLabels.get(elts[0] + '_' + elts[1].toUpperCase());
        }
        if (v == null) {
            v = typeLabels.get(elts[0]);
        }
        if (v == null) {
            v = typeLabels.get("");
        }
        if ((v == null) && (acceptDesc)) {
            v = this.toString();
        }
        return v;
    }

    protected void setLabels(Map<String,String> labels) {
        this.setLabels(null, labels);
    }

    protected void setLabels(String type, Map<String,String> labels) {
        Map<String,String> typeLabels = this.getLabels(type);
        typeLabels.clear();
        if (labels != null) {
            for (Map.Entry<String,String> e : labels.entrySet()) {
                String label = e.getValue();
                if (! isBlank(label)) {
                    this.setLabel(type, e.getKey(), label);
                }
                // Else: ignore...
            }
        }
    }

    /**
     * Sets the label of the specified type for the specified
     * language or language and country.
     * @param  type       the label type, <code>null</code> for the
     *                    {@link #getLabel() default label}.
     * @param  language   the language code or language and country
     *                    codes, separated with an hyphen ('-') or an
     *                    underscore ('_') character.
     * @param  label      the label for the specified language or
     *                    language and country.
     */
    private void setLabel(String type, String language, String label) {
        if (language == null) {
            language = "";
        }
        Map<String,String> typedLabels = this.getLabels(type);
        String[] elts = language.split("-|_");
        typedLabels.put(elts[0], label);
        if ((elts.length > 1) && (elts[1].length() != 0)) {
            typedLabels.put(elts[0] + '_' + elts[1].toUpperCase(), label);
        }
    }

    private Map<String,String> getLabels(String type) {
        if (type == null) {
            type = "";
        }
        Map<String,String> typeLabels = this.labels.get(type);
        if (typeLabels == null) {
            typeLabels = new ConcurrentHashMap<String,String>();
            if (this.labels.putIfAbsent(type, typeLabels) != null) {
                // Concurrent addition of new label type.
                typeLabels = this.labels.get(type);
            }
        }
        return typeLabels;
    }
}
