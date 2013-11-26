package org.datalift.core.project;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.TaskManager;
import org.datalift.fwk.project.TransformationModule;

public abstract class BaseTransformationModule extends BaseModule 
implements TransformationModule {

	protected BaseTransformationModule(String name) {
		super(name);
	}

//    /** {@inheritDoc} */
//    @Override
//	public URI getTransformationId() {
//    	return URI.create(this.getName());
//    }
    
    public TaskManager getTaskManager() {
    	TaskManager taskManager = 
    			Configuration.getDefault().getBean(TaskManager.class);
    	if (taskManager == null)
    		throw new RuntimeException("TaskManager is not initialized");
    	return taskManager;
    }

}
