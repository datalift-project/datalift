package org.datalift.mymodule.ui;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.datalift.core.project.ProcessingTaskImpl;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.TransformationModule;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.TaskManager;

@Path(MyModule.MODULE_NAME)
public class MyModule extends BaseConverterModule implements TransformationModule {

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module name. */
    public static final String MODULE_NAME = "mymodule";
    
    //-------------------------------------------------------------------------
    // Attributes
    //-------------------------------------------------------------------------

	private static Logger logger = Logger.getLogger(MyModule.class);
    TaskManager tm = null;
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new module instance.
     */
    public MyModule() {
        super(MODULE_NAME, 5000, SourceType.CsvSource);
    }
        
    public void postInit(Configuration cfg) {
    	tm = cfg.getBean(tm.getClass());
    }
    
    
    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Returns the index page for the module.
     * @return the index page.
     */
    @GET
    public TemplateModel getIndex(@Context UriInfo uriInfo) {
        return ViewFactory.newView("/" + this.getName() + "/index.vm");
    }
    
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public TemplateModel addProcess(@FormParam("project") String projectId) {
    	System.out.println("param: " + projectId);

    	ProcessingTaskImpl task = new ProcessingTaskImpl(this.getTransformationId());
    	
    	// TODO: verify
    	// e.setDescription(this.getTransformationId().toString());
    	
    	task.addParam("projectId", projectId);
    	task.saveParams();
    	
    	this.tm.addTask(task);
    	
        return ViewFactory.newView("/" + this.getName() + "/index.vm");
    }

    //-------------------------------------------------------------------------
    // TransformationModule contract
    //-------------------------------------------------------------------------
    
    /**
     * TODO: put in superclass.
     */
	@Override
	public URI getTransformationId() {
		URI id = null;
		try {
			id = new URI(MyModule.MODULE_NAME);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return id;
	}

	@Override
	public void execute(ProcessingTask task) {
		try {
			task.loadParams();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		String projectId = (String) task.getParam("projectId");

		logger.debug("Got param: " + projectId);
		logger.info("Task is running...");

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
    	
		logger.info("Task done.");
	}
}
