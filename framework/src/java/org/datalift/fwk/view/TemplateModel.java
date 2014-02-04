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

package org.datalift.fwk.view;


import java.util.Collection;
import java.util.Map;


/**
 * A web service response referencing a template by name and a model to
 * be passed to the template. Such a response object may be returned by
 * a resource method of a resource class. In this respect the template
 * is the view and the controller is the resource class in the
 * Model-View-Controller pattern.
 * <p>
 * The template name may be declared as absolute template name if the
 * name begins with a '/', otherwise the template name is declared as a
 * relative template name.</p>
 * <p>
 * A relative template name is resolved into an absolute template name
 * by prepending the fully qualified class name of the resource preceded
 * and followed by a '/' character, with any '.' and '$' characters
 * replaced with a '/' character.</p>
 *
 * @author lbihanic
 */
public interface TemplateModel extends Map<String,Object>
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The context key for the default application model object. */
    public final static String MODEL_KEY = "it";
    /** The context key for filed classes. */
    public final static String FIELD_CLASSES_KEY =
                            TemplateModel.class.getName() + ".fieldClasses";

    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------

    /**
     * Returns the template name.
     * @return the template name.
     */
    public String getTemplateName();

    /**
     * Sets the {@link #MODEL_KEY default} model object.
     * @param  value   the default model object.
     *
     * @return the previous default model object.
     */
    public Object put(Object value);

    /**
     * Returns the {@link #MODEL_KEY default} model object.
     * @return the default model object.
     */
    public Object get();

    /**
     * Registers a class to make its constants (static fields)
     * directly available in the template.
     */
    public void registerFieldsFor(Class<?> clazz);

    /**
     * Returns the classes the static fields of which shall be
     * made available to the template.
     */
    public Collection<Class<?>> getFieldClasses();
}
