package org.datalift.lov.service;


public class CheckQueryParam extends LovQueryParam {
	
	private final static String URI = "uri";
	private final static String TIMEOUT = "timeout";
	
	private String uri;
	private int timeout;
	
	public CheckQueryParam(String uri) {
		this(uri, 0);
	}
	
	public CheckQueryParam(String uri, int timeout) {
		this.uri = uri;
		this.timeout = timeout;
	}

	
	@Override
	public String getQueryParameters() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(PARAM_TOKEN);
		sb.append(checkParameter(URI, uri));
		sb.append(checkParameter(TIMEOUT, timeout));
		
		return sb.substring(0, sb.length() - 1);
	}

	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
}
