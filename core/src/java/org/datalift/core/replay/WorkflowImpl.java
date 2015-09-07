package org.datalift.core.replay;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;

import org.datalift.core.async.TaskContextBase;
import org.datalift.core.project.BaseRdfEntity;
import org.datalift.core.util.JsonStringMap;
import org.datalift.core.util.VersatileProperties;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.async.Operation;
import org.datalift.fwk.async.Task;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.async.TaskManager;
import org.datalift.fwk.async.TaskStatus;
import org.datalift.fwk.async.UnregisteredOperationException;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.prov.Event;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.replay.Workflow;
import org.datalift.fwk.replay.WorkflowStep;
import org.json.JSONException;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

/**
 * the default implementation of the Workflow interface
 * 
 * @author rcabaret
 *
 */
@Entity
@RdfsClass("datalift:Workflow")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="http://www.datalift.org/core/projects")
public class WorkflowImpl extends BaseRdfEntity implements Workflow{
    
    @RdfId
    private URI uri;
    @RdfProperty("dc:title")
    private String title;
    @RdfProperty("dc:description")
    private String description;
    @RdfProperty("datalift:variables")
    private String variabels = null;
    private JsonStringMap vars = null;
    @RdfProperty("datalift:parameters")
    private String parameters = null;
    private WorkflowStepImpl output = null;
    @RdfProperty("datalift:originEvent")
    private URI origin;

    public WorkflowImpl(){
        //NOP
    }
    
    public WorkflowImpl(URI uri, String title, String description,
            Map<String, String> variables, WorkflowStepImpl output){
        this.uri = uri;
        this.description = description;
        this.title = title;
        this.vars = new JsonStringMap(variables);
        this.variabels = this.vars.toString();
        this.setSteps(output);
    }
    
    /** {@inheritDoc} */
    @Override
    public URI getUri() {
        return this.uri;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return this.title;
    }

