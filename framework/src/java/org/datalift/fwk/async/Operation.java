package org.datalift.fwk.async;

import java.net.URI;
import java.util.Map;

public interface Operation {

    /** key for the project parameter */
    public static final String PROJECT_PARAM_KEY = "DATALIFT_PROJECT";
    /** base for keys of used sources parameters */
    public static final String INPUT_PARAM_KEY = "DATALIFT_USED_";
    /** key for the influenced source parameter */
    public static final String OUTPUT_PARAM_KEY = "DATALIFT_OUTPUT";
    /** base for keys of hidden parameters that the operation can provide */
    public static final String HIDDEN_PARAM_KEY = "DATALIFT_HIDDEN_";
    
    public URI getOperationId();
    public void execute(Map<String, String> parameters) throws Exception;
}
