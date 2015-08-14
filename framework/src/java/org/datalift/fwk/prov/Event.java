package org.datalift.fwk.prov;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.datalift.fwk.project.Project;

/**
 * interface for events that trace every operation executions
 * 
 * @author rcabaret
 *
 */
public interface Event {
    
    public static final EventType CREATION_EVENT_TYPE
            = EventType.addNewInstance(
            URI.create("http://www.datalift.org/core#eventTypeCreation"),
            "creation", "C");
    public static final EventType UPDATE_EVENT_TYPE
            = EventType.addNewInstance(
            URI.create("http://www.datalift.org/core#eventTypeUpdate"),
            "Update", "U");
    public static final EventType DESTRUCTION_EVENT_TYPE
            = EventType.addNewInstance(
            URI.create("http://www.datalift.org/core#eventTypeDestruction"),
            "destruction", "D");
    public static final EventType INFORMATION_EVENT_TYPE
            = EventType.addNewInstance(
            URI.create("http://www.datalift.org/core#eventTypeInformation"),
            "information", "I");
    public static final EventType OUTPUT_EVENT_TYPE
            = EventType.addNewInstance(
            URI.create("http://www.datalift.org/core#eventTypeOutput"),
            "output", "O");
    public static final EventType REPLAY_EVENT_TYPE
            = EventType.addNewInstance(
            URI.create("http://www.datalift.org/core#eventTypeReplay"),
            "replay", "R");
    
    public static final EventSubject SOURCE_EVENT_SUBJECT
            = EventSubject.addNewInstance("source", "S");
    public static final EventSubject PROJECT_EVENT_SUBJECT
            = EventSubject.addNewInstance("project", "P");
    public static final EventSubject ONTOLOGY_EVENT_SUBJECT
            = EventSubject.addNewInstance("ontology", "O");
    public static final EventSubject WORKFLOW_EVENT_SUBJECT
            = EventSubject.addNewInstance("workflow", "W");

    /**
     * return the uri of the event
     * 
     * @return  the event uri
     */
    public URI getUri();
    
    /**
     * return the operation executed during the event
     * 
     * @return  the operation uri
     */
    public URI getOperation();
    
    /**
     * return the parameters used by the operation execution
     * 
     * @return  the map of the parameters
     */
    public Map<String, String> getParameters();
    
    /**
     * return the type of the event
     * 
     * @return  the EventType
     */
    public EventType getEventType();
    
    /**
     * return the time the event begin
     * 
     * @return  the date of the beginning
     */
    public Date getStartTime();
    
    /**
     * return the time the event end
     * 
     * @return  the date of the ending
     */
    public Date getEndTime();
    
    /**
     * return the agent who ordered the event
     * 
     * @return  the agent uri
     */
    public URI getAgent();
    
    /**
     * return the influenced entity
     * 
     * @return  the entity uri
     */
    public URI getInfluenced();
    
    /**
     * return all entity used by the operation during the event
     * 
     * @return  the entity uri collection
     */
    public Collection<URI> getUsed();
    
    /**
     * return the project the event is associated with
     * 
     * @return  the project
     */
    public Project getProject();
    
    /**
     * return the event which triggered this one
     * 
     * @return  the informer event uri
     */
    public URI getInformer();
}
