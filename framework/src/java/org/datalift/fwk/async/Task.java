package org.datalift.fwk.async;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import org.datalift.fwk.prov.Event;

/**
 * the interface for tasks which represent an operation execution.
 * unlike events which are footprints of executions, tasks just contains
 * informations for the execution and disappear after related executions
 * 
 * @author rcabaret
 *
 */
public interface Task{

    /**
     * return the agent who triggered the task
     * 
     * @return  the agent uri
     */
    public URI getAgent();
    
    /**
     * return the uri of the task
     * 
     * @return  the uri
     */
    public URI getUri();
    
    /**
     * return the status of the task
     * 
     * @return  the taskStatus
     */
    public TaskStatus getStatus();
    
    /**
     * return parameters used by the operation as a map of Strings
     * 
     * @return  the parameters map
     */
    public Map<String, String> getParmeters();
    
    /**
     * return the operation executed by the task
     * 
     * @return  the Operation
     */
    public Operation getOperation();
    
    /**
     * return the time of the task execution beginning
     * 
     * @return  the begin Date or null if the task execution is not started
     */
    public Date getStartTime();
    
    /**
     * return the time of the task execution ending
     * 
     * @return  the end Date or null if the task execution is not done
     */
    public Date getEndTime();
    
    /**
     * return the time of the task creation
     * 
     * @return  the creation Date
     */
    public Date getIssueTime();
    
    /**
     * redefine the task status
     * the dates of task life steps must be updated
     * 
     * @param status    the new status
     */
    public void setStatus(TaskStatus status);
    
    /**
     * return the event which (will if the task is not done) trace the Task
     * execution if it exist
     * 
     * @return  the event or null if not exist
     */
    public Event getRunningEvent();
    
    /**
     * set the event which will trace the task
     * 
     * @param event the event
     */
    public void setRunningEvent(Event event);
}
