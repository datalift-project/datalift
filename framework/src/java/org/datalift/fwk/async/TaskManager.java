package org.datalift.fwk.async;

import java.net.URI;
import java.util.Map;

import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.project.Project;

/**
 * the interface for manage tasks executions
 * 
 * @author rcabaret
 *
 */
public interface TaskManager extends LifeCycle{

    /**
     * execute a task as soon as possible
     * 
     * @param project   the project associated to the task
     * @param operation the operation to execute
     * @param parameters    the parameters to give to the operation
     * 
     * @return  the task which is associated with the execution
     * 
     * @throws UnregisteredOperationException   if the operation uri match with no Operation registered instance
     */
    public Task submit(Project project, URI operation,
            Map<String, String> parameters) throws UnregisteredOperationException;
    
    /**
     * wait for the task execution termination (fail, done or abort)
     * 
     * @param task  the task to wait for the end
     * @return  the status of the task
     */
    public TaskStatus waitForEnding(Task task);
}
