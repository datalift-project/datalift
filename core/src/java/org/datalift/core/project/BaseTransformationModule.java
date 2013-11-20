package org.datalift.core.project;

import java.net.URI;
import java.net.URISyntaxException;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.TaskManager;
import org.datalift.fwk.project.TransformationModule;

public abstract class BaseTransformationModule extends BaseModule implements TransformationModule {

	protected BaseTransformationModule(String name) {
		super(name);
	}

	private TaskManager taskManager = null;

	/** {@inheritDoc} */
    @Override
    public void postInit(Configuration cfg) {
    	taskManager = cfg.getBean(TaskManager.class);
    	if (taskManager == null)
    		throw new RuntimeException("TaskManager is not initialized");
    }

	/** {@inheritDoc} */
    @Override
	public TaskManager getTaskManager() {
    	return this.taskManager;
	}
    
	/** {@inheritDoc} */
    @Override
	public URI getTransformationId() {
    	try {
			return new URI(this.getName());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

}
