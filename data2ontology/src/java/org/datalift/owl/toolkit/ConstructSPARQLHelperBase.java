package org.datalift.owl.toolkit;

import org.openrdf.rio.RDFHandlerException;

/**
 * Base implementation of ConstructSPARQLHelper that implements methods not often used,
 * the only methods that you will need to implement is getSparql and handleStatement.
 * 
 * @author mondeca
 *
 */
public abstract class ConstructSPARQLHelperBase extends SPARQLHelperBase implements ConstructSPARQLHelper {

	@Override
	public void endRDF() throws RDFHandlerException {
		// rien
	}

	@Override
	public void handleComment(String arg0) throws RDFHandlerException {
		// rien
	}

	@Override
	public void handleNamespace(String arg0, String arg1)
	throws RDFHandlerException {
		// rien
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		// rien
	}
	
}