package org.datalift.fwk.async;

import java.net.URI;
import java.util.Date;
import java.util.Map;

public interface Task{

    public URI getAgent();
    public URI getUri();
    public TaskStatus getStatus();
    public Map<String, String> getParmeters();
    public Operation getOperation();
    public Date getStartTime();
    public Date getEndTime();
    public Date getIssueTime();
    public void setStatus(TaskStatus status);
}
