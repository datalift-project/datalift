package org.datalift.owl.toolkit;

import org.openrdf.rio.RDFHandler;

/**
 * An extension to a Sesame RDFHandler that can return a SPARQL query, and the associated bindings. This
 * interface is capable of generating a SPARQL query, and handle the results from this query.
 * See the base implementation ConstructSPARQLHelperBase that implements empty methods of RDFHandler
 * that are usually not useful.
 * <p/>This is to be used with SesameSPARQLExecuter. Usage exemple :
 * <code>
 * RepositoryProviderIfc provider = ...;
 * ConstructSPARQLHelper helper = ...;
 * SesameSPARQLExecuter.newExecuter(provider).executeConstruct(helper);
 * </code>
 * 
 * @author mondeca
 *
 */
public interface ConstructSPARQLHelper extends RDFHandler, SPARQLHelperIfc {

}