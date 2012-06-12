package org.datalift.s4ac.services;

public class ContextURI {
	private static String base ="http://example.com/context/";
	public static String get(String sessid) {
		return base + sessid;
	}
	
	public static String get() {
		return base;
	}
}
