package org.datalift.fwk.replay;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WorkflowStep {

    public URI getOperation();
    public List<WorkflowStep> getPreviousSteps();
    public Collection<WorkflowStep> getNextSteps();
    public Map<String, String> getParameters();
    public URI getOriginEvent();
    public void addPreviousStep(WorkflowStep next);
}
