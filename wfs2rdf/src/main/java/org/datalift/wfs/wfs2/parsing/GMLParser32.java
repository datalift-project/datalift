package org.datalift.wfs.wfs2.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.datalift.geoutility.Context;
import org.datalift.model.ComplexFeature;
import org.xml.sax.SAXException;

public class GMLParser32 {

	public  List<ComplexFeature> doParse(String completeUrl) throws SAXException, ParserConfigurationException, IOException
	{
		InputStream in=Context.doGet(completeUrl);	
		
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
		return handler.getCfList();
	}
}
