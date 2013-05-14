package org.datalift.owl.toolkit;

/**
 * An exception that indicates that an error occurred during the initialisation of a Repository
 * by a RepositoryProvider.
 * 
 * @author mondeca
 *
 */
@SuppressWarnings("serial")
public class RepositoryProviderException extends Exception {

	public RepositoryProviderException(String msg) {
		super(msg);
	}

	public RepositoryProviderException(Throwable cause) {
		super(cause);
	}

	public RepositoryProviderException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}