/*
 * Copyright / LIRMM 2011-2012
 * Contributor(s) : T. Colas, F. Scharffe
 *
 * Contact: thibaud.colas@etud.univ-montp2.fr
 */

package org.datalift.interlink;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
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

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * A {@link ProjectModule project module} that uses the Silk link generation
 * framework to generate links between two datasets.
 * This class is a middle man between our front-end interface & back-end logic.
 * TODO Add a way to set form fields via GET.
 *
 * @author tcolas
 * @version 12082012
 */
@Path(SilkInterlinkController.MODULE_NAME)
public class SilkInterlinkController extends BaseModule implements ProjectModule {
	
	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module's name. */
    public static final String MODULE_NAME = "interlink";
    /** Base name of the resource bundle for converter GUI. */
    protected static final  String GUI_RESOURCES_BUNDLE = "resources";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------
    
    /** Datalift's logger. */
    private static final Logger LOG = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The requested module position in menu. */
    private final int position;
    /** The requested module label in menu. */
    private final String label;
    /** The module's back-end logic handler. */
    private final SilkInterlinkModel model;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

	/**
     * Creates a new SilkInterlinkController instance.
     */
    public SilkInterlinkController() {
        super(MODULE_NAME);
        //TODO Switch to the right position.
        position = 9999999;
        label = PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, this).getString(MODULE_NAME + ".button");
        
