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

import org.datalift.fwk.Configuration;
import org.datalift.fwk.async.Operation;
import org.datalift.fwk.async.Task;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.async.TaskManager;
import org.datalift.fwk.async.TaskStatus;
import org.datalift.fwk.async.UnregisteredOperationException;
import org.datalift.fwk.project.GenericRdfDao;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;

public class TaskManagerImpl implements TaskManager{
    
    private static final ThreadLocal<TaskExecution> execution =
            new ThreadLocal<TaskExecution>();
    private ExecutorService executor;
    private Map<String, Operation> operations = new HashMap<String, Operation>();
    private GenericRdfDao dao = null;

    @Override
    public void init(Configuration configuration) {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime()
                .availableProcessors());
        Collection<Class<?>> cls = new ArrayList<Class<?>>();
        cls.add(TaskImpl.class);
        configuration.getBean(ProjectManager.class).addPersistentClasses(cls);
        TaskContext.setDefault(new DefaultTaskContextImpl());
    }

    @Override
    public void postInit(Configuration configuration) {
        Collection<Operation> ops = configuration.getBeans(Operation.class);
        for(Operation op : ops){
            this.operations.put(op.getOperationId().toString(), op);
        }
    }

    @Override
    public void shutdown(Configuration configuration) {
        this.executor.shutdownNow();
    }

    @Override
    public Task submit(Project project, URI operation,
            Map<String, String> parameters) throws UnregisteredOperationException {
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
            Task task = new TaskImpl(URI.create(str.toString()), project, op,
                    parameters);
            this.executor.execute(new TaskExecution(project, task));
            return task;
        } else {
            ((TaskContextImpl) TaskContext.getCurrent())
                    .startOperation(project, operation, parameters);
            try {
                op.execute(parameters);
            } catch (Exception e) {
                throw new RuntimeException("error during operation execution", e);
            }
            ((TaskContextImpl) TaskContext.getCurrent()).endOperation();
            return TaskManagerImpl.execution.get().getTask();
        }
    }

    private class TaskExecution implements Runnable{

        private Task task;
        private GenericRdfDao dao = TaskManagerImpl.this.dao;
        private Project project;
        
        TaskExecution(Project project, Task t){
            this.dao.persist(t);
            this.task = this.dao.save(t);
            this.project = project;
        }
        
        @Override
        public void run() {
            TaskContext.defineContext(new TaskContextImpl(this.task));
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
                ((TaskContextImpl) TaskContext.getCurrent()).endOperation();
            } catch (Exception e) {
                this.task.setStatus(TaskStatus.failStatus);
                this.dao.save(this.task);
                ((TaskContextImpl) TaskContext.getCurrent()).endOperation();
                throw new RuntimeException("error during task execution" + e);
            }
        }
        
        public Task getTask(){
            return this.task;
        }
    }
}
