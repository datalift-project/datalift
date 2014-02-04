package org.datalift.owl.toolkit;

/**
 * An extension of SPARQLHelperIfc to execute SPARQL "INSERT", "DELETE" or "UPDATE" queries.
 * <p/>This is to be used with SesameSPARQLExecuter. Usage exemple :
 * <code>
 * RepositoryProviderIfc provider = ...;
 * UpdateSPARQLHelper helper = ...;
 * SesameSPARQLExecuter.newExecuter(provider).executeUpdate(helper);
 * </code>
 * 
 * @author mondeca
 *
 */
public interface UpdateSPARQLHelper extends SPARQLHelperIfc {
	
}
