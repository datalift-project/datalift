package org.datalift.owl.toolkit;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class ConnectionUtil {

	/**
	 * Smoothly close a connection to a repository - don't forget to close the connections
	 * into "finally" parts.
	 * 
	 * @param connection
	 */
	public static void closeQuietly(RepositoryConnection connection) {
		if(connection != null) {
			try {
				connection.close();
			} catch (RepositoryException ignore) {ignore.printStackTrace();}
		}
	}
	
}
