package org.datalift.fwk.project;

import org.datalift.fwk.LifeCycle;

public interface TaskManager extends LifeCycle {
	
	public void addTask(ProcessingTask task);

}
