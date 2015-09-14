package org.datalift.fwk.async;

/**
 * definition of a operation parameter
 * 
 * @author rcabaret
 */
public class Parameter {
    
    private String label;
    private String translationId;
    private ParameterType type;
    
    /**
     * construct a parameter definition
     * 
     * @param label         the label of the parameter
     * @param translationId the id use to translate the parameter name
     * @param type          the type of the parameter
     */
    public Parameter(String label, String translationId, ParameterType type) {
        this.label = label;
        this.translationId = translationId;
        this.type = type;
    }
    
    /**
     * return the label of the parameter
     * 
     * @return  the label
     */
    public String getLabel() {
        return this.label;
    }
    
    /**
     * return the id use to translate the parameter name
     * 
     * @return the id
     */
    public String getTranslationId() {
        return this.translationId;
    }
    
    /**
     * return the type of the parameter
     * 
     * @return  the ParameterType
     */
    public ParameterType getType() {
        return this.type;
    }
}
