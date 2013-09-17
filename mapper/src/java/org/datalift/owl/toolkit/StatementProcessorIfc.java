package org.datalift.owl.toolkit;

import org.openrdf.model.Statement;

/**
 * A StatementProcessorIfc is capable of processing a Statement to modify it if needed.
 * 
 * @author mondeca
 *
 */
public interface StatementProcessorIfc {

	public boolean accept(Statement s);
	
	public Statement process(Statement s);
	
}
