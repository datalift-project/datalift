package org.datalift.owl.toolkit;

/**
 * The template vocabulary.
 * 
 * @author thomas
 *
 */
public interface TEMPLATE_VOCABULARY {
	
	/*
	 *	Namespace
	 */
	
	public static final String NS = "http://www.mondeca.com/sesame/toolkit/template#";
	
	/*
	 *	Classes
	 */
	
	public static final String TEMPLATE = NS + "Template";
	
	public static final String ARGUMENT = NS + "Argument";
	
	/*
	 * 	Properties
	 */
	
	public static final String NAME = NS + "name";
	
	public static final String DISPLAYNAME = NS + "displayName";
	
	public static final String BODY = NS + "body";
	
	public static final String HASARGUMENT = NS + "hasArgument";
	
	public static final String VARNAME = NS + "varName";
	
	public static final String MANDATORY = NS + "mandatory";
	
	public static final String ORDER = NS + "order";
	
	public static final String ARGUMENTRANGE = NS + "argumentRange";
	
}
