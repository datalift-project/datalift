package org.datalift.wfs.wfs2.parsing;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.http.client.ClientProtocolException;
//import org.datalift.core.util.*;
import org.datalift.geoutility.Context;
import org.datalift.geoutility.FeatureTypeDescription;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.model.Store;

import org.xml.sax.SAXException;

public class WFS2Parser {
	private final static Map<String,Store> cache = new HashMap<String, Store>();
	//10, 3 * 3600
	public Store dataStore;
	private String url;

	public WFS2Parser(String sourceUrl) {
		// TODO Auto-generated constructor stub	
		url=sourceUrl;
	}
	private List<ComplexFeature> doParse(String completeUrl) throws SAXException, ParserConfigurationException, IOException
	{
		InputStream in=Context.doGet(completeUrl);		
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema s = sf.newSchema();
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
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
	public static void main (String[] args) throws SAXException, ParserConfigurationException, IOException
	{
		WFS2Parser mp=new WFS2Parser("http://geoservices.brgm.fr/risques?service=WFS&version=1.1.0&request=getCapabilities");
		mp.doParse(mp.url);
	}
	public void getFeatureType(String FeatureName) throws ClientProtocolException, IOException, SAXException, ParserConfigurationException
	{
		Store ds=cache.get(url);
		if(ds==null || ds.getFtParsed==null)
		{
			List<ComplexFeature> ft=doParse(url+"request=getFeature&typename="+FeatureName);
			if(ds==null)
			{
				ds=new Store();
				cache.put(url, ds);
			}
			//Get feature list
			ds.getFtParsed.put(FeatureName, ft);
			this.dataStore.getFtParsed=ds.getFtParsed;
		}
	}
	
	public void getCapabilities() throws ClientProtocolException, IOException, SAXException, ParserConfigurationException
	{
		Store ds=cache.get(url);
		if(ds==null || ds.getCapParsed==null)
		{
			List<ComplexFeature> caps=doParse(url+"?service=wfs&request=getCapabilities");
			//Get feature list
			if(ds==null)
				{
					ds=new Store();
					cache.put(url, ds);
				}
			ds.getCapParsed=caps;	
		}
		this.dataStore.getCapParsed = ds.getCapParsed;
	}
	
	public List<FeatureTypeDescription> getFeatureTypeDescription() {
		// 
		if(this.dataStore!=null && this.dataStore.getCapParsed!=null)
		{
			ComplexFeature fc=this.dataStore.getCapParsed.get(0);
		List<FeatureTypeDescription> types=new ArrayList<FeatureTypeDescription>();
		if(fc!=null)
		{
			//QName name= new QName("http://www.opengis.net/wfs/2.0","FeatureTypeList");
			
			ComplexFeature child=fc.findFirstChild(Const.FeatureTypeList);
			System.out.println(child);
			if(child!=null)
			{
				for (Attribute a : child.itsAttr) {
					if(a instanceof ComplexFeature)
					{
						ComplexFeature ft=(ComplexFeature) a;
						if(ft.getTypeName().equals(Const.FeatureTypeType))
						{
							FeatureTypeDescription ftd=new FeatureTypeDescription();
							ComplexFeature  name= ft.findFirstChild(Const.Name);
							ComplexFeature  abstractt= ft.findFirstChild(Const.Abstract);
							ComplexFeature  title= ft.findFirstChild(Const.Title);
							ComplexFeature defaultsrs= ft.findFirstChild(Const.DefaultCRS);
							List<ComplexFeature> otherSrs= ft.findChildren(Const.OtherCRS);
							if(defaultsrs!=null)
								{	/*int delimeter= defaultsrs.value.lastIndexOf(":");
									String epsgSrs = defaultsrs.value.substring(delimeter+1);*/
									ftd.setDefaultSrs(defaultsrs.value);
								}
							
							if(name!=null)
							{
									ftd.setName(name.value);
							}
							if(abstractt!=null)
							{
								ftd.setSummary(abstractt.value);
							}
							
							if(title!=null)
							{
								ftd.setTitle(title.value);
							}
							if(otherSrs!=null)
							{
								List<String> srss=new ArrayList<String>();
								for (ComplexFeature srs : otherSrs) {
									srss.add(srs.value);
								}
								ftd.setOtherSrs(srss);
							}
							ftd.setNumberFeature(-1);
							types.add(ftd);
						}
					}
					
				}
				
			}
		
		}
		}
		return null;
	}
	
    /******to be moved from here later ****/
	private void buildFeatures(List<ComplexFeature> elements, Context ctx)
	{
		if(elements!=null)
		{
			ComplexFeature fc=elements.get(0);
			processFeatureCollection(fc, ctx);

		}
	}
	private void processFeatureCollection(ComplexFeature fc, Context ctx)
	{
		for (Attribute a : fc.itsAttr) {
			 if(a instanceof ComplexFeature)
			 {
				 
					/* if(a.getTypeName().equals(Const.FeatureArrayPropertyType))
					 {
						 ComplexFeature members =(ComplexFeature)a;
						 processFeatureCollection(members,ctx);

					 }
					 else
					 {*/
						 if(!a.getTypeName().equals(Const.BoundingShapeType)) //to ignore general bounding box if any
							 {
								 ComplexFeature member =(ComplexFeature)a;
								 for (Attribute aa : member.itsAttr) {
									 if(aa instanceof ComplexFeature)
									 {
										 ComplexFeature ef =(ComplexFeature)aa;
										 ctx.getMapper(ef.getTypeName()).map(ef, ctx);
									 }
								 }
							 }
					 }
			}					
			 
		
	}
	
	
	}
