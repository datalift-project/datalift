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

package org.datalift.core.velocity.jersey;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.jersey.api.view.Viewable;

import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;


/**
 * A default implementation of the {@link ViewFactory} interface that
 * returns Jersey-compatible {@link TemplateModel}s, suitable for being
 * processed by the
 * {@link VelocityTemplateProcessor Velocity template processor}.
 *
 * @author lbihanic
 */
public class VelocityViewFactory extends ViewFactory
{
    //-------------------------------------------------------------------------
    // ViewFactory contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected TemplateModel createView(String templateName, Object model) {
        return new VelocityTemplateModel(templateName, model);
    }

    //-------------------------------------------------------------------------
    // VelocityTemplateModel nested class
    //-------------------------------------------------------------------------

    /**
     * A {@link TemplateModel} wrapper for Jersey's {@Link Viewable}
     * object.
     */
    private final static class VelocityTemplateModel extends Viewable
                                                     implements TemplateModel
    {
        //---------------------------------------------------------------------
        // Instance members
        //---------------------------------------------------------------------

        private final Map<String,Object> model;
        private final Set<Class<?>> fieldClasses = new HashSet<Class<?>>();

        //---------------------------------------------------------------------
        // Constructors
        //---------------------------------------------------------------------

        /**
         * Creates a new template view object.
         * @param  templateName   the template name, absolute or
         *                        relative.
         * @param  model          the (optional) model object.
         */
        @SuppressWarnings("unchecked")
        public VelocityTemplateModel(String templateName, Object model) {
            // Always use a map as back-end model object.
            super(templateName, new HashMap<String,Object>());
            // Get a type-safe reference to the model object.
            this.model = (Map<String,Object>)(this.getModel());
            // Register the pre-defined entry for storing field classes.
            this.put(FIELD_CLASSES_KEY, this.fieldClasses);
            // Update internal model with provided object.
            if (model != null) {
                if (model instanceof Map<?,?>) {
                    // Copy all map entries with a string as key.
                    for (Map.Entry<?,?> e : ((Map<?,?>)model).entrySet()) {
                        if (e.getKey() instanceof String) {
                            this.put((String)(e.getKey()), e.getValue());
                        }
                        // Else: ignore entry.
                    }
                }
                else {
                    // Single object model.
                    this.put(model);
                }
            }
        }

        //---------------------------------------------------------------------
        // TemplateModel contract support
        //---------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        public Object put(Object value) {
            return this.model.put(MODEL_KEY, value);
        }

        /** {@inheritDoc} */
        @Override
        public Object get() {
            return this.get(MODEL_KEY);
        }

        /** {@inheritDoc} */
        @Override
        public void registerFieldsFor(Class<?> clazz) {
            if (clazz != null) {
                this.fieldClasses.add(clazz);
            }
            // Else: ignore.
        }

        /** {@inheritDoc} */
        @Override
        public Collection<Class<?>> getFieldClasses() {
            return Collections.unmodifiableCollection(this.fieldClasses);
        }

        @Override
        public void clear() {
            this.model.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            return this.model.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return this.model.containsValue(value);
        }

        @Override
        public Set<java.util.Map.Entry<String,Object>> entrySet() {
            return this.model.entrySet();
        }

        @Override
        public Object get(Object key) {
            return this.model.get(key);
        }

        @Override
        public boolean isEmpty() {
            return this.model.isEmpty();
        }

        @Override
        public Set<String> keySet() {
            return this.model.keySet();
        }

        @Override
        public Object put(String key, Object value) {
            return this.model.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String,? extends Object> m) {
            this.model.putAll(m);
        }

        @Override
        public Object remove(Object key) {
            return this.model.remove(key);
        }

        @Override
        public int size() {
            return this.model.size();
        }

        @Override
        public Collection<Object> values() {
            return this.model.values();
        }
    }
}