    /** {@inheritDoc} */
    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return this.description;
    }

    /** {@inheritDoc} */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getVariables() {
        this.refreshJsons();
        if(this.vars != null)
            return this.vars;
        else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addVariable(String name, String defaultValue) {
        this.refreshJsons();
        if(this.vars == null)
            this.vars = new JsonStringMap();
        this.vars.put(name, defaultValue);
        this.variabels = this.vars.toString();
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowStep getOutputStep() {
        this.refreshJsons();
        return this.output;
    }

    /** {@inheritDoc} */
    @Override
    public void setSteps(WorkflowStep outputStep) {
        if(outputStep != null){
            // control if the step is the last one on the oriented graph
            if(outputStep.getNextSteps() != null &&
                    !outputStep.getNextSteps().isEmpty())
                throw new RuntimeException("the step must be the last one");
            // control if there are several "last step" on the graph
            Collection<WorkflowStep> controled = new ArrayList<WorkflowStep>();
            List<WorkflowStep> toControl = new ArrayList<WorkflowStep>();
            toControl.add(outputStep);
            while(!toControl.isEmpty()){
                WorkflowStep stc = toControl.get(0);
                if(!controled.contains(stc)){
                    controled.add(stc);
                    toControl.addAll(stc.getNextSteps());
                    toControl.addAll(stc.getPreviousSteps());
                }
                toControl.remove(0);
            }
            boolean root = false;
            for(WorkflowStep s : controled)
                if(s.getNextSteps() == null || s.getNextSteps().isEmpty()){
                    if(root)
                        throw new RuntimeException(
                                "thers several roots on the step graph");
                    else
                        root = true;
                }
            this.output = (WorkflowStepImpl) outputStep;
            this.parameters = this.output.toString();
            this.origin = this.output.getOriginEvent();
        } else {
            this.output = null;
            this.parameters = null;
            this.origin = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void setId(String id) {
        this.uri = URI.create(id);
    }

    /** {@inheritDoc} */
    private void refreshJsons(){
        if(this.parameters != null && this.output == null)
            try {
                this.output = new WorkflowStepImpl(this.parameters,
                        this.getOriginEvent());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        if(this.variabels != null && this.vars == null)
            this.vars = new JsonStringMap(this.variabels);
    }

    /** {@inheritDoc} */
    @Override
    public void removeAllVariables() {
        this.variabels = null;
        this.vars = null;
    }

    /** {@inheritDoc} */
    @Override
    public URI getOriginEvent() {
        return this.origin;
    }
    
    /** {@inheritDoc} */
    @Override
    public void replay(Map<String, String> variables){
        this.refreshJsons();
        ProjectManager projectManager =
                Configuration.getDefault().getBean(ProjectManager.class);
        Event origin = projectManager.getEvent(this.origin);
        Project project = origin.getProject();
        Event parentEvent = null;
        Task fail = null;
        try{
            // build execution levels
            List<Collection<WorkflowStep>> steps = buildExecutionLevels();
            // start operation on TaskContext
            TaskContext context = TaskContext.getCurrent();
            ((TaskContextBase) context).startOperation(project, URI.create(
                    "http://www.datalift.org/core/workflowImpl/operation/replay"),
                    null);
            parentEvent = context.beginAsEvent(
                    Event.INFORMATION_EVENT_TYPE, Event.WORKFLOW_EVENT_SUBJECT);
            //execute levels
            fail = executeLevels(steps);
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally {
            if(parentEvent != null){
                // clean the replay streak
                projectManager.purgeEventChildrens(parentEvent, true);
                // end the operation on the TaskContext
                if(fail == null)
                    ((TaskContextBase) TaskContext.getCurrent()).endOperation(true);
                else
                    ((TaskContextBase) TaskContext.getCurrent()).endOperation(false);
                projectManager.getRdfDao().delete(parentEvent);
                // in case of failure
                if(fail != null){
                    Logger.getLogger()
                            .error("the workflow {} fail during the task {}",
                            this.uri.toString(), fail.getUri().toString());
                    throw new RuntimeException("the workflow " + 
                            this.uri.toString() + " fail during the task " +
                            fail.getUri().toString());
                }
            }
        }
    }

    /**
     * Execute all steps from the last level to the first one
     * 
     * @param steps execution levels
     * @return  null or the failed task if the execution fail
     * @throws UnregisteredOperationException
     */
    private Task executeLevels(List<Collection<WorkflowStep>> steps)
            throws UnregisteredOperationException {
        Task fail = null;
        VersatileProperties properties = new VersatileProperties();
        ProjectManager projectManager =
                Configuration.getDefault().getBean(ProjectManager.class);
        Event origin = projectManager.getEvent(this.origin);
        Project project = origin.getProject();
        TaskManager taskManager =
                Configuration.getDefault().getBean(TaskManager.class);
        Map<WorkflowStep, Task> done = new HashMap<WorkflowStep, Task>();
        // Initialize parameters solver with variables
        if(this.vars != null)
            for(Entry<String, String> prop : this.vars.entrySet())
                properties.putString(prop.getKey(), prop.getValue());
        for(int j = steps.size() - 1; j >= 0 && fail == null; j--){
            List<Task> tasks = new ArrayList<Task>();
            // for every steps of the level
            for(WorkflowStep step : steps.get(j)){
                //resolve parameters and submit the task
                Map<String, String> params = resolveParameters(properties,
                        done, step);
                Task task = taskManager.submit(
                        project, step.getOperation(), params);
                tasks.add(task);
                done.put(step, task);
            }
            // wait for all level tasks ending
            for(Task t : tasks){
                taskManager.waitForEnding(t);
                if(t.getStatus() == TaskStatus.failStatus)
                    fail = t;
            }
        }
        return fail;
    }

    /**
     * resolve all parameters for a Step execution
     * 
     * @param properties    the parameters solver
     * @param done          the already executed steps and tasks which are associated with
     * @param step          the step with parameters to resolve
     * @return              the Map of resolved parameters
     */
    private Map<String, String> resolveParameters(VersatileProperties properties,
            Map<WorkflowStep, Task> done, WorkflowStep step) {
        ProjectManager projectManager =
                Configuration.getDefault().getBean(ProjectManager.class);
        Event origin = projectManager.getEvent(this.origin);
        Project project = origin.getProject();
        Map<String, String> eventParameters = projectManager
                .getEvent(step.getOriginEvent()).getParameters();
        Map<String, String> params = new HashMap<String, String>();
        // for all parameters
        for(String param : eventParameters.keySet()){
            String value = step.getParameters().get(param);
            if(param.startsWith(Operation.INPUT_PARAM_KEY)){
                WorkflowStep prev = this
                        .findPrevousStepUsedOnParameter(step, param);
                params.put(param, done.get(prev).getRunningEvent()
                        .getInfluenced().toString());
            } else if(param.equals(Operation.OUTPUT_PARAM_KEY)) {
                params.put(param, this
                        .checkUriConflict(null).toString());
            } else if(param.equals(Operation.PROJECT_PARAM_KEY)) {
                params.put(param, project.getUri());
            } else if(!param.startsWith(Operation.HIDDEN_PARAM_KEY)) {
                params.put(param, properties.resolveVariables(value));
            }
        }
        return params;
    }

    /**
     * Build the steps of execution, the first on the list is the last to execute
     * 
     * @return the List of levels that are Collections of WorkflowSteps that can be executed at the same time
     */
    private List<Collection<WorkflowStep>> buildExecutionLevels() {
        List<Collection<WorkflowStep>> steps =
                new ArrayList<Collection<WorkflowStep>>();
        steps.add(new ArrayList<WorkflowStep>());
        //last step is the first level
        steps.get(0).add(this.output);
        // all steps that need to be on a higher level
        Map<WorkflowStep, Integer> standBy =
                new HashMap<WorkflowStep, Integer>();
        int i = 0;
        while(!steps.get(i).isEmpty()){
            steps.add(new ArrayList<WorkflowStep>());
            for(WorkflowStep step : steps.get(i)){
                if(step.getPreviousSteps() != null)
                    for(WorkflowStep prev : step.getPreviousSteps()){
                        if(standBy.containsKey(prev)){
                            if(standBy.get(prev) ==
                                    prev.getNextSteps().size() - 1){
                                steps.get(i + 1).add(prev);
                                standBy.remove(prev);
                            } else {
                                standBy.put(prev,
                                        new Integer(standBy.get(prev) + 1));
                            }
                        } else {
                            if(prev.getNextSteps().size() > 1)
                                standBy.put(prev, new Integer(1));
                            else
                                steps.get(i + 1).add(prev);
                        }
                    }
            }
            i++;
        }
        steps.remove(i);
        return steps;
    }
    
    /**
     * return, for the given step, the previous step that point the given parameter
     * 
     * @param step  the step
     * @param parameter the name of the parameter that point to the previous step
     * @return  the previous step
     */
    private WorkflowStep findPrevousStepUsedOnParameter(
            WorkflowStep step,
            String parameter){
        ProjectManager projectManager =
                Configuration.getDefault().getBean(ProjectManager.class);
        Event event = projectManager.getEvent(step.getOriginEvent());
        URI value = URI.create(event.getParameters().get(parameter));
        WorkflowStep input = null;
        for(URI used : event.getUsed()){
            if(used.equals(value)){
                for(WorkflowStep previous : step.getPreviousSteps()){
                    if(value.equals(projectManager.getEvent(
                            previous.getOriginEvent()).getInfluenced())){
                        input = previous;
                    }
                    break;
                }
                break;
            }
        }
        if(input == null)
            throw new RuntimeException(
                    "no used source match with the parameter : " + parameter);
        return input;
    }
    
    /**
     * build an uri that is never used as subject in the internal repository
     * 
     * @param  targetUri        an URI to try
     * @return URI              the usable uri
     */
    private URI checkUriConflict(URI targetUri){
        Repository r = Configuration.getDefault().getInternalRepository();
        URI tryed;
        String base;
        if(targetUri != null){
            base = targetUri.toString();
            tryed = targetUri;
        } else {
            base = "http://www.datalift.org/core/replay/temp/source/";
            tryed = URI.create(base +
                    Double.toString(Math.random()).substring(1, 8));
        }
        boolean found = false;
        do {
            if(found){
                tryed = URI.create(base +
                        Double.toString(Math.random()).substring(1, 8));
            }
            try {
                Map<String,Object> bindings = new HashMap<String,Object>();
                bindings.put("uri", tryed);
                found = r.ask("ASK { ?uri ?p ?o . }", bindings);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        } while (found);
        return tryed;
    }
}
