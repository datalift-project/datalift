package org.datalift.interlinker;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * @author Bouca Nova Dany
 *
 */
public class BaseModule extends org.datalift.fwk.BaseModule {

	protected BaseModule(String name) {
		super(name);
	}

	protected final TemplateModel newView(String template, Object it) {
		return ViewFactory.newView('/' + this.getName() + '/' + template, it);
	}

	@GET
	@Path("sources/{path: .*$}")
	public Response resolveResource(@PathParam("path") String path, @Context UriInfo uriInfo, @Context Request request,
			@HeaderParam(ACCEPT) String acceptHdr) throws WebApplicationException {
		return Configuration.getDefault().getBean(ResourceResolver.class).resolveModuleResource(this.getName(), uriInfo,
				request, acceptHdr);
	}

	@GET
	@Path("js/{path: .*$}")
	@Produces(TEXT_PLAIN)
	public Response loadJavaScript(@PathParam("path") String path) {
		TemplateModel view = this.newView("js/" + path + ".vm", null);
		return Response.ok(view, TEXT_PLAIN).build();
	}
}
