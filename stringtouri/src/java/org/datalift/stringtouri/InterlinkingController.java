/*
 * Copyright / LIRMM 2011-2012
 * Contributor(s) : T. Colas, F. Scharffe
 *
 * Contact: thibaud.colas@etud.univ-montp2.fr
 */

package org.datalift.stringtouri;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import java.io.ObjectStreamException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * An abstract class for all of the interlinking modules, combining default 
 * operations and values.
 * 
 * @author csuglia, tcolas
 * @version 18062013
 */
public abstract class InterlinkingController extends BaseModule implements ProjectModule {

	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Base name of the resource bundle for converter GUI. */
    protected static final  String GUI_RESOURCES_BUNDLE = "resources";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------
    
    /** Datalift's logger. */
    protected static final Logger LOG = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The requested module position in menu. */
    protected int position;
    /** The requested module label in menu. */
    protected String label;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * InterlinkingController with default behavior.
     * @param name Name of the module.
     * @param pos Position of the module's button.
     */
    public InterlinkingController(String name, int pos) {
        super(name);
                
        position = pos;
    }
    
    //-------------------------------------------------------------------------
    // Project management
    //-------------------------------------------------------------------------

    /**
     * Resource getter.
     * @param key The key to retrieve.
     * @return The value of key.
     */
    protected String getTranslatedResource(String key) {
    	return PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, InterlinkingController.class).getString(key);
    }
    
    /**
     * Display an array as a JSON string
     * @param array the List to convert
     * @return a string containing a JSONized version of the array passed as parameter
     */
    protected String getJsonArray(List<String> array){
    	JsonArray jsonSourceList = new JsonArray();
	    for(String element: array){
	    	JsonPrimitive primSource = new JsonPrimitive(element); 
	    	jsonSourceList.add(primSource);
	    }
	    return new Gson().toJson(jsonSourceList);
    }
    
    /**
     * Get a list of RDF triples as a JSON String
     * @param previewResult the list of RDF triples, where every element will be a list of 3 element
     * @return a JSON string that represents a triple store
     */
    protected String getJsonTriplesMatrix(List<LinkedList<String>> previewResult){
    	JsonArray rowList = new JsonArray();
    	for(LinkedList<String> triple : previewResult){
    		JsonObject jsonTriple = new JsonObject();
    		jsonTriple = new JsonObject();
    		jsonTriple.addProperty("subject", triple.get(0));
    		jsonTriple.addProperty("predicate", triple.get(1));
    		jsonTriple.addProperty("object", triple.get(2));
    		rowList.add(jsonTriple);
    	}
    	return new Gson().toJson(rowList);
    }
    
    /**
     * Get details about the sources as a JSON string
     * @param sources source of the project
     * @return a JSON string that lists and give details about every source
     */
    protected String getJsonSourceArray(List<Source> sources){
    	JsonArray jSourceList = new JsonArray();
    	for(Source linkSource: sources){
    		JsonObject jSource = new JsonObject();
    		jSource.addProperty("title",linkSource.getTitle());
    		if(linkSource.getDescription()!=null){
    			jSource.addProperty("description", linkSource.getDescription());
    		}
    		jSource.addProperty("url", linkSource.getUri());
    		jSourceList.add(jSource);
    	}
    	return new Gson().toJson(jSourceList);
    }
    
    /**
     * Retrieves a {@link Project} using its URI.
     * @param projuri the project URI.
     *
     * @return the project.
     * @throws ObjectStreamException if the project does not exist.
     */
    protected final Project getProject(URI projuri) throws ObjectStreamException {
        ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        Project p = pm.findProject(projuri);
		if(p == null){
			throw new TechnicalException("Project not found");
		}
        return p;
    }
    
    /**
     * 
     * @param respObject
     * @return a response object that contains the object passed
     */
    protected Response getOkResponse(Object respObject){
    	ResponseBuilder respBuilder = Response.ok(respObject);
    	return respBuilder.build();
    }
    

    /**
     * Handles our Velocity templates.
     * @param templateName Name of the template to parse.
     * @param it Parameters for the template.
     * @return A new viewable in Velocity Template Language
     */
    protected final TemplateModel newViewable(String templateName, Object it) {
        return ViewFactory.newView("/" + this.getName() + templateName, it);
    }
    
    /**
     * Tells the project manager to add a new button to projects with at least 
     * two sources.
     * @param p Our current project.
     * @return The URI to our project's main page.
     */
    public abstract UriDesc canHandle(Project p);
    
    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * Index page handler for interlinking modules.
     * @param projectId the project using our module.
     * @return Our module's interface.
     * @throws ObjectStreamException A Obscene Reject Mixt Opt.
     */
    public abstract Response getIndexPage(@QueryParam("project") URI projectId) throws ObjectStreamException;
    
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
     */
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
}
