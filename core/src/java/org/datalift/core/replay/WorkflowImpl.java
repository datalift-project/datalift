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
import org.datalift.fwk.async.Task;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.async.TaskManager;
import org.datalift.fwk.async.TaskStatus;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.GenericRdfDao;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.Source;
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
        this.refrechJsons();
        if(this.vars != null)
            return this.vars;
        else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addVariable(String name, String defaultValue) {
        this.refrechJsons();
        if(this.vars == null)
            this.vars = new JsonStringMap();
        this.vars.put(name, defaultValue);
        this.variabels = this.vars.toString();
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowStep getOutputStep() {
        this.refrechJsons();
        return this.output;
    }

    /** {@inheritDoc} */
    @Override
    public void setSteps(WorkflowStep outputStep) {
        if(outputStep != null){
            if(outputStep.getNextSteps() != null &&
                    !outputStep.getNextSteps().isEmpty())
                throw new RuntimeException("the step must be the last one");
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
    private void refrechJsons(){
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
        this.refrechJsons();
        List<Collection<WorkflowStep>> steps =
                new ArrayList<Collection<WorkflowStep>>();
        ProjectManager projectManager =
                Configuration.getDefault().getBean(ProjectManager.class);
        Event origin = projectManager.getEvent(this.origin);
        Project project = origin.getProject();
        Task fail = null;
        Event parentEvent = null;
        try{
            // build the Replay levels
            steps.add(new ArrayList<WorkflowStep>());
            steps.get(0).add(this.output);
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
            //execute steps by level
            VersatileProperties properties = new VersatileProperties();
            if(this.vars != null)
                for(Entry<String, String> prop : this.vars.entrySet())
                    properties.putString(prop.getKey(), prop.getValue());
            Map<WorkflowStep, Task> done = new HashMap<WorkflowStep, Task>();
            TaskManager taskManager =
                    Configuration.getDefault().getBean(TaskManager.class);
            TaskContext context = TaskContext.getCurrent();
            ((TaskContextBase) context).startOperation(project, URI.create(
                    "http://www.datalift.org/core/workflowImpl/operation/replay"),
                    null);
            parentEvent = context.beginAsEvent(
                    Event.INFORMATION_EVENT_TYPE, Event.WORKFLOW_EVENT_SUBJECT);
            for(int j = steps.size() - 1; j >= 0 && fail == null; j--){
                List<Task> tasks = new ArrayList<Task>();
                for(WorkflowStep step : steps.get(j)){
                    Map<String, String> eventParameters = projectManager
                            .getEvent(step.getOriginEvent()).getParameters();
                    Map<String, String> params = new HashMap<String, String>();
                    for(String param : eventParameters.keySet()){
                        String value = step.getParameters().get(param);
                        if(param.startsWith(WorkflowStep.INPUT_PARAM_KEY)){
                            WorkflowStep prev = this
                                    .findPrevousStepUsedOnParameter(step, param);
                            params.put(param, done.get(prev).getRunningEvent()
                                    .getInfluenced().toString());
                        } else if(param.equals(WorkflowStep.OUTPUT_PARAM_KEY)) {
                            params.put(param, this
                                    .checkUriConflict(null).toString());
                        } else if(param.equals(WorkflowStep.PROJECT_PARAM_KEY)) {
                            params.put(param, project.getUri());
                        } else if(!param.startsWith(WorkflowStep.HIDDEN_PARAM_KEY)) {
                            params.put(param, properties.resolveVariables(value));
                        }
                    }
                    Task task = taskManager.submit(
                            project, step.getOperation(), params);
                    tasks.add(task);
                    done.put(step, task);
                }
                for(Task t : tasks){
                    taskManager.waitForEnding(t);
                    if(t.getStatus() == TaskStatus.failStatus)
                        fail = t;
                }
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally {
            if(parentEvent != null){
                // delete events and sources produced during the replay
                TaskContext context = TaskContext.getCurrent();
                project = projectManager.findProject(URI.create(project.getUri()));
                Collection<Source> sourcesToDel = new ArrayList<Source>();
                Collection<URI> ToDel = new ArrayList<URI>();
                Collection<Workflow> workflowsToDel = new ArrayList<Workflow>();
                GenericRdfDao dao = projectManager.getRdfDao();
                Collection<Event> allEvents = this.findAllInformatedEvent(
                        projectManager.getEvents(project).values(), parentEvent);
                for(Event e : allEvents){
                    if(e.getEventType() == Event.CREATION_EVENT_TYPE){
                        if(e.getInfluenced() != null)
                            try{
                                if(ToDel.contains(e.getInfluenced())){
                                    dao.delete(e);
                                    continue;
                                }
                                Source s = project.getSource(e.getInfluenced());
                                if(sourcesToDel.contains(s)){
                                    dao.delete(e);
                                    continue;
                                }
                                Workflow w = project.getWorkflow(e.getInfluenced());
                                if(workflowsToDel.contains(w)){
                                    dao.delete(e);
                                    continue;
                                }
                                if(s != null)
                                    sourcesToDel.add(s);
                                else if(w != null)
                                    workflowsToDel.add(w);
                                else
                                    ToDel.add(e.getInfluenced());
                            } catch (Exception ex){
                                Logger.getLogger().debug(
                                        "the influenced entity {} does not exist",
                                        e.getInfluenced());
                            }
                    }
                    dao.delete(e);
                }
                for(Source s : sourcesToDel)
                    project.remove(s);
                for(Workflow w : workflowsToDel)
                    project.removeWorkflow(w.getUri());
                for(URI o : ToDel){
                    Ontology onto = null;
                    for(Ontology ponto : project.getOntologies())
                        if(ponto.getUri().equals(o.toString()))
                            onto = ponto;
                    if(onto != null)
                        project.removeOntology(onto.getTitle());
                }
                projectManager.saveProject(project);
                project = projectManager.findProject(URI.create(project.getUri()));
                allEvents = this.findAllInformatedEvent(
                        projectManager.getEvents(project).values(), parentEvent);
                for(Event e : allEvents)
                    dao.delete(e);
                if(fail == null)
                    ((TaskContextBase) context).endOperation(true);
                else
                    ((TaskContextBase) context).endOperation(false);
                dao.delete(parentEvent);
            }
        }
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
     * return all events of the list which is directly or transitively informed by the given event
     * 
     * @param events    the Collection of Event to search in
     * @param informer  informer Event
     * @return  the Collection of informed Events
     */
    private Collection<Event> findAllInformatedEvent(Collection<Event> events,
            Event informer){
        List<URI> informers = new ArrayList<URI>();
        List<Event> informed = new ArrayList<Event>();
        List<Event> rejected = new ArrayList<Event>();
        for(Event e : events)
            if(e.getInformer() != null)
                rejected.add(e);
        informers.add(informer.getUri());
        boolean found = true;
        while(found){
            found = false;
            int i = 0;
            while(i < rejected.size()){
                Event e = rejected.get(i);
                if(informers.contains(e.getInformer())){
                    found = true;
                    informers.add(e.getUri());
                    informed.add(e);
                    rejected.remove(i);
                } else {
                    i++;
                }
            }
        }
        return informed;
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
