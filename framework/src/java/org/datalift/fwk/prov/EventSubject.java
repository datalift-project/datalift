package org.datalift.fwk.prov;

import java.util.HashMap;
import java.util.Map;

public final class EventSubject {
    
    private String title;
    private String initial;
    private static Map<String, EventSubject> instances =
            new HashMap<String, EventSubject>();
    
    private EventSubject(String title, String initial) {
        this.title = title;
        this.initial = initial;
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
        if(!(o instanceof EventSubject))
            return false;
        if(this.title.toLowerCase().equals(((EventSubject) o).title.toLowerCase()))
            return true;
        return false;
    }
    
    public static EventSubject addNewInstance(String title, String initial){
        EventSubject ret;
        if(EventSubject.instances.containsKey(title)){
            ret = EventSubject.instances.get(title);
            ret.initial = initial;
        } else {
            ret = new EventSubject(title, initial);
            EventSubject.instances.put(title, ret);
        }
        return ret;
    }
    
    public static EventSubject getInstance(String title){
        return EventSubject.instances.get(title);
    }
}