        model = new SilkInterlinkModel();
    }
    
    //-------------------------------------------------------------------------
    // Project management
    //-------------------------------------------------------------------------

    /**
     * Retrieves a {@link Project} using its URI.
     * @param projuri the project URI.
     *
     * @return the project.
     * @throws ObjectStreamException if the project does not exist.
     */
    private Project getProject(URI projuri) throws ObjectStreamException {
        ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        Project p = pm.findProject(projuri);
                
        return p;
    }

    /**
     * Handles our Velocity templates.
     * @param templateName Name of the template to parse.
     * @param it Parameters for the template.
     * @return A new viewable in Velocity Template Language
     */
    private Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }

    /**
     * Tells the project manager to add a new button to projects with at least 
     * two sources.
     * @param p Our current project.
     * @return The URI to our project's main page.
     */
    @Override
    public final UriDesc canHandle(Project p) {
        UriDesc uridesc = null;

        try {           
            // The project can be handled if it has at least two RDF sources.
            if (model.hasMultipleRDFSources(p, 2)) {
            	// link URL, link label
                uridesc = new UriDesc(this.getName() + "?project=" + p.getUri(), this.label); 
                
                if (this.position > 0) {
                    uridesc.setPosition(this.position);
                }
                if (LOG.isDebugEnabled()) {
                	LOG.debug(MODULE_NAME + " Project " + p.getTitle() + " can use Silk Interconnection.");
                }
            }
            else {
            	if (LOG.isDebugEnabled()) {
                	LOG.debug(MODULE_NAME + " Project " + p.getTitle() + " can't use Silk Interconnection.");
                }
            }
            
        }
        catch (URISyntaxException e) {
            LOG.fatal("Uh !", e);
            throw new RuntimeException(e);
        }
        return uridesc;
    }

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------
	
	/**
     * Index page handler of the Interlink module.
     * @param projectId the project using Interlink
     * @return Our module's interface.
     * @throws ObjectStreamException ?
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public final Response getIndexPage(@QueryParam("project") URI projectId) throws ObjectStreamException {
        // Retrieve the current project.
        Project proj = this.getProject(projectId);
        LinkedList<String> sourcesURIs = model.getSourcesURIs(proj);
        
        HashMap<String, Object> args = new HashMap<String, Object>();
        
        args.put("it", proj);
        args.put("sources", sourcesURIs);
        args.put("classes", model.getAllClasses(sourcesURIs));
        args.put("predicates", model.getAllPredicates(sourcesURIs));
                
		return Response.ok(this.newViewable("/silk-form.vm", args)).build();
    }

	@POST
	@Path("script")
	@Produces(MediaTypes.TEXT_HTML)
	public final Response doCreate(@QueryParam("project") URI projectId, 
			// Source fields.
			@FormParam("sourceAddress") String sourceAddress,
			@FormParam("sourceQuery") String sourceQuery,
			@FormParam("sourceVariable") String sourceVariable,
			@FormParam("sourcePropertyFirst") String sourcePropertyFirst,
			@FormParam("sourceTransformationFirst") String sourceTransformationFirst,
			@FormParam("sourceRegexpTokenFirst") String sourceRegexpTokenFirst,
			@FormParam("sourceStopWordsFirst") String sourceStopWordsFirst,
			@FormParam("sourceSearchFirst") String sourceSearchFirst,
			@FormParam("sourceReplaceFirst") String sourceReplaceFirst,
			// Optional fields for surnumerous comparisons.
			@FormParam("sourcePropertySecund") String sourcePropertySecund,
			@FormParam("sourceTransformationSecund") String sourceTransformationSecund,
			@FormParam("sourceRegexpTokenSecund") String sourceRegexpTokenSecund,
			@FormParam("sourceStopWordsSecund") String sourceStopWordsSecund,
			@FormParam("sourceSearchSecund") String sourceSearchSecund,
			@FormParam("sourceReplaceSecund") String sourceReplaceSecund,
			@FormParam("sourcePropertyThird") String sourcePropertyThird,
			@FormParam("sourceTransformationThird") String sourceTransformationThird,
			@FormParam("sourceRegexpTokenThird") String sourceRegexpTokenThird,
			@FormParam("sourceStopWordsThird") String sourceStopWordsThird,
			@FormParam("sourceSearchThird") String sourceSearchThird,
			@FormParam("sourceReplaceThird") String sourceReplaceThird,
			// Target fields.
			@FormParam("targetAddress") String targetAddress,
			@FormParam("targetQuery") String targetQuery,
			@FormParam("targetVariable") String targetVariable,
			@FormParam("targetPropertyFirst") String targetPropertyFirst,
			@FormParam("targetTransformationFirst") String targetTransformationFirst,
			@FormParam("targetRegexpTokenFirst") String targetRegexpTokenFirst,
			@FormParam("targetStopWordsFirst") String targetStopWordsFirst,
			@FormParam("targetSearchFirst") String targetSearchFirst,
			@FormParam("targetReplaceFirst") String targetReplaceFirst,
			@FormParam("targetPropertySecund") String targetPropertySecund,
			@FormParam("targetTransformationSecund") String targetTransformationSecund,
			@FormParam("targetRegexpTokenSecund") String targetRegexpTokenSecund,
			@FormParam("targetStopWordsSecund") String targetStopWordsSecund,
			@FormParam("targetSearchSecund") String targetSearchSecund,
			@FormParam("targetReplaceSecund") String targetReplaceSecund,
			@FormParam("targetPropertyThird") String targetPropertyThird,
			@FormParam("targetTransformationThird") String targetTransformationThird,
			@FormParam("targetRegexpTokenThird") String targetRegexpTokenThird,
			@FormParam("targetStopWordsThird") String targetStopWordsThird,
			@FormParam("targetSearchThird") String targetSearchThird,
			@FormParam("targetReplaceThird") String targetReplaceThird,
			// Common comparison & aggregation fields.
			@FormParam("metricFirst") String metricFirst,
			@FormParam("minFirst") String minFirst,
			@FormParam("maxFirst") String maxFirst,
			@FormParam("unitFirst") String unitFirst,
			@FormParam("curveFirst") String curveFirst,
			@FormParam("weightFirst") String weightFirst,
			@FormParam("thresholdFirst") String thresholdFirst,
			// Optional fields for surnumerous comparisons.
			@FormParam("metricSecund") String metricSecund,
			@FormParam("minSecund") String minSecund,
			@FormParam("maxSecund") String maxSecund,
			@FormParam("unitSecund") String unitSecund,
			@FormParam("curveSecund") String curveSecund,
			@FormParam("weightSecund") String weightSecund,
			@FormParam("thresholdSecund") String thresholdSecund,
			@FormParam("metricThird") String metricThird,
			@FormParam("minThird") String minThird,
			@FormParam("maxThird") String maxThird,
			@FormParam("unitThird") String unitThird,
			@FormParam("curveThird") String curveThird,
			@FormParam("weightThird") String weightThird,
			@FormParam("thresholdThird") String thresholdThird,
			// Aggregation method & what to do with the newly created script.
			@FormParam("aggregation") String aggregation,
			@FormParam("runScript") String runScript) throws IOException {  
		// Retrieve the current project.
	    Project proj = this.getProject(projectId);
	    String view;
	    
	    // Trim ALL the fields!
    	sourceAddress = sourceAddress.trim();
    	sourceQuery = sourceQuery.trim();
    	sourceVariable = sourceVariable.trim();
    	sourcePropertyFirst = sourcePropertyFirst.trim();
    	sourceTransformationFirst = sourceTransformationFirst.trim();
    	sourceRegexpTokenFirst = sourceRegexpTokenFirst.trim();
    	sourceStopWordsFirst = sourceStopWordsFirst.trim();
    	sourceSearchFirst = sourceSearchFirst.trim();
    	sourceReplaceFirst = sourceReplaceFirst.trim();
    	sourcePropertySecund = sourcePropertySecund.trim();
    	sourceTransformationSecund = sourceTransformationSecund.trim();
    	sourceRegexpTokenSecund = sourceRegexpTokenSecund.trim();
    	sourceStopWordsSecund = sourceStopWordsSecund.trim();
    	sourceSearchSecund = sourceSearchSecund.trim();
    	sourceReplaceSecund = sourceReplaceSecund.trim();
    	sourcePropertyThird = sourcePropertyThird.trim();
    	sourceTransformationThird = sourceTransformationThird.trim();
    	sourceRegexpTokenThird = sourceRegexpTokenThird.trim();
    	sourceStopWordsThird = sourceStopWordsThird.trim();
    	sourceSearchThird = sourceSearchThird.trim();
    	sourceReplaceThird = sourceReplaceThird.trim();
    	targetAddress = targetAddress.trim();
    	targetQuery = targetQuery.trim();
    	targetVariable = targetVariable.trim();
    	targetPropertyFirst = targetPropertyFirst.trim();
    	targetTransformationFirst = targetTransformationFirst.trim();
    	targetRegexpTokenFirst = targetRegexpTokenFirst.trim();
    	targetStopWordsFirst = targetStopWordsFirst.trim();
    	targetSearchFirst = targetSearchFirst.trim();
    	targetReplaceFirst = targetReplaceFirst.trim();
    	targetPropertySecund = targetPropertySecund.trim();
    	targetTransformationSecund = targetTransformationSecund.trim();
    	targetRegexpTokenSecund = targetRegexpTokenSecund.trim();
    	targetStopWordsSecund = targetStopWordsSecund.trim();
    	targetSearchSecund = targetSearchSecund.trim();
    	targetReplaceSecund = targetReplaceSecund.trim();
    	targetPropertyThird = targetPropertyThird.trim();
    	targetTransformationThird = targetTransformationThird.trim();
    	targetRegexpTokenThird = targetRegexpTokenThird.trim();
    	targetStopWordsThird = targetStopWordsThird.trim();
    	targetSearchThird = targetSearchThird.trim();
    	targetReplaceThird = targetReplaceThird.trim();
    	metricFirst = metricFirst != null ? metricFirst.trim() : "";
    	minFirst = minFirst.trim();
    	maxFirst = maxFirst.trim();
    	unitFirst = unitFirst.trim();
    	curveFirst = curveFirst.trim();
    	weightFirst = weightFirst.trim();
    	thresholdFirst = thresholdFirst.trim();
    	metricSecund = metricSecund != null ? metricSecund.trim() : "";
    	minSecund = minSecund.trim();
    	maxSecund = maxSecund.trim();
    	unitSecund = unitSecund.trim();
    	curveSecund = curveSecund.trim();
    	weightSecund = weightSecund.trim();
    	thresholdSecund = thresholdSecund.trim();
    	metricThird = metricThird != null ? metricThird.trim() : "";
    	minThird = minThird.trim();
    	maxThird = maxThird.trim();
    	unitThird = unitThird.trim();
    	curveThird = curveThird.trim();
    	weightThird = weightThird.trim();
    	thresholdThird = thresholdThird.trim();
    	aggregation = aggregation == null || model.isEmptyValue(aggregation) ? "max" : aggregation.trim();
	    
	    HashMap<String, Object> args = new HashMap<String, Object>();
	    args.put("it", proj);
	    
	    if (LOG.isDebugEnabled()) {
	    	LOG.debug(sourceAddress + " | " + sourceQuery + " | " + sourceVariable + " | " + sourcePropertyFirst + " | " + sourceTransformationFirst + " | " + sourceRegexpTokenFirst + " | " + sourceStopWordsFirst + " | " + sourceSearchFirst + " | " + sourceReplaceFirst + " | " + sourcePropertySecund + " | " + sourceTransformationSecund + " | " + sourceRegexpTokenSecund + " | " + sourceStopWordsSecund + " | " + sourceSearchSecund + " | " + sourceReplaceSecund + " | " + sourcePropertyThird + " | " + sourceTransformationThird + " | " + sourceRegexpTokenThird + " | " + sourceStopWordsThird + " | " + sourceSearchThird + " | " + sourceReplaceThird + " | " + targetAddress + " | " + targetQuery + " | " + targetVariable + " | " + targetPropertyFirst + " | " + targetTransformationFirst + " | " + targetRegexpTokenFirst + " | " + targetStopWordsFirst + " | " + targetSearchFirst + " | " + targetReplaceFirst + " | " + targetPropertySecund + " | " + targetTransformationSecund + " | " + targetRegexpTokenSecund + " | " + targetStopWordsSecund + " | " + targetSearchSecund + " | " + targetReplaceSecund + " | " + targetPropertyThird + " | " + targetTransformationThird + " | " + targetRegexpTokenThird + " | " + targetStopWordsThird + " | " + targetSearchThird + " | " + targetReplaceThird + " | " + metricFirst + " | " + minFirst + " | " + maxFirst + " | " + unitFirst + " | " + curveFirst + " | " + weightFirst + " | " + thresholdFirst + " | " + metricSecund + " | " + minSecund + " | " + maxSecund + " | " + unitSecund + " | " + curveSecund + " | " + weightSecund + " | " + thresholdSecund + " | " + metricThird + " | " + minThird + " | " + maxThird + " | " + unitThird + " | " + curveThird + " | " + weightThird + " | " + thresholdThird + " | " + aggregation + " | " + runScript);
	    }
		
		// We first validate all of the fields.
        LinkedList<String> errorMessages = model.getErrorMessages(sourceAddress, sourceQuery, sourceVariable, sourcePropertyFirst, sourceTransformationFirst, sourceRegexpTokenFirst, sourceStopWordsFirst, sourceSearchFirst, sourceReplaceFirst, sourcePropertySecund, sourceTransformationSecund, sourceRegexpTokenSecund, sourceStopWordsSecund, sourceSearchSecund, sourceReplaceSecund, sourcePropertyThird, sourceTransformationThird, sourceRegexpTokenThird, sourceStopWordsThird, sourceSearchThird, sourceReplaceThird, targetAddress, targetQuery, targetVariable, targetPropertyFirst, targetTransformationFirst, targetRegexpTokenFirst, targetStopWordsFirst, targetSearchFirst, targetReplaceFirst, targetPropertySecund, targetTransformationSecund, targetRegexpTokenSecund, targetStopWordsSecund, targetSearchSecund, targetReplaceSecund, targetPropertyThird, targetTransformationThird, targetRegexpTokenThird, targetStopWordsThird, targetSearchThird, targetReplaceThird, metricFirst, minFirst, maxFirst, unitFirst, curveFirst, weightFirst, thresholdFirst, metricSecund, minSecund, maxSecund, unitSecund, curveSecund, weightSecund, thresholdSecund, metricThird, minThird, maxThird, unitThird, curveThird, weightThird, thresholdThird, aggregation);
        if (errorMessages.isEmpty()) {
        	// Creates the Silk script.
    		File configFile = model.createConfigFile(proj, sourceAddress, sourceQuery, sourceVariable, sourcePropertyFirst, sourceTransformationFirst, sourceRegexpTokenFirst, sourceStopWordsFirst, sourceSearchFirst, sourceReplaceFirst, sourcePropertySecund, sourceTransformationSecund, sourceRegexpTokenSecund, sourceStopWordsSecund, sourceSearchSecund, sourceReplaceSecund, sourcePropertyThird, sourceTransformationThird, sourceRegexpTokenThird, sourceStopWordsThird, sourceSearchThird, sourceReplaceThird, targetAddress, targetQuery, targetVariable, targetPropertyFirst, targetTransformationFirst, targetRegexpTokenFirst, targetStopWordsFirst, targetSearchFirst, targetReplaceFirst, targetPropertySecund, targetTransformationSecund, targetRegexpTokenSecund, targetStopWordsSecund, targetSearchSecund, targetReplaceSecund, targetPropertyThird, targetTransformationThird, targetRegexpTokenThird, targetStopWordsThird, targetSearchThird, targetReplaceThird, metricFirst, minFirst, maxFirst, unitFirst, curveFirst, weightFirst, thresholdFirst, metricSecund, minSecund, maxSecund, unitSecund, curveSecund, weightSecund, thresholdSecund, metricThird, minThird, maxThird, unitThird, curveThird, weightThird, thresholdThird, aggregation);
    		
    		if (Boolean.parseBoolean(runScript)) {    			
    			// Last false means that we don't have to validate all again.
            	args.put("newtriples", model.launchSilk(configFile, SilkInterlinkModel.DEFAULT_NB_THREADS, SilkInterlinkModel.DEFAULT_RELOAD_CACHE, false));
            	view = "interlink-success.vm";
    		}
    		else {
    			// Return the XML file.
    			return Response.ok(configFile, "application/xml").build();
    		}
	    }
	    else {
	    	args.put("errormessages", errorMessages);
	    	view = "interlink-error.vm";
	    }

		return Response.ok(this.newViewable("/" + view, args)).build();
	}

	/**
     * File form submit handler : launching Silk.
     * @param projectId the project using Silk.
     * @param data the form's file upload.
     * @param disposition the form's file upload.
     * @param linkSpecId the id of the interlink to execute.
     * @return Our module's post-process page.
     * @throws ObjectStreamException ?
     */
	@POST
	@Path("run")
	@Consumes(MediaTypes.MULTIPART_FORM_DATA)
	@Produces(MediaTypes.TEXT_HTML)
	public final Response doSubmit(@QueryParam("project") URI projectId, 
			@FormDataParam("configFile") InputStream data, 
			@FormDataParam("configFile") FormDataContentDisposition disposition, 
			@FormDataParam("linkSpecId") String linkSpecId) throws ObjectStreamException {    	   	
		// Retrieve the current project.
	    Project proj = this.getProject(projectId);
	    
	    HashMap<String, Object> args = new HashMap<String, Object>();
	    args.put("it", proj);
	    
	    linkSpecId = linkSpecId.trim();
	    
	    String view;

	    File configFile = model.importConfigFile("interlink-config", data);

	    // We first validate all of the fields.
        LinkedList<String> errorMessages = model.getErrorMessages(configFile, linkSpecId);
        if (errorMessages.isEmpty()) {
        	// Last false means that we don't have to validate all again.
        	args.put("newtriples", model.launchSilk(configFile, linkSpecId, SilkInterlinkModel.DEFAULT_NB_THREADS, SilkInterlinkModel.DEFAULT_RELOAD_CACHE, false));
        	view = "interlink-success.vm";
	    }
	    else {
	    	args.put("errormessages", errorMessages);
	    	view = "interlink-error.vm";
	    }
	    

		return Response.ok(this.newViewable("/" + view, args)).build();
	}
	
	/**
     * Traps accesses to module static resources and redirect them
     * toward the default {@link ResourceResolver} for resolution.
     * @param  path        the relative path of the module static
     *                     resource being accessed.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response JAX-RS response} to download the
     *         content of the specified public resource.
     *         plain-text error message if any error occurred while
     *         accessing the requested resource.
     */
    @GET
	@Path("static/{path: .*$}")
	public final Object getStaticResource(@PathParam("path") String path, 
                                    @Context UriInfo uriInfo, 
                                    @Context Request request, 
                                    @HeaderParam(ACCEPT) String acceptHdr) {
    	
        return Configuration.getDefault()
                            .getBean(ResourceResolver.class)
                            .resolveModuleResource(this.getName(), 
                                                   uriInfo, request, acceptHdr);
    }
}
