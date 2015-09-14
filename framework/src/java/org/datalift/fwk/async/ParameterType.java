package org.datalift.fwk.async;

public enum ParameterType {
    /**
     * a simple visible parameter
     */
    visible(true),
    
    /**
     * the project uri parameter
     */
    project(false),
    
    /**
     * an uri for an input source parameter
     */
    input_source(false),
    
    /**
     * the output source uri
     */
    output_source(false),
    
    /**
     * an optional parameter that a workflow can't use
     */
    hidden(false),
    
    /**
     * an url use to reach input data, a workflow can use it for iterations
     */
    input(true);
    
    private boolean isVisible;
    
    ParameterType(boolean isVisible) {
        this.isVisible = isVisible;
    }
    
    /**
     * return if the parameter is usable on a workflow
     * 
     * @return
     */
    public boolean isVisible() {
        return this.isVisible;
    }
}
