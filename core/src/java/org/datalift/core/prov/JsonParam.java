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

package org.datalift.core.prov;

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
 *      Object o  = e.getValue();
 *      System.out.println(e.getKey() + " -> " + o + " (" + o.getClass() + ')');
 *   }
 * </pre>
 *
 * @author lbihanic
 * @author rcabaret
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
            if(!p.clazz.equals("null") && !p.value.equals("null")){
                Class<?> c = Class.forName(p.clazz);
                this.m.put(p.name, this.json.fromJson(p.value, c));
            } else {
                this.m.put(p.name, null);
            }
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
     * Get all parameters. load() method must be called before.
     * 
     * @return The parameters map.
     */
    public  Map<String, Object> getParameters() {
        return new HashMap<String, Object>(this.m);
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
            if(p == null){
                this.clazz = "null";
                this.value = "null";
            } else {
                this.clazz = p.getClass().getName();
                this.value = json.toJson(p);
            }
        }
    }
}