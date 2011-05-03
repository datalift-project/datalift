package org.datalift.samples.ui;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.datalift.fwk.log.Logger;

import com.sun.jersey.api.view.Viewable;


public class UIResource
{
    private final static Logger log = Logger.getLogger();

    /** The parent module. */
    private final UIModule m;

    /**
     * Creates a new resource instance.
     * @param  module   the parent module.
     */
    public UIResource(UIModule module) {
    	m = module;
    }

    /**
     * <i>[Resource method]</i> Returns the page to display the
     * specified widget.
     * @param  widget   the widget name (request path element).
     * @return the widget page.
     */
    @Path("{widget}")
    @GET
    public Viewable getWidget(@PathParam("widget") String widget) {
        log.debug("Processing GET request on {}", widget);
    	return this.m.newViewable("/widgets/"+ widget +".vm", null);
    }
}
