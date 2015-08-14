package org.datalift.core.replay;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datalift.core.util.JsonStringMap;
import org.datalift.fwk.replay.WorkflowStep;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WorkflowStepImpl implements WorkflowStep{

    private URI originEvent;
    private URI operation;
    private Collection<WorkflowStepImpl> nexts = new ArrayList<WorkflowStepImpl>();
    private List<WorkflowStep> previous = new ArrayList<WorkflowStep>();
    private JsonStringMap param;
    
    public WorkflowStepImpl(String json, URI eventOfThis) throws JSONException{
        Map<String, WorkflowStep> steps = new HashMap<String, WorkflowStep>();
        JSONObject jobj = new JSONObject(json);
        // extract all steps
        for(String e : JSONObject.getNames(jobj)){
            if(!e.equals(eventOfThis.toString())){
                JSONObject jstep = jobj.getJSONObject(e);
                WorkflowStep step = new WorkflowStepImpl(
                        URI.create(jstep.getString("operation")),
                        new JsonStringMap(jstep.getJSONObject("parameters")),
                        URI.create(e));
                steps.put(e, step);
            } else {
                JSONObject jstep = jobj.getJSONObject(e);
                this.operation = URI.create(jstep.getString("operation"));
                this.originEvent = URI.create(e);
                this.param = new JsonStringMap(jstep.getJSONObject("parameters"));
                steps.put(e, this);
            }
        }
        // link steps
        for(String e : steps.keySet()){
            WorkflowStep step = steps.get(e);
            JSONObject jstep = jobj.getJSONObject(e);
            JSONArray prevs = jstep.optJSONArray("previous");
            if(prevs != null && prevs.length() > 0){
                for(int i = prevs.length() - 1; i >= 0; i--){
                    step.addPreviousStep(steps.get(prevs.getString(i)));
                }
            }
        }
    }
    
    public WorkflowStepImpl(URI operation, Map<String, String> parameters,
            URI originEvent){
        this.operation = operation;
        this.param = new JsonStringMap(parameters);
        this.originEvent = originEvent;
    }
    
    @Override
    public void addPreviousStep(WorkflowStep nextStep){
        Map<URI, WorkflowStepImpl> controled;
        List<WorkflowStepImpl> toControl;
        WorkflowStepImpl step = (WorkflowStepImpl) nextStep;
        // control itself
        if(nextStep.getOriginEvent().equals(this.originEvent))
            throw new RuntimeException("a step cant be itself previous");
        // control duplicate steps
        controled = new HashMap<URI, WorkflowStepImpl>();
        toControl = new ArrayList<WorkflowStepImpl>();
        toControl.add(this);
        toControl.add(step);
        while(!toControl.isEmpty()){
            WorkflowStepImpl stc = toControl.get(0);
            if(controled.containsKey(stc.originEvent)){
                if(controled.get(stc.originEvent) != stc)
                    throw new RuntimeException(
                            "tow steps cant have the same origin event");
            } else {
                controled.put(stc.originEvent, stc);
                toControl.addAll(stc.nexts);
                for(WorkflowStep s : stc.previous)
                    toControl.add((WorkflowStepImpl) s);
            }
            toControl.remove(0);
        }
        // control loops
        controled = new HashMap<URI, WorkflowStepImpl>();
        toControl = new ArrayList<WorkflowStepImpl>();
        toControl.addAll(this.nexts);
        while(!toControl.isEmpty()){
            WorkflowStepImpl stc = toControl.get(0);
            if(!controled.containsKey(stc.originEvent)){
                if(step.originEvent.equals(stc.originEvent))
                    throw new RuntimeException(
                            "the step is already on the workflow : " +
                            stc.originEvent.toString());
                controled.put(stc.originEvent, stc);
                toControl.remove(0);
                for(WorkflowStepImpl n : stc.nexts)
                    toControl.add(n);
            } else {
                toControl.remove(0);
            }
        }
        // add as previous
        this.previous.add(step);
        step.nexts.add(this);
    }
    
    public String getJson(){
        return WorkflowStepImpl.genericStepToJsonObject(this, null).toString();
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
    public Collection<WorkflowStep> getNextSteps() {
        return new ArrayList<WorkflowStep>(this.nexts);
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<String, String>(this.param);
    }

    @Override
    public boolean equals(Object o){
        if(o == null)
            return false;
        if(!(o instanceof WorkflowStep))
            return false;
        WorkflowStep ths = this;
        WorkflowStep oth = (WorkflowStep) o;
        while(ths.getNextSteps() != null || !ths.getNextSteps().isEmpty())
            ths = ths.getNextSteps().iterator().next();
        while(oth.getNextSteps() != null || !oth.getNextSteps().isEmpty())
            oth = oth.getNextSteps().iterator().next();
        return WorkflowStepImpl.genericStepToJsonObject(oth, null).toString()
                .equals(WorkflowStepImpl.genericStepToJsonObject(ths, null));
    }
    
    private static JSONObject genericStepToJsonObject(WorkflowStep step,
            JSONObject graph){
        JSONObject jobj = graph;
        if(graph == null)
            jobj = new JSONObject();
        try{
            JSONObject jstep  = new JSONObject(); 
            jstep.put("operation", step.getOperation().toString());
            jstep.put("parameters", new JsonStringMap(step.getParameters())
                    .getJSONObject());
            if(step.getPreviousSteps() != null &&
                    !step.getPreviousSteps().isEmpty()){
                JSONArray jarr = new JSONArray();
                List<WorkflowStep> prev = step.getPreviousSteps();
                for(int i = 0; i < prev.size(); i++){
                    jarr.put(prev.get(i).getOriginEvent().toString());
                    if(jobj.optJSONObject(prev.get(i).getOriginEvent()
                            .toString()) == null)
                        WorkflowStepImpl.genericStepToJsonObject(prev.get(i), jobj);
                }
                jstep.put("previous", jarr);
            }
            jobj.put(step.getOriginEvent().toString(), jstep);
            return jobj;
        } catch (Exception e){}
        return null;
    }

    @Override
    public URI getOriginEvent() {
        return this.originEvent;
    }
}
