package org.datalift.core.async;

import java.net.URI;
import java.util.Map;

import org.datalift.core.prov.EventImpl;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.prov.Event;

/**
 * this abstract class is the base of all default implementations of the
 * TaskContext interface
 * 
 * @author rcabaret
 *
 */
public abstract class TaskContextBase extends TaskContext{

    /**
     * notify the beginning of an operation execution
     * 
     * @param project   the project associated with the execution
     * @param operation the operation executed
     * @param parameters    the parameters for the operation
     */
    public abstract void startOperation(Project project, URI operation,
            Map<String, String> parameters);
    
    /**
     * notify the end of the last operation executed and return the running
     * event if it exist
     * 
     * @param well  true if the execution finished normally
     * @return  the running Event or null if no event have been started during the operation execution
     */
    public abstract Event endOperation(boolean well);
    
    /**
     * a class which represent an operation execution
     * 
     * @author rcabaret
     *
     */
    protected class OperationExecution{
        protected Project project;
        protected EventImpl event = null;
        protected URI operation;
        protected Map<String, String> parameters;
        
        public OperationExecution(Project project, URI operation,
                Map<String, String> parameters){
            this.operation = operation;
            this.parameters = parameters;
            this.project = project;
        }
    }
}
