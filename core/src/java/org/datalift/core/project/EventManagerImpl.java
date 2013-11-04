package org.datalift.core.project;

import java.util.Date;

import org.datalift.fwk.project.Event;
import org.datalift.fwk.project.EventManager;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectCreationEvent;
import org.datalift.fwk.project.User;

/**
 * EventManager implementation class used to manage events.
 * 
 * NOTE: We can maybe use the observer pattern to notify when an event is
 *       completed.
 * 
 * @author avalensi
 *
 */
public class EventManagerImpl implements EventManager {

    /** {@inheritDoc} */
	@Override
	public ProjectCreationEvent newProjectCreationEvent(User u, Project p) {
    	ProjectCreationEventImpl e = new ProjectCreationEventImpl();

    	//e.setId(eventURI);
		//e.setDescription("");
		//e.setParameter("none");
		//e.setEndedAtTime(new Date());
		e.setUsed(p);
		e.setWasAssociatedWith(u);
		e.setStartedAtTime(new Date());

		return e;
	}
	
    /** {@inheritDoc} */
	@Override
	public void setEventRunning(Event e) {
		
	}

    /** {@inheritDoc} */
	@Override
	public void setEventComplete(Event e) {
		e.setEndedAtTime(new Date());
	}

    /** {@inheritDoc} */
	@Override
	public void setEventFailed(Event e) {
		e.setEndedAtTime(new Date());
	}

}
