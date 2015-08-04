package org.datalift.fwk.async;

import java.net.URI;

import org.datalift.fwk.prov.EventSubject;
import org.datalift.fwk.prov.EventType;

public abstract class TaskContext {
    
    private static final ThreadLocal<TaskContext> curent = new ThreadLocal<TaskContext>();
    protected static TaskContext defaultOne = null;

    protected TaskContext(){
        if(TaskContext.curent.get() == null)
            TaskContext.curent.set(this);
        else
            throw new RuntimeException("try to create two TaskContext on the same thread");
    }
    
    public abstract URI getCurrentAgent();
    public abstract URI getCurrentEvent();
    public abstract void beginAsEvent(EventType eventType, EventSubject eventSubject);
    public abstract void addUsedOnEvent(URI used);
    public abstract void addInfluencedEntityOnEvent(URI influenced);

    public static void setDefault(TaskContext taskContext){
        TaskContext.defaultOne = taskContext;
    }
    
    public static boolean isDefined(){
        return TaskContext.curent.get() != null;
    }
    
    public static void defineContext(TaskContext context){
        if(TaskContext.isDefined())
            throw new RuntimeException("Task Context already defined");
        else
            TaskContext.curent.set(context);
    }
    
    public static TaskContext getCurrent(){
        TaskContext tc = TaskContext.curent.get();
        if(tc == null){
            if(TaskContext.defaultOne == null)
                throw new RuntimeException("default Task Context undefined");
            else
                return TaskContext.defaultOne;
        } else {
            return tc;
        }
    }
}
