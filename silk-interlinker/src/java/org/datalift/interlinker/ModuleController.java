package org.datalift.interlinker;

import java.net.URI;
import java.util.ResourceBundle;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.SqlDatabaseSource;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import static org.datalift.fwk.MediaTypes.*;

/**
 * A superclass for the DataLift Silk Interlinker Module, which can discover links between repositories
 * in the data store give a configuration file.
 * 
 * @author csuglia
 * 
 */
public abstract class ModuleController extends BaseModule implements ProjectModule {

	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Base name of the resource bundle for converter GUI. */
    protected static final  String GUI_RESOURCES_BUNDLE = "resources";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------
    
    /** Datalift's logger. */
    protected static final Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The requested module position in menu. */
    protected int position;
    /** The requested module label in menu. */
    protected String label;
    /** The DataLift project manager. */
    protected ProjectManager projectManager;
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Interlinking Controller with default behavior.
     * @param name Name of the module.
     * @param pos Position of the module's button.
     */
    public ModuleController(String name, int pos) {
        super(name);
        this.position = pos;
        this.label = getTranslatedResource(name + ".button");
    }
    
    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Retrieves a {@link Project} using its URI.
     * @param projuri the project URI.
     *
     * @return the project.
     * @throws ObjectNotFoundException if the project does not exist.
     */
    protected final Project getProject(URI projuri) throws ObjectNotFoundException {
        ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        Project p = pm.findProject(projuri);
        if (p == null) {
            throw new ObjectNotFoundException("project.not.found", projuri);
        }
        return p;
    }

    /**
	 * Resource getter.
	 * @param key The key to retrieve.
	 * @return The value of key.
	 */
	protected String getTranslatedResource(String key) {
		ResourceBundle localeBundle = PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, ModuleController.class);
		return localeBundle.getString(key);
	}

	/**
     * Handles the Velocity templates.
     * @param templateName Name of the template to parse.
     * @param it Parameters for the template.
     * @return A new viewable in Velocity Template Language
     */
    protected final TemplateModel newViewable(String templateName, Object it) {
        return ViewFactory.newView("/" + this.getName() + templateName, it);
    }
    
    
    //-------------------------------------------------------------------------
    // Web Service
    //-------------------------------------------------------------------------
    
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
    
    /**
     * Get a template model builder to redirect the user from the module page to the source list
     * @param src the source created
     * @return a Response Builder, which can build a service response to redirect the browser to the 
     * source list, basing on the redirect velocity script.
     */
    protected ResponseBuilder getSourceListPage(Source src){
    	String targetUrl = src.getProject().getUri() + "#source";
    	TemplateModel template = ViewFactory.newView("/" + this.getName() + "/redirect.vm" ,targetUrl);
    	return Response.created(URI.create(src.getUri()))
                   .entity(template)
                   .type(TEXT_HTML_UTF8);
    }
    
    /**
     * Returns a service response displaying the specified project
     * using the specified template.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  projectId      the URI of the project to display.
     *
     * @return a template model for rendering the specified template,
     *         populated with the specified project.
     */
    protected final Response newProjectView(String templateName,
                                            URI projectId) {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Display the module page relying on a velocity script.
            TemplateModel view = this.newViewable(templateName, p);
            //pass to the script the project and this module, so it can display info and settings
   	     	view.put("it", p);
   	     	view.put("module", this);  
            response = Response.ok(view, TEXT_HTML_UTF8).build();
        }catch(ObjectNotFoundException e){
        	log.fatal(e);
        	throw new RuntimeException(e);
        }
        
        return response;
    }
    
    //-------------------------------------------------------------------------
    // ProjectModule contract support
    //-------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    /**
     * Get the URI of the module main web page.
     * @param p Our current project.
     * @return <code>null</code> if there are no source compatible with the modules;
     *        The uri of the module if there are sources that can be converted.
     */
    @Override
	public abstract UriDesc canHandle(Project p);
    
    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);
        this.projectManager = configuration.getBean(ProjectManager.class);
    }
    
        
    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + " (\"/" + this.getName() + "\" " + SqlDatabaseSource.class.getSimpleName() + ')';
    }


}