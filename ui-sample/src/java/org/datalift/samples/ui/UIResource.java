package org.datalift.samples.ui;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.datalift.fwk.log.Logger;

import com.sun.jersey.api.view.Viewable;


public class UIResource
{
    private final static Logger log = Logger.getLogger();

    private final UIModule m;

    public UIResource(UIModule module) {
    	m = module;
    }

    @Path("{widget}")
    @GET
    public Viewable getWidget(@PathParam("widget") String widget) {
        log.debug("Processing GET request on {}", widget);
    	return this.m.newViewable("/widgets/"+ widget +".vm", null);
    }
}
