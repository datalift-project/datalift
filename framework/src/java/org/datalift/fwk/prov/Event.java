package org.datalift.fwk.prov;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.datalift.fwk.project.Project;

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
    
    public static final EventSubject SOURCE_EVENT_SUBJECT
            = EventSubject.addNewInstance("source", "S");
    public static final EventSubject PROJECT_EVENT_SUBJECT
            = EventSubject.addNewInstance("project", "P");
    public static final EventSubject ONTOLOGY_EVENT_SUBJECT
            = EventSubject.addNewInstance("ontology", "O");

    public URI getUri();
    public URI getOperation();
    public Map<String, Object> getParameters();
    public EventType getEventType();
    public Date getStartTime();
    public Date getEndTime();
    public URI getAgent();
    public URI getInfluenced();
    public Collection<URI> getUsed();
    public Project getProject();
    public URI getInformer();
}
