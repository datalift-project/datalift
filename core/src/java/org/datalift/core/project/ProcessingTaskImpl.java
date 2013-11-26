package org.datalift.core.project;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.TransformationModule;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;

@Entity
@MappedSuperclass
@RdfsClass("datalift:TransformationEvent")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="datalift:datalift")
public class ProcessingTaskImpl extends EventImpl implements ProcessingTask {

	//-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

	@RdfProperty("datalift:transformationId")
	String transformationId;

	@RdfProperty("datalift:parameters")
	String parameters;

	@RdfProperty("datalift:eventStatus")
	private String eventStatus;

	JsonParam params = new JsonParam();
	
	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------
	
	/**
	 * Constructor to create a processing task.
	 * 
	 * @param transformationId   the id of the class derived from 
	 *                           TransformationModule used to execute the task. 
	 * @param baseUri            it is the base URI until the project name like
	 *                           "http://www.datalift.org/project/name/"
	 */
	public ProcessingTaskImpl(String transformationId, String baseUri) {
		char lastChar = baseUri.charAt(baseUri.length() - 1);
		if (lastChar != '#' && lastChar != '/')
			baseUri += '/';

		this.setId(baseUri + UUID.randomUUID());
		this.transformationId = transformationId;
		this.setEventStatus(EventStatus.NEW);
	}

	//-------------------------------------------------------------------------
    // Runnable contract implementation
    //-------------------------------------------------------------------------

	public void run() {
		Configuration cfg = Configuration.getDefault();
		TransformationModule m = (TransformationModule) cfg.getBean(
				this.getTransformationId());
		
		ProjectManager pm = 
				Configuration.getDefault().getBean(ProjectManager.class);
		
		this.setEventStatus(EventStatus.RUNNING);
		pm.saveEvent(this);
		
		if (m.execute(this))
			this.setEventStatus(EventStatus.COMPLETE);
		else
			this.setEventStatus(EventStatus.FAIL);
		
		pm.saveEvent(this);
	}
	
	//-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

	public String getTransformationId() {
		return this.transformationId;
	}

	public EventStatus getEventStatus() {
		return EventStatus.valueOf(this.eventStatus);
	}

	public void setEventStatus(EventStatus eventStatus) {
		this.eventStatus = eventStatus.toString();
	}
	
	public void addParam(String name, Object param) {
		this.params.add(name, param);
	}
	
	public void saveParams() {
		this.parameters = this.params.save();
	}
	
	public void loadParams() throws Exception {
		this.params.load(this.parameters);
	}
	
	public Object getParam(String name) {
		return this.params.getParam(name);
	}
	
	//-------------------------------------------------------------------------
    // Inner classes
    //-------------------------------------------------------------------------

	/**
	 * 
	 * @author lbihanic
	 * 
	 * Usage:
	 *   JsonParam tj = new JsonParam();
	 *   tj.add("1", "abc");
	 *   tj.add("toto", Boolean.TRUE);
	 *   tj.add("0", Integer.valueOf(123));
	 *   tj.add("project", URI.create("http://localhost:9091/project/toto"));
	 *   String s = tj.save();
	 *   System.out.println(s);
	 *
	 *   tj = new JsonParam();
	 *   tj.load(s);
	 *   for (Map.Entry<String,Object> e : tj.m.entrySet()) {
	 *   	Object o  = e.getValue();
	 *   	System.out.println(e.getKey() + " -> " + o + " (" + o.getClass() + ')');
	 *   }
	 *
	 */
	private class JsonParam
	{
		private Map<String,Object> m = new HashMap<String,Object>();
		private Gson json = new Gson();

		public void add(String name, Object param) {
			this.m.put(name, param);
		}

		public String save() {
			ParamWrapper[] x = new ParamWrapper[this.m.keySet().size()];
			int i=0;
			for (Map.Entry<String,Object> e : m.entrySet()) {
				x[i++] = new ParamWrapper(e.getKey(), e.getValue());
			}
			return json.toJson(x);
		}

		public void load(String json) throws Exception {
			this.m.clear();
			ParamWrapper[] x = this.json.fromJson(json, ParamWrapper[].class);
			for (ParamWrapper p : x) {
				Class<?> c = Class.forName(p.clazz);
				this.m.put(p.name, this.json.fromJson(p.value, c));
			}
		}
		
		public Object getParam(String name) {
			return this.m.get(name);
		}

		private final class ParamWrapper
		{
			public final String name;
			public final String clazz;
			public final String value;

			public ParamWrapper(String name, Object p) {
				this.name  = name;
				this.clazz = p.getClass().getName();
				this.value = json.toJson(p);
			}
		}
	}
}
