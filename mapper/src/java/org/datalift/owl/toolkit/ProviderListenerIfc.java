package org.datalift.owl.toolkit;

import org.openrdf.repository.Repository;

/**
 * Loads data into a Sesame repository. The data could come from a file, a URL, an XML+XSL, etc.
 * @author mondeca
 *
 */
public interface ProviderListenerIfc {

	/**
	 * Adds some data into the repository passed as a variable. Data can come from an RDF file,
	 * an XML file + an XSL file, remote data from a URL, or any other data source depending on
	 * the implementation.
	 * @param repository
	 * @throws RepositoryProviderException
	 */
	public void afterInit(Repository repository) throws RepositoryProviderException;
	
}