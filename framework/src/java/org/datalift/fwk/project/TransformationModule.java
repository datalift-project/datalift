package org.datalift.fwk.project;

import java.net.URI;

import org.datalift.fwk.LifeCycle;

public interface TransformationModule extends LifeCycle {
	public void execute(ProcessingTask task);
	public URI getTransformationId();
	
	/**
	 * Get the task manager used to execute asynchronous tasks.
	 * 
	 * @return the task manager or null if it is not init. task manager
	 *         should be init in the postInit method.
	 */
	public TaskManager getTaskManager();
}
