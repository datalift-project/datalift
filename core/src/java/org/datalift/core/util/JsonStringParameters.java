package org.datalift.core.util;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonStringParameters {

    private Map<String, String> map = null;
    private String json = null;
    
    public JsonStringParameters(String json){
        this.json = new String(json);
        try {
            JSONObject jobj = new JSONObject(json);
            String[] names = JSONObject.getNames(jobj);
            this.map = new HashMap<String, String>();
            for(String n : names)
                this.map.put(n, jobj.getString(n));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
     }
    
    public JsonStringParameters(Map<String, String> map){
        this.map = new HashMap<String, String>(map);
        this.json = new JSONObject(map).toString();
     }
    
    public String getJson(){
        return this.json;
    }
    
    public Map<String, String> getParametersMap(){
        return this.map;
    }
    
    @Override
    public String toString(){
        return this.json;
    }
}
