package org.datalift.core.project;

import static org.datalift.core.DefaultConfiguration.DATALIFT_HOME;
import static org.datalift.core.DefaultConfiguration.PRIVATE_STORAGE_PATH;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_DEFAULT_FLAG;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_URIS;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_URL;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.datalift.core.DefaultConfiguration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.TaskManager;
import org.datalift.fwk.project.TransformationModule;
import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TaskManagerTest {
	private final static String RDF_STORE = "internal";
	private Properties props = new Properties();
	private TaskManager tm = new TaskManagerImpl();
	DefaultConfiguration cfg;
	protected volatile boolean processExecuted = false;


	@Before
	public void setUp() throws Exception {
		this.props.put(DATALIFT_HOME, "tests");
		this.props.put(REPOSITORY_URIS, RDF_STORE);
		this.props.put(RDF_STORE + REPOSITORY_URL, "sail:///");
		//this.props.put(RDF_STORE + REPOSITORY_URL, 
		//		"http://localhost:9091/openrdf-sesame/repositories/internal");
		this.props.put(RDF_STORE + REPOSITORY_DEFAULT_FLAG, "true");
		this.props.put(PRIVATE_STORAGE_PATH, ".");
		
        // Load application configuration.
        cfg = new DefaultConfiguration(this.props);
        cfg.init();
        Configuration.setDefault(cfg);

        // Init ProjectManager.
		DefaultProjectManager pm = new DefaultProjectManager();
		pm.init(cfg);
		pm.postInit(cfg);
        cfg.registerBean(pm);

        // Init task manager.
        cfg.registerBean(tm);
        tm.init(cfg);
        
        Project p = pm.newProject(
        		new URI("http://projet-test"), 
        		"projet-test", 
        		"Testing project", 
        		new URI("http://test"));
        pm.saveProject(p);
	}
	
	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testAddEvent() throws Exception {
		TestModule m = new TestModule();
		cfg.registerBean(m.getTransformationId(), m);
		m.postInit(cfg);
		m.addProcess("http://projet-test");
		tm.shutdown(cfg);
		assertTrue(processExecuted);

	}
	
	public class TestModule extends BaseModule implements TransformationModule{

	    //----------------------------------------------------------------------
	    // Constants
	    //----------------------------------------------------------------------

	    /** The module name. */
	    public static final String MODULE_NAME = "mymodule";
	    
	    //----------------------------------------------------------------------
	    // Attributes
	    //----------------------------------------------------------------------
	    
	    private Logger logger = Logger.getLogger();
	    
	    //----------------------------------------------------------------------
	    // Constructors
	    //----------------------------------------------------------------------

	    /**
	     * Creates a new module instance.
	     */
	    public TestModule() {
	    	super(TestModule.MODULE_NAME);
	    }
	        
	    //----------------------------------------------------------------------
	    // Web services (in real usage)
	    //----------------------------------------------------------------------
	    
	    /**
	     * This method create a task to be given to the task manager. When a 
	     * task is created, it is possible to save the parameters to use it in
	     * the method which will execute the transformation.
	     * 
	     * @param projectId
	     * @throws URISyntaxException 
	     */
	    public void addProcess(String projectId) throws URISyntaxException {
	    	ProcessingTaskImpl task = new ProcessingTaskImpl(
	    			this.getTransformationId(),
	    			new URI(projectId),
	    			new URI("http://test.src"));
	    	
	    	task.addParam("projectId", projectId);
	    	task.saveParams();
	    	
	    	TaskManager tm = this.getTaskManager();
	    	if (tm == null)
	    		logger.error("TaskManager is not initialized");
	    	tm.addTask(task);
	    	logger.info("Process added.");
	    }

	    //----------------------------------------------------------------------
	    // TransformationModule contract
	    //----------------------------------------------------------------------
	    
	    /**
	     * It is the method which execute the transformation. Here it is in the
	     * same class than the web services but it can be nice to put it in a
	     * separate class.
	     * 
	     * @param task is the task made in the web service.
	     */
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

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
	    	
			processExecuted = true;
	    	logger.info("Task done.");
			
			return true;
		}

		/**
		 * This method return the transformation id. The transformation id is
		 * needed to register the class to get it when we want to execute the
		 * transformation.
		 * 
		 * TODO: Put this class in an abstract class, because it is a recurrent 
		 * implementation.
		 * 
		 * @return the transformation id.
		 */
		@Override
		public String getTransformationId() {
			return this.getClass().getName();
		}

	    //----------------------------------------------------------------------
	    // Specific implementation
	    //----------------------------------------------------------------------

		/**
		 * This method return the task manager
		 * 
		 * TODO: Put this class in an abstract class, because it is a recurrent 
		 * implementation.
		 * 
		 * @return the task manager.
		 */
	    public TaskManager getTaskManager() {
	    	TaskManager taskManager = 
	    			Configuration.getDefault().getBean(TaskManager.class);
	    	if (taskManager == null)
	    		throw new RuntimeException("TaskManager is not initialized");
	    	return taskManager;
	    }

	}
}
