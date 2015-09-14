package org.datalift.fwk.async;

import java.net.URI;

/**
 * the interface for Datalift operations
 * 
 * @author rcabaret
 */
public interface Operation {
    
    /**
     * Return the uri that identify the Operation
     * 
     * @return the URI
     */
    public URI getOperationId();
    
    /**
     * Execute the Operation
     * 
     * @param parameters    the parameters
     * @throws Exception
     */
    public void execute(Parameters parameters) throws Exception;
    
    /**
     * Return the parameters pattern that the operation need to be executed without values
     * 
     * @return  the Parameters
     */
    public Parameters getBlankParameters();
}
