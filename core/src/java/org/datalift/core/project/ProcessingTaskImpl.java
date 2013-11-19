package org.datalift.core.project;

import java.net.URI;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.TransformationModule;
import org.datalift.fwk.project.ProcessingTask;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import java.util.HashMap;
import java.util.Map;

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
	URI transformationId;

	@RdfProperty("datalift:eventStatus")
	private EventStatus eventStatus;

	@RdfProperty("datalift:parameters")
	String parameters;

	JsonParam params = new JsonParam();
	
	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------
	
	public ProcessingTaskImpl(URI transformationId) {
		this.transformationId = transformationId;
		this.eventStatus = EventStatus.NEW;
	}

	//-------------------------------------------------------------------------
    // Runnable contract implementation
    //-------------------------------------------------------------------------

	public void run() {
		Configuration cfg = Configuration.getDefault();
		TransformationModule m = (TransformationModule) cfg.getBean(this.getTransformationId().toString());
		if (m == null)
			throw new RuntimeException("Unable to gat TransformationModule (null)");
		m.execute(this);
	}
	
	//-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

	public URI getTransformationId() {
		return transformationId;
	}

	public EventStatus getEventStatus() {
		return eventStatus;
	}

	public void setEventStatus(EventStatus eventStatus) {
		this.eventStatus = eventStatus;
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
