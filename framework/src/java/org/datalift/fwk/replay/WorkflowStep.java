package org.datalift.fwk.replay;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface WorkflowStep {

    public URI getOperation();
    public List<WorkflowStep> getPreviousSteps();
    public WorkflowStep getNextStep();
    public Map<String, String> getParameters();
    public void setNextStep(WorkflowStep next);
}
