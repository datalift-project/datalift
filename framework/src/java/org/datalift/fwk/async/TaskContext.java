package org.datalift.fwk.async;

import java.net.URI;

import org.datalift.fwk.prov.Event;
import org.datalift.fwk.prov.EventSubject;
import org.datalift.fwk.prov.EventType;

/**
 * this interface provide services to implementations of Operation interface
 * 
 * @author rcabaret
 *
 */
public abstract class TaskContext {
    
    private static final ThreadLocal<TaskContext> curent = new ThreadLocal<TaskContext>();
    protected static TaskContext defaultOne = null;
    
    /**
     * return the current agent who triggered the current operation
     * 
     * @return the agent URI
     */
    public abstract URI getCurrentAgent();
    
    /**
     * return the current running event, this event is associated with the
     * nearest operation which call the method beginAsEvent
     * 
     * @return the Event
     */
    public abstract Event getCurrentEvent();
    
    /**
     * create an event for the currently running Operation and return it, this
     * event will be automatically ended and persisted after the end of the
     * operation execution
     * 
     * @param eventType the type of the event
     * @param eventSubject  the subject of the event
     * @return  the created Event
     */
    public abstract Event beginAsEvent(EventType eventType, EventSubject eventSubject);
    
    /**
     * add a new used entity to the running Event
     * 
     * @param used  the URI of the entity to add
     */
    public abstract void addUsedOnEvent(URI used);
    
    /**
     * set the influenced entity of the running event
     * 
     * @param influenced    the URI of the entity
     */
    public abstract void addInfluencedEntityOnEvent(URI influenced);

    /**
     * set the TaskContext instance that will be returned if no context is
     * defined for the thread
     * 
     * @param taskContext   the TaskContext instance
     */
    public static void setDefault(TaskContext taskContext){
        TaskContext.defaultOne = taskContext;
    }
    
    /**
     * return true if there is a TaskContext is defined for the current thread
     * 
     * @return
     */
    public static boolean isDefined(){
        return TaskContext.curent.get() != null;
    }
    
    /**
     * define a TaskContext instance for the current thread
     * 
     * @param context   the TaskContext
     */
    public static void defineContext(TaskContext context){
        if(TaskContext.isDefined())
            throw new RuntimeException("Task Context already defined");
        else
            TaskContext.curent.set(context);
    }
    
    /**
     * return the TaskContext associated with the current thread or the default
     * one if it is not defined
     * 
     * @return  the TaskContext
     */
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
