package org.datalift.fwk.project;

import java.net.URI;

public interface ProcessingTask extends Runnable, Event {

	public enum EventStatus {
		NEW,
		RUNNING,
		FAIL,
		COMPLETE
	}

	public URI getTransformationId();
	public EventStatus getEventStatus();
	public void setEventStatus(EventStatus eventStatus);
	public void addParam(String name, Object param);
	public void saveParams();
	public void loadParams() throws Exception;
	public Object getParam(String name);

}
