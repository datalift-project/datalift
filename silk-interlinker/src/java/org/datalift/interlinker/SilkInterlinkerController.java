package org.datalift.interlinker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.project.Project;

import org.silk.interlinker.script.InterlinkedSourcesInfo;
import org.silk.interlinker.script.ScriptFileWriter;
import org.silk.interlinker.script.SilkSource;

import com.sun.jersey.multipart.FormDataParam;



import static org.datalift.fwk.MediaTypes.*;

@Path(SilkInterlinkerController.MODULE_NAME)
public class SilkInterlinkerController extends ModuleController{
	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration.  */
    public static final String MODULE_NAME = "silk-interlinker";
    
    private final static String SOURCE_LIST_SERVICE_PATH="sources";
    
    private final static String PREDICATE_LIST_SERVICE_PATH="predicates";
    
    private final static String GET_SCRIPT_PATH="script";
    
    private final static String RUN_SCRIPT_PATH="run";
    
    private final static String UPLOAD_SILK_FILE_PATH="upload-silk";
    
    private final static String UPLOAD_EDOAL_FILE_PATH="upload-edoal";
    
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
	public SilkInterlinkerController() {
		super(MODULE_NAME, 9000);
		model = new SilkInterlinkerModel();
	}

	
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The module's back-end logic handler. */
    protected SilkInterlinkerModel model;

	
	//-------------------------------------------------------------------------
    // Specific Implementation
    //-------------------------------------------------------------------------
	
	/**
     * Tells the project manager to add a new button to projects with at least 
     * two sources.
     * @param p Our current project.
     * @return The URI to our project's main page.
     */
    public final UriDesc canHandle(Project p) {
        UriDesc uridesc = null;

        try {           
            // The project can be handled if it has at least two RDF sources.
            if (model.hasRDFSource(p)) {
            	// link URL, link label
                uridesc = new UriDesc(this.getName() + "?project=" + p.getUri(), this.label); 
                
                if (this.position > 0) {
                    uridesc.setPosition(this.position);
                }
            }
        }
        catch (URISyntaxException e) {
            log.fatal("URI syntax error", e);
            throw new TechnicalException(e);
        }
        return uridesc;
    }

	
    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------
	 
	/**
	 * Display the Silk Interlinker module home page
	 */
    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam("project") URI projectId) {
    	return this.newProjectView("/link-discoverer.vm", projectId);
    }

    /**
     * Produce the silk script and return its relative path
     * @param sourcesToInterlink
     * @return the relative path where the script is located
     * @throws URISyntaxException 
     * @throws IOException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(GET_SCRIPT_PATH)
    public Response getScriptPath(InterlinkedSourcesInfo sourcesToInterlink,
    		@Context UriInfo uriInfo) throws URISyntaxException {
    	//create the script and return its path
    	File silkScript =  ScriptFileWriter.createSilkScript(sourcesToInterlink);
    	String scriptPath = silkScript.getPath();
    	//the partial path will be used to build the remote path
    	URI scriptFileUri = new URI(uriInfo.getBaseUri() + scriptPath.substring(scriptPath.indexOf("project")));
    	//now that we got the complete path we can send it
    	return Response.created(scriptFileUri).build();
    }
    
    /**
     * Get the source list, where every element is the name
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(SOURCE_LIST_SERVICE_PATH)
    public Response getSourceList(@QueryParam("project") URI projectId){
	    try {
			Project proj = this.getProject(projectId);
			String jsonSources = model.getJsonSources(proj);
			return Response.ok(jsonSources, MediaType.APPLICATION_JSON).build();
		} catch (ObjectStreamException e) {
			throw new TechnicalException(e);
		}
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PREDICATE_LIST_SERVICE_PATH)
    public Response getPredicateList(@QueryParam("source") String sourceId){
    	List<String> predicates = model.getPredicates(sourceId);
    	String jsonPredicates = model.getSimpleJsonArray(predicates);
    	return Response.ok(jsonPredicates).build();
    }
    
    
    @POST
    @Path(UPLOAD_SILK_FILE_PATH)
    @Consumes(MediaTypes.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response runUploadedSilkScript(@QueryParam("project") URI projectId,
    		@FormDataParam("inputSilkFile") InputStream scriptStream,
    		@FormDataParam("inputSilkIdentifier") String silkIdentifier) throws ObjectStreamException{
    	Project proj = this.getProject(projectId);
    	HashMap<String, Object> args = new HashMap<String, Object>();
	    args.put("it", proj.getUri());
    	String linkSpecId = silkIdentifier.trim();
    	File configFile = model.importConfigFile("interlink-config", scriptStream);

        LinkedList<String> errorMessages = model.getErrorMessages(configFile, linkSpecId);
        if (errorMessages.isEmpty()) {
        	model.launchSilk(proj,configFile, null,null,null, linkSpecId, 
        			SilkInterlinkerModel.DEFAULT_NB_THREADS, SilkInterlinkerModel.DEFAULT_RELOAD_CACHE, false);
        	return Response.ok(this.newViewable("/redirect.vm" , args)).build();
    	}else{
    		return Response.status(Status.BAD_REQUEST).build();
    	}
    	
    	
    }
    
    @POST
    @Path(UPLOAD_EDOAL_FILE_PATH)
    @Consumes(MediaTypes.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response runUploadedEdoalScript(
    		@QueryParam("project") URI projectId,
    		@FormDataParam("inputEdoalFile") InputStream scriptStream,
    		@FormDataParam("selectEdoalSource") String edoalSource,
    		@FormDataParam("selectEdoalTarget") String edoalTarget,
    		@FormDataParam("edoalMetricSelect") String metric,
    		@FormDataParam("edoalThreshold") String thresold){
    	HashMap<String, Object> args = new HashMap<String, Object>();
	    args.put("it", projectId);
    	model.convertEdoalScript(scriptStream, edoalSource, edoalTarget, metric, thresold);
    	return Response.ok(this.newViewable("/redirect.vm" , args)).build();
    }
    
    @POST
    @Path(RUN_SCRIPT_PATH)
    public Response createSourceFromScript(SilkSource silkSource,
    		@Context UriInfo uriInfo) throws ObjectStreamException, URISyntaxException{
    	Project proj = this.getProject(silkSource.getProject());
    	File scriptFile = Configuration.getDefault().getPublicStorage().getFile(silkSource.getScriptFilePath());
    	LinkedList<LinkedList<String>> ret = model.launchSilk(proj,scriptFile, silkSource.getTargetContext(), silkSource.getNewSourceContext(), silkSource.getNewSourceName(),
    			SilkInterlinkerModel.DEFAULT_NB_THREADS, SilkInterlinkerModel.DEFAULT_RELOAD_CACHE, false);
    	if(ret.isEmpty()){
    		return Response.created(new URI(silkSource.getNewSourceContext())).build();
    	}else{
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    	}
    	
    }
    
  
}
