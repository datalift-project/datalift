package org.datalift.model;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.xml.parsers.ParserConfigurationException;


import org.apache.http.client.ClientProtocolException;
//import org.datalift.core.util.*;

import org.datalift.model.ComplexFeature;

import org.datalift.model.Store;
import org.datalift.wfs.wfs2.parsing.GMLParser32;
import org.xml.sax.SAXException;


public abstract class BaseServiceClient {

		protected final static Map<String,Store> cache = new HashMap<String, Store>();
		//10, 3 * 3600
		protected Store dataStore;
		protected String baseUrl;
		protected GMLParser32 parser;
		protected  String serviceType;
		public BaseServiceClient(String sourceUrl) {
			// TODO Auto-generated constructor stub	
			baseUrl=sourceUrl;
			parser=new GMLParser32();
		}

	
		public void getCapabilities() throws ClientProtocolException, IOException, SAXException, ParserConfigurationException
		{
			Store ds=cache.get(baseUrl+serviceType);
			if(ds==null || ds.getCapParsed==null )
			{
				ComplexFeature caps=parser.doParse(baseUrl+"?service="+serviceType+"&request=GetCapabilities");
				//Get feature list
				if(ds==null)
				{
					ds=new Store();
					cache.put(baseUrl+serviceType, ds);
				}
				ds.getCapParsed=caps;	
			}
			this.dataStore=ds;
			this.dataStore.getCapParsed = ds.getCapParsed;
		}
		
		public ComplexFeature getUtilData(String typeName)
		{
			ComplexFeature root = dataStore.getFtParsed.get(typeName);
			return root;
		}

}
