package org.datalift.owl.toolkit;

/**
 * An extension of SPARQLHelperIfc to execute SPARQL "ASK" queries.
 * <p/>This is to be used with SesameSPARQLExecuter. Usage exemple :
 * <code>
 * RepositoryProviderIfc provider = ...;
 * AskSPARQLHelper helper = ...;
 * boolean result = SesameSPARQLExecuter.newExecuter(provider).executeAsk(helper);
 * </code>
 * 
 * @author mondeca
 *
 */
public interface AskSPARQLHelper extends SPARQLHelperIfc {
	
}
