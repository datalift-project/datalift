package org.datalift.fwk.project;

import java.net.URI;

import org.datalift.fwk.LifeCycle;

public interface TransformationModule extends LifeCycle {
	public void execute(ProcessingTask task);
	public URI getTransformationId();
}
