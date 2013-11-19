package org.datalift.fwk;

import java.net.URI;

import org.datalift.fwk.project.ProcessingTask;

public interface TransformationModule extends LifeCycle {
	public void execute(ProcessingTask task);
	public URI getTransformationId();
}
