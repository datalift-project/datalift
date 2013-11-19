package org.datalift.core.project;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.TaskManager;

public class TaskManagerImpl implements TaskManager {

	//-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------
	
	LinkedBlockingQueue<Runnable> taskQueue = 
			new LinkedBlockingQueue<Runnable>();
	ExecutorService threadPoolExecutor = null;
	
	//-------------------------------------------------------------------------
    // TaskManager contract implementation
    //-------------------------------------------------------------------------
	
	/** {@inheritDoc} */
	@Override
	public void addTask(ProcessingTask task) {
		if (this.threadPoolExecutor == null)
			throw new RuntimeException("ThreadPoolExecutor is null. Be sure to call 'start' method");
		this.threadPoolExecutor.execute(task);
	}

	/** {@inheritDoc} */
	@Override
	public void start() {
		if (this.threadPoolExecutor != null)
			throw new RuntimeException("TaskManager is already running.");
		
		// TODO: put into config.
		int  corePoolSize  =    5;
		// Not used with a LinkedBlockingQueue
		int  maxPoolSize   =   10;
		long keepAliveTime = 5000;

		this.threadPoolExecutor =
				new ThreadPoolExecutor(
						corePoolSize,
		                maxPoolSize,
		                keepAliveTime,
		                TimeUnit.MILLISECONDS,
		                this.taskQueue
		                );
	}
	
	/** {@inheritDoc} 
	 * @throws InterruptedException */
	@Override
	public boolean shutdown(boolean wait, long timeout) throws InterruptedException {
		boolean ok = true;
		this.threadPoolExecutor.shutdown();
		if (wait)
			ok = this.threadPoolExecutor.awaitTermination(timeout, TimeUnit.SECONDS);
		this.threadPoolExecutor = null;

		return ok;
	}
}
