package org.datalift.fwk.async;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public enum TaskStatus {
    runStatus(URI.create("http://www.datalift.org/core#runStatus"), "run"),
    abortStatus(URI.create("http://www.datalift.org/core#abortStatus"), "abort"),
    failStatus(URI.create("http://www.datalift.org/core#failStatus"), "fail"),
    newStatus(URI.create("http://www.datalift.org/core#newStatus"), "new"),
    doneStatus(URI.create("http://www.datalift.org/core#doneStatus"), "done");
    
    private URI uri;
    private String title;
    private static Map<String, TaskStatus> values = null;
    
    TaskStatus(URI uri, String title) {
        this.uri = uri;
        this.title = title;
    }

    public URI getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }
    
    public static TaskStatus getByUri(URI uri){
        if(TaskStatus.values == null){
            TaskStatus.values = new HashMap<String, TaskStatus>();
            for(TaskStatus v : TaskStatus.values())
                TaskStatus.values.put(v.uri.toString(), v);
        }
        return TaskStatus.values.get(uri.toString());
    }
}
