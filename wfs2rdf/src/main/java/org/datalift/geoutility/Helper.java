package org.datalift.geoutility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Helper {

	private final static DatatypeFactory df;
	
	static {
		try {
			df = DatatypeFactory.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	//extracts the epsg code from the uri or the urn of crs
	//we assume the epsg code is at the end of the uri/urn
	public static String constructSRIDValue(String crsURI) {
		Pattern pp = Pattern.compile("[0-9]+$");
		Matcher m = pp.matcher(crsURI);
		if(m.find()) {
			return m.group();
		}
		return null;
	}
	public static XMLGregorianCalendar getDate(String value) {
		if(value==null) return null;
		XMLGregorianCalendar d = df.newXMLGregorianCalendar(value);
		return d;
	}
}
