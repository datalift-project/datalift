package org.datalift.utilities;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.datalift.fwk.log.Logger;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;

public class Helper {
	//private final static Logger log = Logger.getLogger();
	private final static DatatypeFactory df;
	private final static Logger log = Logger.getLogger();
	
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
	public static String capitalize(String className)
	{
		return className.substring(0, 1).toUpperCase() + className.substring(1);
	}
	
	public static boolean isEmpty(ComplexFeature f)
	{
		boolean empty=false;
		if(!Helper.isSet(f.value))
			{
				empty=true;
			}
		for (Attribute a : f.itsAttr) {
			if(!a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.nil)&& !a.name.equals(Const.nilReason))
			{
				return false;		 
			}
		}
		return empty;
	}
	public static InputStream doGet(String getUrl) throws Exception
	{
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(getUrl);
		// add request header
		//request.addHeader("User-Agent", USER_AGENT);
		HttpResponse response = client.execute(request);
		int responseCode=response.getStatusLine().getStatusCode();
		if(responseCode!=200)
		{
			log.error("error getting http response from the distant server using the url : "+getUrl);
			throw new Exception();
		}
		return response.getEntity().getContent();
		//			 InputStream in = new FileInputStream("src/main/resources/sos_capabilities.xml");
		//		     return in;
	}
}
