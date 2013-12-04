/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *                  A. Valensi
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.core.project;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.apache.log4j.Logger;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.TransformationModule;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;

/**
 * This class is the implementation of the interface ProcessingTask. It is an 
 * event used to execute the task of a {@link TransformationModule}.
 *  
 * @author avalensi
 */
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
	
	private static Logger logger = Logger.getLogger(ProcessingTaskImpl.class);
	
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
		System.out.println(transformationId);
		this.setEventStatus(EventStatus.NEW);
		
		// TODO: 
		//SecurityContext.getContext().getPrincipal();

		this.setStartedAtTime(new Date());
	}

	//-------------------------------------------------------------------------
    // Runnable contract implementation
    //-------------------------------------------------------------------------

	/**
	 * It is the method called by the {@link TaskManager} to execute the process
	 * of transformation of a {@link TransformationModule}. It also update the
	 * status and the end date of the {@link Event}.
	 */
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

		this.setEndedAtTime(new Date());

		pm.saveEvent(this);
		
		logger.info("Saved");
	}
	
	//-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
	@Override
	public String getTransformationId() {
		return this.transformationId;
	}

    /** {@inheritDoc} */
	@Override
	public EventStatus getEventStatus() {
		return EventStatus.valueOf(this.eventStatus);
	}

    /** {@inheritDoc} */
	@Override
	public void setEventStatus(EventStatus eventStatus) {
		this.eventStatus = eventStatus.toString();
	}
	
    /** {@inheritDoc} */
	@Override
	public void addParam(String name, Object param) {
		this.params.add(name, param);
	}
	
    /** {@inheritDoc} */
	@Override
	public void saveParams() {
		this.parameters = this.params.save();
	}
	
    /** {@inheritDoc} */
	@Override
	public void loadParams() throws Exception {
		this.params.load(this.parameters);
	}
	
    /** {@inheritDoc} */
	@Override
	public Object getParam(String name) {
		return this.params.getParam(name);
	}
	
	//-------------------------------------------------------------------------
    // Inner classes
    //-------------------------------------------------------------------------

	/**
	 * This class is used to serialize object to {@link String} using Gson.
	 * <br /><br />
	 * 
	 * Usage:
	 * <pre>
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
	 * </pre>
	 *
	 * @author lbihanic
	 */
	private class JsonParam
	{

		//---------------------------------------------------------------------
	    // Instance members
	    //---------------------------------------------------------------------
		
		/** Contain the association of key/parameter */
		private Map<String,Object> m = new HashMap<String,Object>();
		
		/** Object used to serialize/de-serialize */
		private Gson json = new Gson();

		//---------------------------------------------------------------------
	    // Methods implementation
	    //---------------------------------------------------------------------
		
		/**
		 * Add a parameter.
		 * 
		 * @param name is the key.
		 * @param param is the parameter.
		 */
		public void add(String name, Object param) {
			this.m.put(name, param);
		}

		/**
		 * Save (serialize) the parameters.
		 * 
		 * @return The serialized {@link String}.
		 */
		public String save() {
			ParamWrapper[] x = new ParamWrapper[this.m.keySet().size()];
			int i=0;
			for (Map.Entry<String,Object> e : m.entrySet()) {
				x[i++] = new ParamWrapper(e.getKey(), e.getValue());
			}
			return json.toJson(x);
		}

		/**
		 * Load (de-serialize) the parameters.
		 * 
		 * @param json is the serialized {@link String}.
		 * @throws Exception
		 */
		public void load(String json) throws Exception {
			this.m.clear();
			ParamWrapper[] x = this.json.fromJson(json, ParamWrapper[].class);
			for (ParamWrapper p : x) {
				Class<?> c = Class.forName(p.clazz);
				this.m.put(p.name, this.json.fromJson(p.value, c));
			}
		}
		
		/**
		 * Get a parameter. load() method must be called before.
		 * 
		 * @param name is the key to get the parameter.
		 * @return The parameter.
		 */
		public Object getParam(String name) {
			return this.m.get(name);
		}

		/**
		 * It is used to help the serialization.
		 * 
		 * @author lbihanic
		 *
		 */
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
