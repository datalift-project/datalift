package org.datalift.core.async;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.persistence.Entity;

import org.datalift.core.project.BaseRdfEntity;
import org.datalift.core.util.JsonStringMap;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.async.Operation;
import org.datalift.fwk.async.Task;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.async.TaskStatus;
import org.datalift.fwk.async.UnregisteredOperationException;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.prov.Event;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

/**
 * an implementation of Task to execute on an own thread and to persist with
 * Empire
 * 
 * @author rcabaret
 *
 */
@Entity
@RdfsClass("datalift:Task")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="http://www.datalift.org/core/project-history")
public class TaskImpl extends BaseRdfEntity implements Task{
    
    @RdfId
    private URI uri;
    @RdfProperty("datalift:operation")
    private URI operationId;
    private Operation operation;
    @RdfProperty("datalift:parameters")
    private String parameters;
    private JsonStringMap param;
    @RdfProperty("prov:wasAssociatedWith")
    private URI agent;
    @RdfProperty("datalift:status")
    private URI status;
    @RdfProperty("prov:startedAtTime")
    private Date start;
    @RdfProperty("prov:endedAtTime")
    private Date end;
    @RdfProperty("dcterms:issued")
    private Date issue;
    private Event runningEvent;
    
    /**
     * construct an empty TaskImpl
     */
    public TaskImpl(){
        //NOP
    }
    
    /**
     * construct a TaskImpl
     * 
     * @param uri   the uri of the task
     * @param project   the project associated with the task
     * @param operation the operation executed by the task
     * @param parameters    the parameters used by the operation
     */
    public TaskImpl(URI uri, Project project, Operation operation,
            Map<String, String> parameters){
        if(parameters != null){
            this.param = new JsonStringMap(parameters);
        } else {
            this.param = null;
            this.parameters = null;
        }
        this.operation = operation;
        this.operationId = operation.getOperationId();
        this.agent = TaskContext.getCurrent().getCurrentAgent();
        this.status = TaskStatus.newStatus.getUri();
        this.issue = new Date();
        this.uri = uri;
    }

    /** {@inheritDoc} */
    @Override
    public URI getAgent() {
        return this.agent;
    }

    /** {@inheritDoc} */
    @Override
    public URI getUri() {
        return this.uri;
    }

    /** {@inheritDoc} */
    @Override
    public TaskStatus getStatus() {
        return TaskStatus.getByUri(this.status);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getParmeters() {
        if(this.param == null && this.parameters != null){
            this.param = new JsonStringMap(this.parameters);
            return this.param;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Operation getOperation() {
        if(this.operation == null){
            Collection<Operation> ops = Configuration.getDefault()
                    .getBeans(Operation.class);
            for(Operation o : ops){
                if(o.getOperationId().equals(this.operationId)){
                    this.operation = o;
                    break;
                }
            }
            if(this.operation == null)
                throw new RuntimeException(
                        new UnregisteredOperationException(this.operationId));
        }
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
    protected void setId(String id) {
        this.uri = URI.create(id);
    }

    /** {@inheritDoc} */
    @Override
    public void setStatus(TaskStatus status) {
        TaskStatus s = TaskStatus.getByUri(this.status);
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
        this.status = status.getUri();
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
