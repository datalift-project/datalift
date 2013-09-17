package org.datalift.owl.toolkit;

/**
 * Defines objects that are capable of building and returning a SPARQL query (either SELECT
 * or CONSTRUCT). Concrete implementations can simply return a String, read from a File, read
 * in a Spring configuration, parse some format, etc. 
 * 
 * @author mondeca
 *
 */
public interface SPARQLQueryBuilderIfc {

	public String getSPARQL();
	
}
