package org.datalift.core.async;

import java.net.URI;

import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.prov.EventSubject;
import org.datalift.fwk.prov.EventType;
import org.datalift.fwk.security.SecurityContext;

public class DefaultTaskContextImpl extends TaskContext{

    @Override
    public URI getCurrentAgent() {
        URI ret;
        try{
            if(SecurityContext.getUserPrincipal() != null)
                ret = URI.create("http://www.datalift.org/core/agent/" +
                        SecurityContext.getUserPrincipal());
            else
                ret = URI.create("http://www.datalift.org/core/agent/Unknow");
        } catch (Exception e){
            ret = URI.create("http://www.datalift.org/core/agent/Unknow");
        }
        return ret;
    }

    @Override
    public URI getCurrentEvent() {
        return null;
    }

    @Override
    public void beginAsEvent(EventType eventType, EventSubject eventSubject) {
        throw new RuntimeException("cant create an event on the main thread");
    }

    @Override
    public void addUsedOnEvent(URI used) {
        throw new RuntimeException("no current event to update");
    }

    @Override
    public void addInfluencedEntityOnEvent(URI influenced) {
        throw new RuntimeException("no current event to update");
    }

    
}
