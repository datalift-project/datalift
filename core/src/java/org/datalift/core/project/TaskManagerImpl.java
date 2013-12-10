/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *                  A. Valensi
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.core.project;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.TaskManager;
import org.datalift.fwk.project.ProcessingTask.EventStatus;

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

		task.setEventStatus(EventStatus.NEW);
		ProjectManager pm = 
				Configuration.getDefault().getBean(ProjectManager.class);
		pm.saveEvent(task);
		
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
