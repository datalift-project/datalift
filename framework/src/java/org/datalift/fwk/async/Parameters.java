package org.datalift.fwk.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * parameters for Operations execution
 * throws exceptions if undefined labels are used
 * 
 * @author rcabaret
 */
public class Parameters {
    private Map<String, Parameter> parametersMap =
            new HashMap<String, Parameter>();
    private Map<String, String> valuesMap = new HashMap<String, String>();
    private String project = null;
    private String output = null;
    
    /**
     * construct the parameter list with every given parameters pattern
     * 
     * @param params    all parameters pattern
     */
    public Parameters(Collection<Parameter> params) {
        Map<String, Integer> doubles = new HashMap<String, Integer>();
        for (Parameter param : params) {
            // if there are several projects
            if (param.getType().equals(ParameterType.project)) {
                if (this.project == null) {
                    this.project = param.getLabel();
                }
                else {
                    throw new RuntimeException("an operation cant be in several projects");
                }
            }
            // if there are several output sources
            if (param.getType().equals(ParameterType.output_source)) {
                if (this.output == null) {
                    this.output = param.getLabel();
                }
                else {
                    throw new RuntimeException("an operation cant have several outputs");
                }
            }
            // if several parameters have the same label
            if (this.parametersMap.containsKey(param.getLabel())) {
                if (doubles.containsKey(param.getLabel())) {
                    doubles.put(param.getLabel(),
                            new Integer(doubles.get(param.getLabel()) + 1));
                }
                else {
                    doubles.put(param.getLabel(), new Integer(2));
                }
            }
            // if the parameter is not null
            if (param != null) {
                this.parametersMap.put(param.getLabel(), param);
            }
        }
        // throw exception if there was many parameters for the same label
        if (!doubles.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            if (doubles.keySet().size() > 1) {
                Entry<String, Integer> entry = doubles.entrySet().iterator().next();
                msg.append("a label is used several times : ")
                        .append(entry.getKey()).append(" ")
                        .append(entry.getValue()).append(" times");
            }
            else {
                Iterator<Entry<String, Integer>> i = doubles.entrySet().iterator();
                Entry<String, Integer> entry = i.next();
                msg.append(doubles.keySet().size())
                        .append(" labels are used several times : ")
                        .append(entry.getKey()).append(" : ")
                        .append(entry.getValue()).append(" times");
                while (i.hasNext()) {
                    entry = i.next();
                    msg.append(" ; ").append(entry.getKey()).append(" : ")
                            .append(entry.getKey()).append(" : ")
                            .append(entry.getValue()).append(" times");
                }
                throw new RuntimeException(msg.toString());
            }
            
        }
    }
    
    /**
     * define a value for the labeled parameter
     * 
     * @param paramLabel    the label of the parameter
     * @param value         the value to associate with
     */
    public void setValue(String paramLabel, String value) {
        if (this.parametersMap.containsKey(paramLabel)) {
            this.valuesMap.put(paramLabel, value);
        }
        else {
            throw new RuntimeException("the parameter is unknown : " + paramLabel);
        }
    }
    
    /**
     * return the value associated with the labeled parameter
     * 
     * @param paramLabel    the label of the parameter
     * @return  the associated value or null if there is no value associated with
     */
    public String getValue(String paramLabel) {
        if (this.parametersMap.containsKey(paramLabel)) {
            return this.valuesMap.get(paramLabel);
        }
        else {
            throw new RuntimeException("the parameter is unknown : " + paramLabel);
        }
    }
    
    /**
     * set values for all given labeled parameters
     * 
     * @param values    Map of labels and values
     */
    public void setValues(Map<String, String> values) {
        for (Entry<String, String> entry : values.entrySet()) {
            this.setValue(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * return all defined values
     * 
     * @return  Map of labels and values
     */
    public Map<String, String> getValues() {
        Map<String, String> vals = new HashMap<String, String>();
        for (Entry<String, String> entry : this.valuesMap.entrySet()) {
            vals.put(entry.getKey(), entry.getValue());
        }
        return vals;
    }
    
    /**
     * return all defined values only for visible parameters
     * 
     * @return  Map of labels and values
     */
    public Map<String, String> getVisibleValues() {
        Map<String, String> vals = new HashMap<String, String>();
        for (Entry<String, Parameter> entry : this.parametersMap.entrySet()) {
            String value = this.valuesMap.get(entry.getKey());
            if (entry.getValue().getType().isVisible() && value != null) {
                vals.put(entry.getKey(), value);
            }
        }
        return vals;
    }
    
    /**
     * return the parameter associated with the given label
     * 
     * @param paramLabel    the label
     * @return  the associated Parameter
     */
    public Parameter getParameter(String paramLabel) {
        if (this.parametersMap.containsKey(paramLabel)) {
            return this.parametersMap.get(paramLabel);
        }
        else {
            throw new RuntimeException("the parameter is unknown : " + paramLabel);
        }
    }
    
    /**
     * return the value associated with the project typed parameter
     * 
     * @return  the value of null if no value is associated or if no project parameter was defined
     */
    public String getProjectValue() {
        return this.getValue(this.project);
    }
    
    /**
     * return the value associated with the output source typed parameter
     * 
     * @return  the value of null if no value is associated or if no output source parameter was defined
     */
    public String getOutputValue() {
        return this.getValue(this.output);
    }
    
    /**
     * return the collection of every defined labels
     * 
     * @return  the labels collection
     */
    public Collection<String> labels() {
        Collection<String> ret = new ArrayList<String>();
        for (Parameter param : this.parametersMap.values()) {
            ret.add(param.getLabel());
        }
        return ret;
    }
}
