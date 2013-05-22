package org.datalift.owl.toolkit;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;

/**
 * Base abstract implementation of SPARQLHelperIfc that implements setters and getters
 * for bindings, defaultGraphs, namedGraphs, includedInferred flag, defaultRemoveGraphs
 * and defaultInsertGraphs.
 * 
 * The default is null for all these properties.
 * 
 * @author mondeca
 *
 */
public abstract class SPARQLHelperBase implements SPARQLHelperIfc {

	protected Map<String, Value> bindings = null;
	
	protected Boolean includeInferred = null;
	
	protected Set<URI> defaultGraphs = null;
	
	protected Set<URI> namedGraphs = null;
	
	protected Set<URI> defaultRemoveGraphs = null;
	
	protected URI defaultInsertGraph = null;
	
	
	@Override
	public Map<String, Value> getBindings() {
		return bindings;
	}

	@Override
	public Boolean isIncludeInferred() {
		return includeInferred;
	}

	@Override
	public Set<URI> getDefaultGraphs() {
		return defaultGraphs;
	}

	@Override
	public Set<URI> getNamedGraphs() {
		return namedGraphs;
	}

	public void setBindings(Map<String, Value> bindings) {
		this.bindings = bindings;
	}

	public void setIncludeInferred(Boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	public void setDefaultGraphs(Set<URI> defaultGraphs) {
		this.defaultGraphs = defaultGraphs;
	}

	public void setNamedGraphs(Set<URI> namedGraphs) {
		this.namedGraphs = namedGraphs;
	}

	@Override
	public Set<URI> getDefaultRemoveGraphs() {
		return defaultRemoveGraphs;
	}

	public void setDefaultRemoveGraphs(Set<URI> defaultRemoveGraphs) {
		this.defaultRemoveGraphs = defaultRemoveGraphs;
	}

	@Override
	public URI getDefaultInsertGraph() {
		return defaultInsertGraph;
	}

	public void setDefaultInsertGraph(URI defaultInsertGraph) {
		this.defaultInsertGraph = defaultInsertGraph;
	}

}
