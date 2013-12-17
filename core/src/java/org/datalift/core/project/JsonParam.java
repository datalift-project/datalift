package org.datalift.core.project;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

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
public class JsonParam
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
	 * Add a parameter.
	 * 
	 * @param m map parameter to add.
	 */
	public void add(Map<String, Object> m) {
		this.m.putAll(m);
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
