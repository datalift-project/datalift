package org.datalift.fwk.project;

public interface TaskManager {
	
	public void addTask(ProcessingTask task);
	public void start();
	
	/**
	 * Shutdown the task manager.
	 * 
	 * @param wait if true, stop is blocking until the task manager is stopped
	 *        or the timeout occurs.
	 * @param timeout is the waiting time in second until force stop. If 'wait'
	 *        is false, 'timeout' is not used.
	 * @return false if the timeout occurs, true for all other cases. 
	 * @throws InterruptedException 
	 */
	public boolean shutdown(boolean wait, long timeout) throws InterruptedException;
}
