package org.datalift.core.project;

import java.util.Date;

import org.datalift.fwk.project.Event;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.User;

import com.clarkparsia.empire.annotation.RdfProperty;

public class EventImpl implements Event {
	
	//-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:eventStatus")
	private EventStatus eventStatus;
    @RdfProperty("dcterms:description")
    private String description;
    @RdfProperty("datalift:parameter")
    private String parameter;
    @RdfProperty("prov:startedAtTime")
    private Date startedAtTime;
    @RdfProperty("prov:endedAtTime")
    private Date endedAtTime;
    @RdfProperty("prov:wasAssociatedWith")
    private User wasAssociatedWith;
    @RdfProperty("prov:used")
    private Source used;
    @RdfProperty("prov:wasInformedBy")
    private Event wasInformedBy;

    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    public EventStatus getEventStatus() {
		return eventStatus;
	}
	public void setEventStatus(EventStatus eventStatus) {
		this.eventStatus = eventStatus;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getParameter() {
		return parameter;
	}
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	public Date getStartedAtTime() {
		return startedAtTime;
	}
	public void setStartedAtTime(Date startedAtTime) {
		this.startedAtTime = startedAtTime;
	}
	public Date getEndedAtTime() {
		return endedAtTime;
	}
	public void setEndedAtTime(Date endedAtTime) {
		this.endedAtTime = endedAtTime;
	}
	public User getWasAssociatedWith() {
		return wasAssociatedWith;
	}
	public void setWasAssociatedWith(User wasAssociatedWith) {
		this.wasAssociatedWith = wasAssociatedWith;
	}
	public Source getUsed() {
		return used;
	}
	public void setUsed(Source used) {
		this.used = used;
	}
	public Event getWasInformedBy() {
		return wasInformedBy;
	}
	public void setWasInformedBy(Event wasInformedBy) {
		this.wasInformedBy = wasInformedBy;
	}
    
}
