package org.datalift.fwk.async;

import java.net.URI;
import java.util.Map;

import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.project.Project;

public interface TaskManager extends LifeCycle{

    public Task submit(Project project, URI operation,
            Map<String, Object> parameters) throws UnregisteredOperationException;
}
