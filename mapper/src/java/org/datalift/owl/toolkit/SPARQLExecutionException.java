package org.datalift.owl.toolkit;

/**
 * An exception indicating an error happened while executing a SPARQL query
 * 
 * @author mondeca
 */
public class SPARQLExecutionException extends Exception {

	private static final long serialVersionUID = -7140173477048016716L;

	public SPARQLExecutionException(String msg) {
		super(msg);
	}

	public SPARQLExecutionException(Throwable cause) {
		super(cause);
	}

	public SPARQLExecutionException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
