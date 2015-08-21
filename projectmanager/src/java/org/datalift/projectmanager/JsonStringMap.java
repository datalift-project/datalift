package org.datalift.projectmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * this class is a Map implementation with Strings as keys and values.
 * this implementation is based on the json.org library, it provide interactions
 * with JSONObject class and with json strings.
 * 
 * @author rcabaret
 *
 */
public class JsonStringMap implements Map<String, String> {

    private JSONObject jobj = null;
    
    /**
     * construct an empty map
     */
    public JsonStringMap(){
        this.jobj = new JSONObject();
    }
    
    /**
     * construct a map based on a json formated string
     * 
     * @param json  a json as a string
     */
    public JsonStringMap(String json){
        try {
            this.jobj = new JSONObject(json);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * construct a map based on an other one
     * 
     * @param map   the map to extract from
     */
    public JsonStringMap(Map<String, String> map){
        this.jobj = new JSONObject();
        for(Map.Entry<String, String> e : map.entrySet())
            this.put(e.getKey(), e.getValue());
    }
    
    /**
     * construct a map based on a JSONObject, every values are cast as String
     * 
     * @param json  a JSONObject to read
     */
    public JsonStringMap(JSONObject json){
        if(json == null || json.length() == 0)
            this.jobj = new JSONObject();
        else
            this.jobj = new JSONObject(json, JSONObject.getNames(json));
    }
    
    /**
     * return a json string with an json object as the key value content
     * 
     * @return the json string
     */
    public String getJson(){
        return this.jobj.toString();
    }
    
    /**
     * return a json string with an json object as the key value content
     * 
     * @return the json string
     */
    @Override
    public String toString(){
        return this.jobj.toString();
    }
    
    /**
     * return a JSONObject as the key value content
     * 
     * @return the JSONObject
     */
    public JSONObject getJSONObject(){
        String[] names = JSONObject.getNames(this.jobj);
        if(names != null)
            return new JSONObject(this.jobj, names);
        else
            return new JSONObject();
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return this.jobj.length();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return this.jobj.length() == 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsKey(Object key) {
        String k = key.toString();
        return this.jobj.has(k);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsValue(Object value) {
        String v = value.toString();
        return JsonStringMap.getAllAsStringMap(this.jobj).containsValue(v);
    }

    /** {@inheritDoc} */
    @Override
    public String get(Object key) {
        String k = key.toString();
        return this.jobj.optString(k);
    }

    /** {@inheritDoc} */
    @Override
    public String put(String key, String value) {
        if(key == null)
            throw new IllegalArgumentException("the key is null");
        String p = null;
        if(!this.jobj.isNull(key))
            p = this.jobj.optString(key);
        try {
            this.jobj.put(key, value);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public String remove(Object key) {
        String k = key.toString();
        String p = this.jobj.optString(k);
        this.jobj.remove(k);
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        for(Map.Entry<? extends String, ? extends String> e : m.entrySet()){
            this.put(e.getKey(), e.getValue());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        this.jobj = new JSONObject();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> keySet() {
        Set<String> s = new HashSet<String>();
        String[] names = JSONObject.getNames(this.jobj);
        if(names != null)
            for(String k : names)
                    s.add(k);
        return s;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> values() {
        Collection<String> s = new ArrayList<String>();
        String[] names = JSONObject.getNames(this.jobj);
        if(names != null)
            for(String k : names)
                    s.add(this.jobj.optString(k));
        return s;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        Set<Map.Entry<String, String>> s
                = new HashSet<Map.Entry<String, String>>();
        String[] names = JSONObject.getNames(this.jobj);
        if(names != null)
            for(String k : names)
                s.add(new Couple(k, this.jobj.optString(k)));
        return s;
    }
    
    //cast all values of the JSONObject as String
    private static Map<String, String> getAllAsStringMap(JSONObject json){
        Map<String, String> result = new HashMap<String, String>();
        String[] names = JSONObject.getNames(json);
        if(names != null)
            for(String n : names)
                try {
                    result.put(n, json.get(n).toString());
                } catch (JSONException e) {}
        return result;
    }
    
    // the entry class for the Map.entrySet method
    private class Couple implements Map.Entry<String, String>{
        
        private String key;
        private String value;

        public Couple(String k, String v){
            this.key = new String(k);
            this.value = new String(v);
        }
        
        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public String getValue() {
            return this.value;
        }

        @Override
        public String setValue(String value) {
            String v = JsonStringMap.this.put(this.key, value);
            this.value = value;
            return v;
        }
        
    }
}
