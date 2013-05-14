package org.datalift.lov.service;

import org.datalift.fwk.log.Logger;

/**
 * Abstract class that defines a Lov Service.
 * @author freddy
 *
 */
public abstract class LovService {
	
	protected final static Logger log = Logger.getLogger();

	public abstract String search(SearchQueryParam params);
	public abstract String check(CheckQueryParam params);
}
