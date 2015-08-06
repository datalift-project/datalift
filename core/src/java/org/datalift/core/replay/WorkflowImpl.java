package org.datalift.core.replay;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;

import org.datalift.core.project.BaseRdfEntity;
import org.datalift.core.util.JsonStringParameters;
import org.datalift.fwk.replay.Workflow;
import org.datalift.fwk.replay.WorkflowStep;
import org.json.JSONException;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

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
    private JsonStringParameters vars = null;
    @RdfProperty("datalift:parameters")
    private String parameters = null;
    private WorkflowStepImpl output = null;

    public WorkflowImpl(){
        //NOP
    }
    
    public WorkflowImpl(URI uri, String title, String description,
            Map<String, String> variables, WorkflowStepImpl output){
        this.uri = uri;
        this.description = description;
        this.title = title;
        this.vars = new JsonStringParameters(variables);
        this.variabels = this.vars.toString();
        this.output = output;
        this.parameters = this.output.toString();
    }
    
    @Override
    public URI getUri() {
        return this.uri;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, String> getVariables() {
        this.refrechJsons();
        if(this.vars != null)
            return this.vars.getParametersMap();
        else
            return null;
    }

    @Override
    public void addVariable(String name, String defaultValue) {
        this.refrechJsons();
        Map<String, String> var;
        if(this.vars == null)
            var = new HashMap<String, String>();
        else
            var = this.vars.getParametersMap();
        var.put(name, defaultValue);
        this.vars = new JsonStringParameters(var);
        this.variabels = this.vars.toString();
    }

    @Override
    public WorkflowStep getOutputStep() {
        this.refrechJsons();
        return this.output;
    }

    @Override
    public void setSteps(WorkflowStep outputStep) {
        this.output = (WorkflowStepImpl) outputStep;
        this.parameters = this.output.toString();
    }

    @Override
    protected void setId(String id) {
        this.uri = URI.create(id);
    }

    private void refrechJsons(){
        if(this.parameters != null && this.output == null)
            try {
                this.output = new WorkflowStepImpl(this.parameters);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        if(this.variabels != null && this.vars == null)
            this.vars = new JsonStringParameters(this.variabels);
    }
}
