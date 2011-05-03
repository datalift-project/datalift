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
     * "<code>sample-ui</code>".
     */
    public UIModule() {
        super("sample-ui", true);
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
