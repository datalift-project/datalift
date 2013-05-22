package org.datalift.owl.toolkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrdf.repository.Repository;

/**
 * An abstract implementation of a repository provider that delegates initialization of the repository itself to subclasses.
 * This class takes care of keeping the repository in a variable and handling a list of listeners triggered after initialization.
 * @author Mondeca
 */
public abstract class AbstractRepositoryProvider implements RepositoryProviderIfc {

	// le repository
	protected Repository repository;
	
	// listeners
	private List<ProviderListenerIfc> listeners;
	
	@Override
	public void init() throws RepositoryProviderException {
		// create an initialized repository
		this.repository = doInit();
		// triggers the listeners if any - to load data in the repository if needed.
		if(this.listeners != null) {
			for (ProviderListenerIfc aListener : this.listeners) {
				aListener.afterInit(this.repository);
			}
		}
	}
	
	/**
	 * Delegates repository initialization to subclasses
	 * @return
	 * @throws RepositoryProviderException
	 */
	protected abstract Repository doInit()
	throws RepositoryProviderException;

	/**
	 * A a single listener to the list of listeners of this provider
	 * @param injector
	 */
	public void addListener(ProviderListenerIfc listener) {
		if(this.listeners == null) {
			this.listeners = new ArrayList<ProviderListenerIfc>(Arrays.asList(new ProviderListenerIfc[] {listener}));
		} else {
			this.listeners.add(listener);
		}
	}	
	
	public List<ProviderListenerIfc> getListeners() {
		return listeners;
	}

	public void setListeners(List<ProviderListenerIfc> listeners) {
		this.listeners = listeners;
	}
	
	/**
	 * @deprecated for retro-compatibility
	 */
	public void setDataInjector(ProviderListenerIfc listener) {
		setListeners(Arrays.asList(new ProviderListenerIfc[]{listener}));
	}

	public Repository getRepository() {
		return repository;
	}

}
