package org.datalift.owl.toolkit;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;

/**
 * A generic interface that defines a SPARQL query along with its bindings, default graphs, named graphs
 * parameters (everything needed to execute the query). This is common to both SelectSPARQLHelper and ConstructSPARQLHelper.
 *  
 * @author mondeca
 *
 */
public interface SPARQLHelperIfc {

	/**
	 * Returns a SPARQL query to be executed by a SPARQLExecuterIfc.
	 * @return
	 */
	public String getSPARQL();
	
	/**
	 * Returns the bindings to be set on the returned SPARQL query. For exemple, if the method
	 * getSPARQL() returns the query "CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}", one could bind the "s" variables
	 * by returning "new HashMap(){{put("s","http://www.exemple.com/ontolohy#123456");}};" 
	 * 
	 * @return the bindings that will be set on the SPARQL query
	 */
	public Map<String, Value> getBindings();
	
	/**
	 * Tells if the sparql query should be executed including the inferred statements or not.
	 * If this returns null, the default behavior of the SPARQLExecuterIfc will be used.
	 * 
	 * @return
	 */
	public Boolean isIncludeInferred();
	
	/**
	 * Returns the set of URIs (as java.net.URI) that will constitute the default graph in which the query of this
	 * helper will be executed. Return null to specify no default graph.
	 * 
	 * @return
	 */
	public Set<URI> getDefaultGraphs();
	
	/**
	 * Returns the set of URIs (as java.net.URI) that will be the named graphs of the query of this
	 * helper. Return null to specify no named graphs.
	 * 
	 * @return
	 */
	public Set<URI> getNamedGraphs();
	
	
	/**
	 * Returns the default graph in which insertion from this SPARQL query will be made (using
	 * the INSERT keyword).
	 * 
	 * @return
	 */
	public URI getDefaultInsertGraph();

	/**
	 * Returns the set of URIs (as java.net.URI) in which the deletions from this SPARQL query
	 * (using DELETE keyword) will be made. Return null to specify the default graph.
	 * 
	 * @return
	 */
	public Set<URI> getDefaultRemoveGraphs();
}