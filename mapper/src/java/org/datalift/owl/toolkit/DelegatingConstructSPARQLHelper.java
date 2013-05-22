package org.datalift.owl.toolkit;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * A concrete implementation of a ConstructSPARQLHelper that associates a SPARQLQueryBuilderIfc with an
 * RDFHandler. This decouples the SPARQL query construction logic from the result processing logic.
 * 
 * @author mondeca
 *
 */
public class DelegatingConstructSPARQLHelper extends ConstructSPARQLHelperBase implements ConstructSPARQLHelper {

	protected SPARQLHelperIfc helper;
	protected RDFHandler delegate;	
	
	/**
	 * Associates a SPARQL helper with a RDFHandler.
	 * 
	 * @param helper the helper that will generate the SPARQL query string and other parameters.
	 * @param delegate the RDFHandler that will process the result of the SPARQL query.
	 */
	public DelegatingConstructSPARQLHelper(
			SPARQLHelperIfc helper,
			RDFHandler delegate) 
	{
		super();
		this.helper = helper;
		this.delegate = delegate;
	}
	
	/**
	 * Convenience method to associate a SPARQL string with a handler.
	 * 
	 * @param sparql
	 * @param delegate
	 */
	public DelegatingConstructSPARQLHelper(
			String sparql,
			RDFHandler delegate) 
	{
		super();
		this.helper = new SPARQLHelper(sparql);
		this.delegate = delegate;
	}
	
	/**
	 * Convenience method to associate a SPARQL builder with a handler.
	 * 
	 * @param builder SPARQL builder
	 * @param delegate
	 */
	public DelegatingConstructSPARQLHelper(
			SPARQLQueryBuilderIfc builder,
			RDFHandler delegate) 
	{
		super();
		this.helper = new SPARQLHelper(builder);
		this.delegate = delegate;
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		this.delegate.endRDF();
	}

	@Override
	public void handleComment(String c) throws RDFHandlerException {
		this.delegate.handleComment(c);
	}

	@Override
	public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
		this.delegate.handleNamespace(arg0, arg1);
	}

	@Override
	public void handleStatement(Statement s) throws RDFHandlerException {
		this.delegate.handleStatement(s);
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		this.delegate.startRDF();
	}
	
	@Override
	public String getSPARQL() {
		return helper.getSPARQL();
	}

	@Override
	public Map<String, Value> getBindings() {
		return helper.getBindings();
	}

	@Override
	public Boolean isIncludeInferred() {
		return helper.isIncludeInferred();
	}

	@Override
	public Set<URI> getNamedGraphs() {
		return helper.getNamedGraphs();
	}

	@Override
	public Set<URI> getDefaultGraphs() {
		return helper.getDefaultGraphs();
	}

	/**
	 * Access to the SPARQL helper
	 * @return
	 */
	public SPARQLHelperIfc getHelper() {
		return helper;
	}

	/**
	 * Access to the handler
	 * @return
	 */
	public RDFHandler getDelegate() {
		return delegate;
	}

}