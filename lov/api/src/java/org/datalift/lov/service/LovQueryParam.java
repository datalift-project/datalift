package org.datalift.lov.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.datalift.fwk.util.StringUtils;

/**
 * Base class for query parameters.
 * 
 * @author freddy
 *
 */
public abstract class LovQueryParam {
	
	protected final static String PARAM_TOKEN = "?";
	protected final static String SEP = "&";
	
	public abstract String getQueryParameters();
	
	protected final String checkParameter(String paramName, String stringParam) {
		try {
			return ( ! StringUtils.isBlank(stringParam) ) ? paramName + "=" + URLEncoder.encode(stringParam, "UTF-8") + SEP : "";
			
		} catch (UnsupportedEncodingException e) {
			// Nothing to do if UTF-8 isn't supported
			return "";
		}
	}
	
	protected final String checkParameter(String paramName, int intParam) {
		return (intParam != 0) ? paramName + "=" + intParam + SEP : "";
	}
}
