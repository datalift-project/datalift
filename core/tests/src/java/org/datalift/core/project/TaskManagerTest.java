package org.datalift.core.project;

import static org.datalift.core.DefaultConfiguration.DATALIFT_HOME;
import static org.datalift.core.DefaultConfiguration.PRIVATE_STORAGE_PATH;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_DEFAULT_FLAG;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_URIS;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_URL;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.datalift.core.DefaultConfiguration;
import org.datalift.fwk.project.ProcessingTask;
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
	private static Logger logger = Logger.getLogger(Prov.class);
	private Properties props = new Properties();
	private TaskManager tm = new TaskManagerImpl();
	DefaultConfiguration cfg;
	protected volatile boolean processExecuted = false;


	@Before
	public void setUp() throws Exception {
		this.props.put(DATALIFT_HOME, "tests");
		this.props.put(REPOSITORY_URIS, RDF_STORE);
		this.props.put(RDF_STORE + REPOSITORY_URL, "sail:///");
		//	        this.props.put(RDF_STORE + REPOSITORY_URL, 
		//	        		"http://localhost:9091/openrdf-sesame/repositories/tests");
		this.props.put(RDF_STORE + REPOSITORY_DEFAULT_FLAG, "true");
		this.props.put(PRIVATE_STORAGE_PATH, ".");
		
        // Load application configuration.
        cfg = new DefaultConfiguration(this.props);
        cfg.init();
        Configuration.setDefault(cfg);

        cfg.registerBean(tm);
        tm.start();
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testAddEvent() throws Exception {
		TestModule m = new TestModule();
		TestModule n = new TestModule();
		cfg.registerBean(m.getTransformationId().toString(), m);
		m.postInit(cfg);
		m.addProcess("process1");
//		Thread.sleep(1000);
		m.addProcess("process2");
		m.addProcess("process3");
		m.addProcess("process4");
		m.addProcess("process5");
		m.addProcess("process6");
		tm.shutdown(true, 10);
		assertTrue(processExecuted);

	}
	
	public class TestModule extends BaseModule implements TransformationModule {

	    //-------------------------------------------------------------------------
	    // Constants
	    //-------------------------------------------------------------------------

	    /** The module name. */
	    public static final String MODULE_NAME = "mymodule";
	    
	    //-------------------------------------------------------------------------
	    // Attributes
	    //-------------------------------------------------------------------------

		// private static Logger logger = Logger.getLogger(TestModule.class);
	    TaskManager tm = null;
	    
	    //-------------------------------------------------------------------------
	    // Constructors
	    //-------------------------------------------------------------------------

	    /**
	     * Creates a new module instance.
	     */
	    public TestModule() {
	    	super(TestModule.MODULE_NAME);
	    }
	        
	    public void postInit(Configuration cfg) {
	    	tm = cfg.getBean(TaskManager.class);
	    	if (tm == null)
	    		throw new RuntimeException("TaskManager is not initialized");
	    }
	    
	    //-------------------------------------------------------------------------
	    // Web services
	    //-------------------------------------------------------------------------

	    public void addProcess(String projectId) {
	    	ProcessingTaskImpl task = new ProcessingTaskImpl(this.getTransformationId());
	    	
	    	// TODO: verify
	    	// e.setDescription(this.getTransformationId().toString());
	    	
	    	task.addParam("projectId", projectId);
	    	task.saveParams();
	    	
	    	if (tm == null)
	    		logger.error("TaskManager is not initialized");
	    	this.tm.addTask(task);
			logger.debug("process added");
	    	System.out.println("[" + projectId + "] Process added.");
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
				id = new URI(TestModule.MODULE_NAME);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return id;
		}

		@Override
		public void execute(ProcessingTask task) {
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
	    	
			processExecuted = true;
	    	System.out.println("[" + projectId + "] Task done.");
			logger.info("Task done.");
		}
	}

}
