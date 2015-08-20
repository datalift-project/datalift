package org.datalift.fwk.replay;

import java.net.URI;
import java.util.Map;

/**
 * interface for workflows
 * 
 * @author rcabaret
 *
 */
public interface Workflow {

    /**
     * return the workflow uri
     * 
     * @return  the URI
     */
    public URI getUri();
    
    /**
     * return the title of the workflow
     * 
     * @return  the title
     */
    public String getTitle();
    
    /**
     * set the title of the workflow
     * 
     * @param title the new title
     */
    public void setTitle(String title);
    
    /**
     * return the workflow description
     * 
     * @return  the description
     */
    public String getDescription();
    
    /**
     * set the workflow description
     * 
     * @param description   the description
     */
    public void setDescription(String description);
    
    /**
     * return the map of all variables associated with it default value
     * 
     * @return  the map of variables and there default value
     */
    public Map<String, String> getVariables();
    
    /**
     * add a variable to the workflow
     * 
     * @param name  the name of the variable
     * @param defaultValue  the default value of the variable, it must be different than null
     */
    public void addVariable(String name, String defaultValue);
    
    /**
     * remove all variables of the workflow
     */
    public void removeAllVariables();
    
    /**
     * return the last step of the workflow
     * 
     * @return  the WorkflowStep
     */
    public WorkflowStep getOutputStep();
    
    /**
     * set the steps graph by the last step
     * the given step must be the only one ending step
     * 
     * @param outputStep    the WorkflowStep
     */
    public void setSteps(WorkflowStep outputStep);
    
    /**
     * return the event from where the workflow where extracted
     * 
     * @return  the Event URI
     */
    public URI getOriginEvent();
    
    /**
     * execute the workflow with the given variables
     * 
     * @param variables the map of variables and it values
     * @throws Exception    exception that replay and steps execution throws
     */
    public void replay(Map<String, String> variables) throws Exception;
}
