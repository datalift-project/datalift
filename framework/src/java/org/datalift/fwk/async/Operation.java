package org.datalift.fwk.async;

import java.net.URI;
import java.util.Map;

public interface Operation {

    public URI getOperationId();
    public void execute(Map<String, String> parameters) throws Exception;
}
