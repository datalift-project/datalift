package org.datalift.fwk.project;

import org.datalift.fwk.LifeCycle;

public interface TransformationModule extends LifeCycle {
	
	/**
	 * This method is used by the thread pool to do the process of a module
	 * asynchronously.
	 * @param task   it is the task with the parameters inside
	 * @return       true if succeed or false if not.
	 */
	public Boolean execute(ProcessingTask task);
	public String getTransformationId();
}
