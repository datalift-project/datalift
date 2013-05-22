package org.datalift.owl.toolkit;

import org.openrdf.query.TupleQueryResultHandler;

/**
 * An extension to a Sesame TupleQueryResultHandler that can generate a SPARQL query, and return associated bindings.
 * <p/>This interface is capable of generating a SPARQL query, and handle the results from this query.
 * See the base implementation SelectSPARQLHelperBase that implements empty methods of TupleQueryResultHandler
 * that are usually not useful.
 * <p/>This is to be used with SesameSPARQLExecuter. Usage exemple :
 * <code>
 * RepositoryProviderIfc provider = ...;
 * SelectSPARQLHelper helper = ...;
 * SesameSPARQLExecuter.newExecuter(provider).executeSelect(helper);
 * </code>
 * 
 * @author mondeca
 *
 */
public interface SelectSPARQLHelper extends TupleQueryResultHandler, SPARQLHelperIfc {
	
}