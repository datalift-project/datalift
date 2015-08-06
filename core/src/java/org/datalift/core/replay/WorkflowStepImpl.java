package org.datalift.core.replay;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datalift.core.util.JsonStringParameters;
import org.datalift.fwk.replay.WorkflowStep;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WorkflowStepImpl implements WorkflowStep{

    private URI operation;
    private WorkflowStepImpl next = null;
    private List<WorkflowStep> previous = new ArrayList<WorkflowStep>();
    private JsonStringParameters param;
    
    public WorkflowStepImpl(String json) throws JSONException{
        this(null, new JSONObject(json));
    }
    
    private WorkflowStepImpl(WorkflowStepImpl next, JSONObject jobj) throws JSONException{
        this.setNextStep(next);
        this.operation = URI.create(jobj.getString("operation"));
        this.param = new JsonStringParameters(jobj.getString("parameters"));
        this.next = null;
        JSONArray jprevious = jobj.optJSONArray("previous");
        if(jprevious != null)
            for(int i = 0; i < jprevious.length(); i++)
                new WorkflowStepImpl(this, jprevious.getJSONObject(i));
    }
    
    public WorkflowStepImpl(URI operation, Map<String, String> parameters){
        this.operation = operation;
        this.param = new JsonStringParameters(parameters);
    }
    
    @Override
    public void setNextStep(WorkflowStep nextStep){
        WorkflowStepImpl next = (WorkflowStepImpl) nextStep;
        if(this.next != next){
            if(next == null){
                this.next.previous.remove(this);
            } else {
                next.previous.add(this);
                if(this.next != null)
                    this.next.previous.remove(this);
            }
            this.next = next;
        }
    }
    
    public String getJson(){
        return this.toJsonObject().toString();
    }
    
    private JSONObject toJsonObject(){
        try{
            JSONObject jobj = new JSONObject();
            jobj.put("operation", this.operation.toString());
            jobj.put("parameters", this.param.toString());
            if(!this.previous.isEmpty()){
                JSONArray jarr = new JSONArray();
                for(WorkflowStep p : this.previous)
                    jarr.put(((WorkflowStepImpl) p).toJsonObject());
                jobj.put("previous", jarr);
            }
            return jobj;
        } catch (Exception e){}
        return null;
    }
    
    @Override
    public String toString(){
        return this.getJson();
    }
    
    @Override
    public URI getOperation() {
        return this.operation;
    }

    @Override
    public List<WorkflowStep> getPreviousSteps() {
        return new ArrayList<WorkflowStep>(this.previous);
    }

    @Override
    public WorkflowStep getNextStep() {
        return this.next;
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<String, String>(this.param.getParametersMap());
    }

    
}
