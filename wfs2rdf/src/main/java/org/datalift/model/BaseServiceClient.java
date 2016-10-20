package org.datalift.model;

import java.util.HashMap;
import java.util.Map;

import org.datalift.gml32.GMLParser32;


public abstract class BaseServiceClient {

		protected final static Map<String,Store> cache = new HashMap<String, Store>();
		//10, 3 * 3600
		protected Store dataStore;
		protected String baseUrl;
		protected GMLParser32 parser;
		protected  String serviceType;
		public BaseServiceClient(String sourceUrl) {
			baseUrl=sourceUrl;
			parser=new GMLParser32();
		}

		public void getCapabilities() throws Exception
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
