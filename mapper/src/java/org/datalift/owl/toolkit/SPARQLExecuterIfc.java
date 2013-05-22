package org.datalift.owl.toolkit;

/**
 * Executes SPARQL queries on a repository and call the helper to handle results.
 * @author mondeca
 *
 */
public interface SPARQLExecuterIfc {

	/**
	 * Executes a SPARQL query given by the helper, and delegates the result processing to the helper.
	 * @param helper
	 * @throws SPARQLExecutionException
	 */
	public void executeSelect(SelectSPARQLHelper helper) throws SPARQLExecutionException;
	
	/**
	 * Executes a SPARQL query given by the helper, and delegates the result processing to the helper.
	 * @param helper
	 * @throws SPARQLExecutionException
	 */
	public void executeConstruct(ConstructSPARQLHelper helper) throws SPARQLExecutionException;
	
	/**
	 * Executes a SPARQL query given by the helper, and return the boolean result
	 * @param helper
	 * @throws SPARQLExecutionException
	 */
	public boolean executeAsk(AskSPARQLHelper helper) throws SPARQLExecutionException;
}
