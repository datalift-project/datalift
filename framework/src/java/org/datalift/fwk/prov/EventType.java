package org.datalift.fwk.prov;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public final class EventType
{
    private static Map<URI, EventType> instances =
            new HashMap<URI, EventType>();

    private URI uri;
    private String title;
    private String initial;

    private EventType(URI uri, String title, String initial) {
        this.uri = uri;
        this.title = title;
        this.initial = initial;
    }

    public URI getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getInitial() {
        return initial;
    }

    @Override
    public boolean equals(Object o){
        if(o == null)
            return false;
        if(!(o instanceof EventType))
            return false;
        if(this.uri.equals(((EventType) o).uri))
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return this.uri.hashCode();
    }

    public static EventType addNewInstance(URI uri, String title, String initial){
        EventType ret;
        if(EventType.instances.containsKey(uri)){
            ret = EventType.instances.get(uri);
            ret.initial = initial;
            ret.title = title;
        } else {
            ret = new EventType(uri, title, initial);
            EventType.instances.put(uri, ret);
        }
        return ret;
    }

    public static EventType getInstance(URI uri){
        return EventType.instances.get(uri);
    }
}
