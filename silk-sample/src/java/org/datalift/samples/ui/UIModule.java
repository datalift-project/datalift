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

package org.datalift.samples.ui;


import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;

import com.sun.jersey.api.view.Viewable;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.log.Logger;


public class UIModule extends BaseModule
{
    private final static Logger log = Logger.getLogger();

    /**
     * Creates a new UIModule instance with the default module name
     * "<code>sample-silk</code>".
     */
    public UIModule() {
        super("sample-silk", true);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Map<String, Class<?>> getResources() {
        Map<String, Class<?>> resources = new HashMap<String, Class<?>>();
        resources.put("widget", UIResource.class);
        return resources;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Returns the index page for the module.
     * @return the index page.
     */
    @GET
    public Viewable getIndex() {
        log.debug("Processing GET request");
        return this.newViewable("/index.vm", null);
    }

    /**
     * Return a viewable for the specified template, populated with the
     * specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated viewable.
     */
    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }
}
