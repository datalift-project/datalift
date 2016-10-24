package org.datalift.core.async;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import org.datalift.fwk.async.Operation;
import org.datalift.fwk.async.Task;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.async.TaskStatus;
import org.datalift.fwk.prov.Event;

/**
 * an implementation of Task never persited for synchronous task execution
 * 
 * @author rcabaret
 *
 */
public class SynchroneTask implements Task{
    
    private TaskStatus status = TaskStatus.newStatus;
    private Date start = null;
    private Date end = null;
    private Date issue = new Date();
    private Operation operation;
    private Map<String, String> parameters;
    private Event runningEvent;

    /**
     * construct a SynchronousTask
     * 
     * @param operation the operation executed by the task
     * @param parameters    the parameters used by the operation
     */
    public SynchroneTask(Operation operation, Map<String, String> parameters){
        this.operation = operation;
        this.parameters = parameters;
    }
    
    /** {@inheritDoc} */
    @Override
    public URI getAgent() {
        return TaskContext.getCurrent().getCurrentAgent();
    }

    /** {@inheritDoc} */
    @Override
    public URI getUri() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public TaskStatus getStatus() {
        return this.status;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getParmeters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public Operation getOperation() {
        return this.operation;
    }

    /** {@inheritDoc} */
    @Override
    public Date getStartTime() {
        return this.start;
    }

    /** {@inheritDoc} */
    @Override
    public Date getEndTime() {
        return this.end;
    }

    /** {@inheritDoc} */
    @Override
    public Date getIssueTime() {
        return this.issue;
    }

    /** {@inheritDoc} */
    @Override
    public void setStatus(TaskStatus status) {
        TaskStatus s = this.status;
        if(s == status)
            return;
        if(s != TaskStatus.newStatus && status == TaskStatus.newStatus)
            throw new RuntimeException("a previous status cant be bring back");
        if(s == TaskStatus.failStatus && status != TaskStatus.failStatus)
            throw new RuntimeException("a previous status cant be bring back");
        if(s == TaskStatus.doneStatus && status != TaskStatus.doneStatus)
            throw new RuntimeException("a previous status cant be bring back");
        if(s == TaskStatus.abortStatus && status != TaskStatus.abortStatus)
            throw new RuntimeException("a previous status cant be bring back");
        if(s == TaskStatus.runStatus && status == TaskStatus.newStatus)
            throw new RuntimeException("a previous status cant be bring back");
        if(s == TaskStatus.runStatus && status == TaskStatus.abortStatus)
            throw new RuntimeException("a running task cant be abort");
        this.status = status;
        if(status == TaskStatus.doneStatus || status == TaskStatus.failStatus ||
                status == TaskStatus.abortStatus)
            this.end = new Date();
        if(status == TaskStatus.runStatus)
            this.start = new Date();
    }
    
    /** {@inheritDoc} */
    @Override
    public Event getRunningEvent() {
        return this.runningEvent;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setRunningEvent(Event event){
        this.runningEvent = event;
    }
}
