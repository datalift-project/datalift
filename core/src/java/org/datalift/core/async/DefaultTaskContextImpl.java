package org.datalift.core.async;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.datalift.core.prov.EventImpl;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.prov.Event;
import org.datalift.fwk.prov.EventSubject;
import org.datalift.fwk.prov.EventType;
import org.datalift.fwk.security.SecurityContext;

public class DefaultTaskContextImpl extends TaskContext{

    private final ThreadLocal<ArrayList<OperationExecution>> threadExecutions =
            new ThreadLocal<ArrayList<OperationExecution>>();
    
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
    public Event getCurrentEvent() {
        ArrayList<OperationExecution> executions = this.threadExecutions.get();
        Event ev = null;
        if(executions != null)
            if(!executions.isEmpty()){
                int i = executions.size() - 2;
                ev = executions.get(i + 1).event;
                while(ev == null && i >= 0){
                    ev = executions.get(i).event;
                    i--;
                }
            }
        return ev;
    }

    @Override
    public Event beginAsEvent(EventType eventType, EventSubject eventSubject) {
        ArrayList<OperationExecution> executions = this.threadExecutions.get();
        if(executions == null || executions.isEmpty())
            return null;
        OperationExecution oe = executions.get(executions.size() - 1);
        ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        if(oe.event != null)
            return oe.event;
        URI operationE = oe.operation;
        if(oe.operation == null)
            operationE = URI
            .create("http://www.datalift.org/core/operation/DefaultTaskContextOperation");
        EventType eventTypeE = eventType;
        if(eventType == null)
            eventTypeE = Event.INFORMATION_EVENT_TYPE;
        Date startE = new Date();
        //build event uri
        StringBuilder str;
        if(oe.project == null)
            str = new StringBuilder("http://www.datalift.org/core");
        else
            str = new StringBuilder(oe.project.getUri());
        str.append("/event/").append(eventTypeE.getInitial());
        if(eventSubject != null)
            str.append(eventSubject.getInitial());
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        str.append("/").append(format.format(startE)).append("/");
        str.append(Double.toString(Math.random()).substring(2, 6));
        URI id = URI.create(str.toString());
        //create event and put it on the project
        EventImpl event = new EventImpl(id, oe.project, operationE, oe.parameters,
                eventTypeE, startE, null, this.getCurrentAgent(), null,
                TaskContext.getCurrent().getCurrentEvent().getUri());
        EventImpl evt = (EventImpl) pm.saveEvent(event);
        oe.event = evt;
        return evt;
    }

    @Override
    public void addUsedOnEvent(URI used) {
        ArrayList<OperationExecution> executions = this.threadExecutions.get();
        if(executions == null || executions.isEmpty())
            throw new RuntimeException("no current event to update");
        OperationExecution oe = executions.get(executions.size() - 1);
        if(oe == null || oe.event == null)
            throw new RuntimeException("no current event to update");
        oe.event.addUsed(used);
    }

    @Override
    public void addInfluencedEntityOnEvent(URI influenced) {
        ArrayList<OperationExecution> executions = this.threadExecutions.get();
        if(executions == null || executions.isEmpty())
            throw new RuntimeException("no current event to update");
        OperationExecution oe = executions.get(executions.size() - 1);
        if(oe == null || oe.event == null)
            throw new RuntimeException("no current event to update");
        oe.event.setInfluenced(influenced);
    }
    
    public void startOperation(Project project, URI operation,
            Map<String, String> parameters){
        ArrayList<OperationExecution> executions = this.threadExecutions.get();
        if(executions == null){
            executions = new ArrayList<OperationExecution>();
            this.threadExecutions.set(executions);
        }
        executions.add(new OperationExecution(project, operation, parameters));
    }
    
    public Event endOperation(boolean well){
        ArrayList<OperationExecution> executions = this.threadExecutions.get();
        if(executions == null || executions.isEmpty())
            throw new RuntimeException("no operation to end");
        OperationExecution oe = executions.get(executions.size() - 1);
        if(oe.event != null && well){
            ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
            pm.saveEvent(oe.event);
        }
        executions.remove(oe);
        return oe.event;
    }

    private class OperationExecution{
        private Project project;
        private EventImpl event = null;
        private URI operation;
        private Map<String, String> parameters;
        
        public OperationExecution(Project project, URI operation,
                Map<String, String> parameters){
            this.operation = operation;
            this.parameters = parameters;
            this.project = project;
        }
    }
}
