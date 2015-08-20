package org.datalift.core.async;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.async.Operation;
import org.datalift.fwk.async.Task;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.async.TaskManager;
import org.datalift.fwk.async.TaskStatus;
import org.datalift.fwk.async.UnregisteredOperationException;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.GenericRdfDao;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.prov.Event;

/**
 * the default implementation for the TaskManager interface
 * 
 * @author rcabaret
 *
 */
public class TaskManagerImpl implements TaskManager{
    
    private static final ThreadLocal<TaskExecution> execution =
            new ThreadLocal<TaskExecution>();
    private ExecutorService executor;
    private Map<String, Operation> operations = new HashMap<String, Operation>();
    private GenericRdfDao dao = null;
    private Map<Task, Future<?>> lockMap = new HashMap<Task, Future<?>>();

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime()
                .availableProcessors());
        Collection<Class<?>> cls = new ArrayList<Class<?>>();
        cls.add(TaskImpl.class);
        configuration.getBean(ProjectManager.class).addPersistentClasses(cls);
        TaskContext.setDefault(new DefaultTaskContextImpl());
    }

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        Collection<Operation> ops = configuration.getBeans(Operation.class);
        for(Operation op : ops){
            this.operations.put(op.getOperationId().toString(), op);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown(Configuration configuration) {
        this.executor.shutdownNow();
    }

    /** {@inheritDoc} */
    @Override
    public Task submit(Project project, URI operation,
            Map<String, String> parameters) throws UnregisteredOperationException {
        synchronized(this.lockMap){
            for(Task t : this.lockMap.keySet())
                if(this.lockMap.get(t).isDone())
                    this.lockMap.remove(t);
        }
        Operation op = this.operations.get(operation.toString());
        if(op == null)
            throw new UnregisteredOperationException(operation);
        if(TaskManagerImpl.execution.get() == null){
            if(this.dao == null)
                this.dao = Configuration.getDefault().getBean(ProjectManager.class)
                        .getRdfDao();
            //build task uri
            StringBuilder str;
            if(project == null)
                str = new StringBuilder("http://www.datalift.org/core");
            else
                str = new StringBuilder(project.getUri());
            str.append("/task/");
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.SSS'Z'");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            str.append(format.format(new Date())).append("/");
            str.append(Double.toString(Math.random()).substring(2, 6));
            //create and execute task
            TaskImpl task = new TaskImpl(URI.create(str.toString()), project, op,
                    parameters);
            synchronized(this.lockMap){
                this.lockMap.put(task,
                        this.executor.submit(new TaskExecution(project, task,
                                TaskContext.getCurrent().getCurrentEvent())));
            }
            return task;
        } else {
            ((TaskContextImpl) TaskContext.getCurrent())
                    .startOperation(project, operation, parameters);
            SynchroneTask task = new SynchroneTask(op, parameters);
            try {
                task.setStatus(TaskStatus.runStatus);
                op.execute(parameters);
            } catch (Exception e) {
                task.setStatus(TaskStatus.failStatus);
                ((TaskContextImpl) TaskContext.getCurrent()).endOperation(false);
                throw new RuntimeException("error during operation execution", e);
            }
            task.setStatus(TaskStatus.doneStatus);
            task.setRunningEvent(((TaskContextImpl) TaskContext.getCurrent())
                    .endOperation(true));
            return task;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public TaskStatus waitForEnding(Task task) {
        Future<?> f = null;
        synchronized(this.lockMap){
            f = this.lockMap.get(task);
        }
        if(f == null){
            return task.getStatus();
        } else {
            try {
                f.get();
            } catch (Exception e) {}
            return task.getStatus();
        }
    }

    //runable class to execute a task on a different thread
    private class TaskExecution implements Runnable{

        private TaskImpl task;
        private GenericRdfDao dao = TaskManagerImpl.this.dao;
        private Project project;
        private Event informer;
        
        TaskExecution(Project project, TaskImpl t, Event informer){
            this.dao.persist(t);
            this.task = this.dao.save(t);
            this.project = project;
            this.informer = informer;
        }
        
        @Override
        public void run() {
            TaskContext.defineContext(new TaskContextImpl(this.task, this.informer));
            ((TaskContextImpl) TaskContext.getCurrent()).startOperation(
                    this.project,
                    this.task.getOperation().getOperationId(),
                    this.task.getParmeters());
            this.task.setStatus(TaskStatus.runStatus);
            this.dao.save(this.task);
            try {
                this.task.getOperation().execute(this.task.getParmeters());
                this.task.setStatus(TaskStatus.doneStatus);
                this.dao.delete(this.task);
                this.task.setRunningEvent(((TaskContextImpl) TaskContext
                        .getCurrent()).endOperation(true));
            } catch (Exception e) {
                this.task.setStatus(TaskStatus.failStatus);
                this.dao.save(this.task);
                ((TaskContextImpl) TaskContext.getCurrent()).endOperation(false);
                Logger.getLogger().error("the task fail", e);
                throw new RuntimeException("error during task execution" + e);
            }
        }
    }
}
