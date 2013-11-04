package org.datalift.fwk.project;

public interface EventManager {
	public ProjectCreationEvent newProjectCreationEvent(User u, Project p);

	void setEventRunning(Event e);

	void setEventComplete(Event e);

	void setEventFailed(Event e);
}
