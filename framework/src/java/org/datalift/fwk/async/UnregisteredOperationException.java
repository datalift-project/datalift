package org.datalift.fwk.async;

import java.net.URI;

@SuppressWarnings("serial")
public class UnregisteredOperationException extends Exception{

    public UnregisteredOperationException(URI operation){
        super("the operation runnable code is unreachable : " +
                operation.toString());
    }
}
