package org.datalift.fwk.replay;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * the interface for workflow steps
 * 
 * @author rcabaret
 *
 */
public interface WorkflowStep {
 
    /**
     * return the operation to execute during the step
     * 
     * @return  Operation URI
     */
    public URI getOperation();
    
    /**
     * return the list of all steps that must be executed before this one because it use there results
     * 
     * @return  the WorkflowSteps List
     */
    public List<WorkflowStep> getPreviousSteps();
    
    /**
     * return the collection of all steps that will use this one in the workflow
     * 
     * @return  the WorkflowStep Collection
     */
    public Collection<WorkflowStep> getNextSteps();
    
    /**
     * return the pattern values of parameters, it must be resolved before given it to the Operation
     * 
     * @return  the Map of parameters with the corresponding pattern
     */
    public Map<String, String> getParameters();
    
    /**
     * return the event from which the step was extracted
     * 
     * @return  the Event URI
     */
    public URI getOriginEvent();
    
    /**
     * add a step as previous to this one
     * 
     * @param next
     */
    public void addPreviousStep(WorkflowStep previous);
}
