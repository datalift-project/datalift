package org.datalift.fwk.project;

public interface ProcessingTask extends Runnable, Event {

	public enum EventStatus {
		NEW,
		RUNNING,
		FAIL,
		COMPLETE
	}

	public String getTransformationId();
	public EventStatus getEventStatus();
	public void setEventStatus(EventStatus eventStatus);
	public void addParam(String name, Object param);
	public void saveParams();
	public void loadParams() throws Exception;
	public Object getParam(String name);

}
