package org.datalift.fwk.replay;

import java.net.URI;
import java.util.Map;

public interface Workflow {

    public URI getUri();
    public String getTitle();
    public void setTitle(String title);
    public String getDescription();
    public void setDescription(String description);
    public Map<String, String> getVariables();
    public void addVariable(String name, String defaultValue);
    public void removeAllVariables();
    public WorkflowStep getOutputStep();
    public void setSteps(WorkflowStep outputStep);
    public URI getOriginEvent();
    public void setOriginEvent(URI event);
    public void replay(Map<String, String> variables) throws Exception;
}
