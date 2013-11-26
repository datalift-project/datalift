package org.datalift.mymodule.ui;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.datalift.fwk.MediaTypes.TEXT_HTML_UTF8;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.TaskManager;
import org.datalift.fwk.project.TransformationModule;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

@Path(MyModule.MODULE_NAME)
public class MyModule extends BaseModule implements TransformationModule {

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module name. */
    public static final String MODULE_NAME = "mymodule";
    
    //-------------------------------------------------------------------------
    // Attributes
    //-------------------------------------------------------------------------

    private static Logger logger = Logger.getLogger(MyModule.class);
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new module instance.
     */
    public MyModule() {
    	super(MyModule.MODULE_NAME);
    }
    
    /** {@inheritDoc} */
    @Override
	public URI getTransformationId() {
    	return URI.create(this.getName());
    }
    
    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------    
    
    private TaskManager getTaskManager() {
    	TaskManager taskManager = 
    			Configuration.getDefault().getBean(TaskManager.class);
    	if (taskManager == null)
    		throw new RuntimeException("TaskManager is not initialized");
    	return taskManager;
    }
    
    private ProjectManager getProjectManager() {
    	ProjectManager projectManager = 
    			Configuration.getDefault().getBean(ProjectManager.class);
    	if (projectManager == null)
    		throw new RuntimeException("ProjectManager is not initialized");
    	return projectManager;
    }
    
    /**
     * Return a model for the specified template view, populated with
     * the specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated template model.
     */
    private TemplateModel newView(String templateName, Object it) {
        return ViewFactory.newView(
                                "/" + this.getName() + '/' + templateName, it);
    }
    
    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response index() {
    	Response response = null;

    	TemplateModel view = this.newView("index.vm", null);
    	//view.put("converter", this);
    	response = Response.ok(view, TEXT_HTML_UTF8).build();

    	return response;
    }
    
    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    @Path("addprocess")
    public Response addProcess(String projectId) {
    	ProcessingTask task = this.getProjectManager().newProcessingTask(
    			this.getTransformationId());

    	task.addParam("projectId", projectId);
    	task.saveParams();
    	
    	TaskManager tm = this.getTaskManager();
    	if (tm == null)
    		logger.error("TaskManager is not initialized");
    	tm.addTask(task);
		logger.debug("process added");
    	System.out.println("[" + projectId + "] Process added.");

    	Response response = null;
    	TemplateModel view = this.newView("index.vm", null);
    	response = Response.ok(view, TEXT_HTML_UTF8).build();
    	return response;
    }
    
    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    @Path("gethistory")
    public String getHistory() {
    	///http://localhost:9091/sparql?query=SELECT...&default-graph-uri=internal
    	
    	return "history";
    }

    //-------------------------------------------------------------------------
    // TransformationModule contract
    //-------------------------------------------------------------------------
    
	@Override
	public Boolean execute(ProcessingTask task) {
		logger.debug("execute()");
		try {
			task.loadParams();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		String projectId = (String) task.getParam("projectId");

		logger.debug("Got param: " + projectId);
		logger.info("Task is running...");
    	System.out.println("[" + projectId + "] Task is running...");

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
    	
    	System.out.println("[" + projectId + "] Task done.");
		logger.info("Task done.");
		
		return true;
	}
}