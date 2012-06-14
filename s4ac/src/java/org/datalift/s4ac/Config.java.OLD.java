package org.datalift.s4ac;

public final class Config {
	
	public static final String sesameServer = "http://localhost:8080/openrdf-sesame";
	public static final String baseUri = "http://localhost:8080/datalift/s4ac";
	public static final String liftedRep = "lifted";
	public static final String secureRep = "secured";
	public static final String dataRep = "data";
	public static final String internalRep = "internal";
	
	
	public static String getSesameServer() {
		return sesameServer;
	}
	
	public static String getDataRep() {
		return dataRep;
	}

	public static String getInternalRep() {
		return internalRep;
	}
	
	public static String getLiftedRep() {
		return liftedRep;
	}
	
	public static String getSecureRep() {
		return secureRep;
	}

	public static String getBaseUri() {
		return baseUri;
	}
}
