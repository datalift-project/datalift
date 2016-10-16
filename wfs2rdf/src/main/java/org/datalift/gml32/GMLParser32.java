package org.datalift.gml32;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Helper;

public class GMLParser32 {

	public  ComplexFeature doParse(String completeUrl) throws Exception
	{
		InputStream in=Helper.doGet(completeUrl);			
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI,"org.apache.xerces.jaxp.validation.XMLSchemaFactory",this.getClass().getClassLoader());
		Schema s = sf.newSchema();
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance("org.apache.xerces.jaxp.SAXParserFactoryImpl", this.getClass().getClassLoader());
		saxParserFactory.setNamespaceAware(true);
		saxParserFactory.setValidating(true);
		saxParserFactory.setXIncludeAware(true);
		saxParserFactory.setSchema(s);
		SAXParser saxParser = saxParserFactory.newSAXParser();
		Handler handler = new Handler(saxParser);
		saxParser.parse(in, handler);
		//Get feature list
		return handler.getRoot();
	}
}
