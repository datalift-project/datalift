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


import org.datalift.fwk.Configuration;


/**
 * A pluggable factory for allocating view
 * {@link TemplateModel template models} web service responses. A
 * concrete implementation shall be made available by every Datalift
 * framework implementations.
 *
 * @author lbihanic
 */
public abstract class ViewFactory
{
    private static ViewFactory provider = null;

    /**
     * Returns a new template model for web service response rendering.
     * @param  templateName   the template name, absolute or relative.
     *
     * @return a new template view.
     * @see    #newView(String, Object)
     */
    public static TemplateModel newView(String templateName) {
        return newView(templateName, null);
    }

    /**
     * Returns a new template model for web service response rendering
     * with an associated model object.
     * <p>
     * If <code>model</code> is a {@link Map}, it shall contain the
     * whole object model and its <strong>content</strong> is copied
     * into the object model (non-string keys are ignored). Otherwise
     * the object is associated to the
     * {@link TemplateModel#MODEL_KEY default model key}.<br />It is
     * possible to associate a Map to the default model key by using
     * the following two-step method:</p>
     * <blockquote><pre>
     *  Map map = ...
     *  TemplateModel tm = ViewFactory.newView(templateName);
     *  tm.put(map);
     * </pre></blockquote>
     * @param  templateName   the template name, absolute or relative.
     * @param  model          the (optional) model object.
     *
     * @return a new template view.
     */
    public static TemplateModel newView(String templateName, Object model) {
        if (provider == null) {
            provider = Configuration.getDefault().getBean(ViewFactory.class);
        }
        return provider.createView(templateName, model);
    }

    /**
     * Factory method concrete sub-classes shall implements to
     * instantiate {@link TemplateModel} implementations.
     * @param  templateName   the template name, absolute or relative.
     * @param  model          the (optional) model object.
     *
     * @return a template model implementation.
     * @see    #createView(String, Object)
     */
    abstract protected TemplateModel createView(String templateName,
                                                Object model);
}
