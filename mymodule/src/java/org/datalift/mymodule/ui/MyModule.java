/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *                  A. Valensi
 *
 * Contact: dlfr-datalift@atos.net
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

package org.datalift.mymodule.ui;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.datalift.fwk.MediaTypes.TEXT_HTML_UTF8;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.TaskManager;
import org.datalift.fwk.project.TransformationModule;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

@Path(MyModule.MODULE_NAME+ "/")
public class MyModule extends BaseModule implements TransformationModule {

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module name. */
    public static final String MODULE_NAME = "mymodule";
    
    //-------------------------------------------------------------------------
    // Attributes
    //-------------------------------------------------------------------------

    private static Logger logger = Logger.getLogger();
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new module instance.
     */
    public MyModule() {
    	super(MyModule.MODULE_NAME);
    }
    
    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------    
    
    /** {@inheritDoc} */
    @Override
	public String getTransformationId() {
    	return this.getClass().getName();
    }
    
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
    
    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
    	configuration.registerBean(this.getClass().getName(), this);
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
    public Response addProcess(@Context UriInfo uriInfo) {
    	ProcessingTask task = this.getProjectManager().newProcessingTask(
    			this.getTransformationId(),
    			"http://www.datalift.org/project/name/event/");

//    	task.addParam("projectId", projectId);
//    	task.saveParams();
    	
    	TaskManager tm = this.getTaskManager();
    	if (tm == null)
    		logger.error("TaskManager is not initialized");
    	tm.addTask(task);
		logger.debug("[" + task.getUri() + "] Process added");

		logger.info("URI: {}", uriInfo.getRequestUri());
		
    	Response response = null;
    	response = 
    			Response.seeOther(uriInfo.getRequestUri().resolve(".")).build();
    	
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
//		try {
//			task.loadParams();
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
		//String projectId = (String) task.getParam("projectId");

		//logger.debug("Got param: " + projectId);
		logger.info("[" + task.getUri() + "Task is running...");

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
    	
		logger.info("[" + task.getUri() + "] Task done.");
		
		return true;
	}
}