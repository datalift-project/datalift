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
import org.datalift.fwk.async.Task;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.prov.Event;
import org.datalift.fwk.prov.EventSubject;
import org.datalift.fwk.prov.EventType;

public class TaskContextImpl extends TaskContext{

    private URI taskAgent;
    private ArrayList<OperationExecution> executions = new ArrayList<OperationExecution>();
    private Event informer;
    
    public TaskContextImpl(Task task, Event informer){
        super();
        this.taskAgent = URI.create(task.getUri().toString() + "/softwareAgent");
        this.informer = informer;
    }
    
    public void startOperation(Project project, URI operation,
            Map<String, String> parameters){
        this.executions.add(new OperationExecution(project, operation, parameters));
    }
    
    public Event endOperation(boolean well){
        OperationExecution oe = this.executions.get(this.executions.size() - 1);
        if(oe.event != null && well){
            ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
            pm.saveEvent(oe.event);
        }
        this.executions.remove(oe);
        return oe.event;
    }
    
    @Override
    public URI getCurrentAgent() {
        Event event = this.getCurrentEvent();
        if(event == null){
            if(TaskContext.defaultOne == null)
                throw new RuntimeException("default Task Context undefined");
            return TaskContext.defaultOne.getCurrentAgent();
        } else {
            return this.taskAgent;
        }
    }

    @Override
    public Event getCurrentEvent() {
        Event ev = this.informer;
        if(!this.executions.isEmpty()){
            int i = this.executions.size() - 2;
            ev = this.executions.get(i + 1).event;
            while(ev == null && i >= 0){
                ev = this.executions.get(i).event;
                i--;
            }
        }
        return ev;
    }

    @Override
    public Event beginAsEvent(EventType eventType, EventSubject eventSubject) {
        OperationExecution oe = this.executions.get(this.executions.size() - 1);
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
        URI informerE = null;
        if(TaskContext.getCurrent().getCurrentEvent() != null)
            informerE = TaskContext.getCurrent().getCurrentEvent().getUri();
        //create event and put it on the project
        EventImpl event = new EventImpl(id, oe.project, operationE, oe.parameters,
                eventTypeE, startE, null, this.getCurrentAgent(), null,
                informerE);
        EventImpl evt = (EventImpl) pm.saveEvent(event);
        oe.event = evt;
        return evt;
    }
    
    @Override
    public void addUsedOnEvent(URI used) {
        if(this.executions.isEmpty())
            throw new RuntimeException("no current event to update");
        OperationExecution oe = this.executions.get(this.executions.size() - 1);
        if(oe == null)
            throw new RuntimeException("no current event to update");
        oe.event.addUsed(used);
    }

    @Override
    public void addInfluencedEntityOnEvent(URI influenced) {
        if(this.executions.isEmpty())
            throw new RuntimeException("no current event to update");
        OperationExecution oe = this.executions.get(this.executions.size() - 1);
        if(oe == null)
            throw new RuntimeException("no current event to update");
        oe.event.setInfluenced(influenced);
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
