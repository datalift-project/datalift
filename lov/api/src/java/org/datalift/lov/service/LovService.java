package org.datalift.lov.service;


/**
 * Abstract class that defines a Lov Service.
 * 
 * @author freddy
 *
 */
public abstract class LovService
{
	public abstract void checkLovData();
	public abstract String search(SearchQueryParam params);
	public abstract String check(CheckQueryParam params);
	public abstract String vocabs();
}
