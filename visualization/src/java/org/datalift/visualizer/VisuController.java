/*
 * Copyright / Copr. 2010-2013  - EURECOM -  for the DataLift project
 * Contributor(s) : Ghislain Atemezing, Raphaël Troncy
 *
 * Contact: atemezin@eurecom.fr
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

package org.datalift.visualizer;

import java.io.ObjectStreamException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;


/**
 * Visu main controller class, making its model and view cooperate.
 * Exposes /visualization as DVIA's main URL.
 *
 * @author gatemezing, rtroncy
 */
@Path(VisuController.MODULE_NAME)
public class VisuController extends ModuleController {
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module's name. */
    public static final String MODULE_NAME = "visualization";
    private final static Logger log = Logger.getLogger();
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The module's back-end logic handler. */
    protected final VisuModel model;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new VisuController instance.
     */
    public VisuController() {
        super(MODULE_NAME, 9100);

        //label = getTranslatedResource(MODULE_NAME + "visualization.button");
        model = new VisuModel(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Project management
    //-------------------------------------------------------------------------

    /**
     * Tells the project manager to add a new button to projects with at least
     * two sources.
     * @param p Our current project.
     * @return The URI to our project's main page.
     */
    public final UriDesc canHandle(Project p) {
        UriDesc projectURL = null;
        try {
            if (p.getSources().size()>1) {
                // The URI should be a URI for running the interconnection
                projectURL = new UriDesc(this.getName() + "?project=" + p.getUri(),
                                         "Simple Visualisation");
                if (this.position > 0) {
                    projectURL.setPosition(this.position);
                }
            }
        }
        catch (Exception e) {
            log.fatal("Uh?", e);
            throw new RuntimeException(e);
        }
        return projectURL;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * Index page handler of the visualization module.
     * @param projectId the project using visualization
     * @return Our module's interface.
     * @throws ObjectStreamException
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getIndexPage(@QueryParam("project") URI projectId)
                                                throws ObjectStreamException {
        // Retrieve the current project and its sources.
        Project proj = this.getProject(projectId);

       LinkedList<String> sourcesURIs = model.getSourcesURIs(proj);

        // Display visualization page.
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("it", proj);
        args.put("visualizing", this);
        
		args.put("sources", sourcesURIs);
        
		
        return Response.ok(this.newViewable("/sgvizler.vm", args)).build();
    }

    @GET
    @Path("visu-sgvizler")
    @Produces(MediaType.TEXT_HTML)
    public Response getIndexPage2(
                        @QueryParam("sgvzlr_cQuery") String sgvzlr_cQuery,
                        @QueryParam("sgvzlr_formQuery") String sgvzlr_formQuery,
                        @QueryParam("sgvzlr_endpoint") String sgvzlr_endpoint)
                                                  throws ObjectStreamException {
        // Display conversion configuration page.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("cquery", sgvzlr_cQuery);
        args.put("formQuery", sgvzlr_formQuery);
        args.put("endpoint", sgvzlr_endpoint);
        args.put("visualizing", this);

        return Response.ok(this.newViewable("/sgvizler-view.vm", args))
                       .build();
    }

    @GET
    @Path("static/{path: .*$}")
    public Object getStaticResource(@PathParam("path") String path,
                                    @Context UriInfo uriInfo,
                                    @Context Request request,
                                    @HeaderParam(ACCEPT) String acceptHdr) {
        return Configuration.getDefault()
                            .getBean(ResourceResolver.class)
                            .resolveModuleResource(this.getName(),
                                                   uriInfo, request, acceptHdr);
    }

    /**
     * File form submit handler : launching sgvizler.
     * @param projectId the project using visualizer.
     * @param sgvzler_prefixes the prefixes of the form.
     * @param sgvzler_query the query.
     * @param sgvzler_endpoint the endpoint.
     * @param sgvzler_chartWidth the width of the chart to build.
     * @param sgvzler_chartHeight the height of the chart to build.
     * @param sgvzler_chartType the type of the chart to build.
     * @return Our module's post-process page.
     * @throws ObjectStreamException ?
     */
    @POST
    @Path("visualize")
    @Consumes(MediaTypes.APPLICATION_FORM_URLENCODED)
    @Produces(MediaTypes.TEXT_HTML)
    public final Response doVisualize(@QueryParam("project") URI projectId)
                                                throws ObjectStreamException { 
        // Retrieve the current project.
        Project proj1 = this.getProject(projectId);

        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("it", proj1);
        args.put("visualizing", this);

        String view = "sgvizler-view.vm";
        return Response.ok(this.newViewable("/" + view, args)).build();
    }
}
