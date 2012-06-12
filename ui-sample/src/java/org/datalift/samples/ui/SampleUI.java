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


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;


@Path(SampleUI.MODULE_NAME)
public class SampleUI extends BaseModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module name. */
    public static final String MODULE_NAME = "sample-ui";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new UIModule instance.
     */
    public SampleUI() {
        super(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Returns the index page for the module.
     * @return the index page.
     */
    @GET
    public TemplateModel getIndex(@Context UriInfo uriInfo) {
        return ViewFactory.newView("/" + uriInfo.getPath() + "/index.vm");
    }

    /**
     * <i>[Resource method]</i> Returns the page to display the
     * specified widget.
     * <p>
     * This method is actually a JAS-RS "sub-resource locator" method
     * that demonstrates usage of sub-resource objects.</p>
     * @param  widget   the widget name (request path element).
     *
     * @return the widget page.
     */
    @Path("widget/{widget}")
    public WidgetResource getWidget(@PathParam("widget") String widget) {
        return new WidgetResource();
    }

    //-------------------------------------------------------------------------
    // WidgetResource nested class
    //-------------------------------------------------------------------------

    /**
     * A JAX-RS sub-resource class return a widget page.
     */
    public final static class WidgetResource
    {
        /**
         * <i>[Resource method]</i> Returns the page displaying the
         * specified widget.
         * <p>
         * This method demonstrates usage of {@link UriInfo} to extract
         * the information from the request path.</p>
         * @param  uriInfo   the JAX-RS request URI data.
         *
         * @return the widget page.
         */
        @GET
        public TemplateModel getWidget(@Context UriInfo uriInfo) {
            String path = uriInfo.getPath();
            log.debug("Sub-resource processing of GET request on {}", path);
            return ViewFactory.newView("/" + path + ".vm");
        }
    }
}
