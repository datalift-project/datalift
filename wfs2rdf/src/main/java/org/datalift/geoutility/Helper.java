package org.datalift.geoutility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.datalift.fwk.log.Logger;

public class Helper {
	//private final static Logger log = Logger.getLogger();
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
		XMLGregorianCalendar d=null ;
		//log.debug("this is the value to be parsed "+value);
		try {
			 d = df.newXMLGregorianCalendar(value);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(value);
		}
		return d;
	}
	public static  boolean isSet(String s)
	{
		if (s==null || s.equals("")) return false;
		return true;
	}
}
