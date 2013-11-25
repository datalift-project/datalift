package org.datalift.core.project;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.TaskManager;

// TODO: add properties in skel. properties file
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
			throw new RuntimeException(
					"ThreadPoolExecutor is null. Be sure to call 'start' method");
		this.threadPoolExecutor.execute(task);
	}

	/** {@inheritDoc} */
	@Override
	public void init(Configuration configuration) {
		if (this.threadPoolExecutor != null)
			throw new RuntimeException("TaskManager is already running.");
		
		int  corePoolSize = Integer.parseInt(
				configuration.getProperty("task.manager.corepoolsize", "5"));
		// Not used with a LinkedBlockingQueue
		int  maxPoolSize = Integer.parseInt(
				configuration.getProperty("task.manager.maxpoolsize", "10")); 
		long keepAliveTime = Integer.parseInt(
				configuration.getProperty("task.manager.keepalivetime", "5000"));

		this.threadPoolExecutor =
				new ThreadPoolExecutor(
						corePoolSize,
		                maxPoolSize,
		                keepAliveTime,
		                TimeUnit.MILLISECONDS,
		                this.taskQueue
		                );
	}

	@Override
	public void postInit(Configuration configuration) {
		// NOP
	}

	/** {@inheritDoc} */
	@Override
	public void shutdown(Configuration configuration) {
		long timeout = Integer.parseInt(
				configuration.getProperty("task.manager.shutdown.timeout", "15"));
		this.threadPoolExecutor.shutdown();
		if (timeout > 0) {
			try {
				this.threadPoolExecutor.awaitTermination(
						timeout, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		this.threadPoolExecutor = null;
	}
}
