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

    @GET
    public Viewable getIndex() {
        log.debug("Processing GET request");
        return this.newViewable("/index.vm", null);
    }

    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }
}
