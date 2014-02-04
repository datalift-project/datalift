package org.datalift.owl.toolkit;

import java.util.List;

import org.openrdf.query.TupleQueryResultHandlerException;

/**
 * Base implementation of SelectSPARQLHelper that implements methods not often used,
 * the only method that you will need to implement is getSparql and handleSolution.
 * 
 * @author mondeca
 *
 */
public abstract class SelectSPARQLHelperBase extends SPARQLHelperBase implements SelectSPARQLHelper {

	@Override
	public void endQueryResult() 
	throws TupleQueryResultHandlerException {
		// rien
	}

	@Override
	public void startQueryResult(List<String> arg0)
	throws TupleQueryResultHandlerException {
		// rien
	}
	
}