package org.datalift.owl.toolkit;

import org.openrdf.repository.Repository;

/**
 * A repository provider builds and return Sesame repositories.
 * WARNING : the "init" method must be called before "getRepository" 
 * 
 * @author mondeca
 *
 */
public interface RepositoryProviderIfc {
	
	/**
	 * Initialize the provider after all its parameters have been set. Typically
	 * this method is used by Spring after initialisation.
	 */
	public void init() throws RepositoryProviderException;
	
	/**
	 * Returns a Sesame repository correctly configured and loaded with data.
	 * This method  should directly return a Repository object, 
	 * and NOT reconstruct the repository at each call !
	 * @return
	 */
	public Repository getRepository();
	
}