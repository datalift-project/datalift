package org.datalift.lov;

import org.datalift.fwk.log.Logger;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class Util {
	
	Logger log = Logger.getLogger(Util.class);
	
	public static void CloseQuietly(Object connection){
		
		if(connection != null && connection instanceof RepositoryConnection){
			try {
				((RepositoryConnection) connection).close();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
