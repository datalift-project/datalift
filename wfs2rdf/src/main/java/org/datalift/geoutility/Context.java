package org.datalift.geoutility;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.datalift.model.Const;
import org.datalift.wfs.wfs2.mapping.AnyTypeMapper;
import org.datalift.wfs.wfs2.mapping.AnyURIMapper;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.datalift.wfs.wfs2.mapping.CodeListMapper;
import org.datalift.wfs.wfs2.mapping.EmfMapper;
import org.datalift.wfs.wfs2.mapping.GeomMapper;
import org.datalift.wfs.wfs2.mapping.Mapper;
import org.datalift.wfs.wfs2.mapping.MobileMapper;
import org.datalift.wfs.wfs2.mapping.ObservationPropertyTypeMapper;
import org.datalift.wfs.wfs2.mapping.ReferenceTypeMapper;
import org.datalift.wfs.wfs2.mapping.StringOrRefTypeMapper;
import org.datalift.wfs.wfs2.mapping.TimePeriodMapper;
import org.datalift.wfs.wfs2.parsing.WFS2Parser;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;


import com.sun.xml.internal.txw2.Document;

public class Context {


	public static URI DefaultSubjectURI = null;
	public Model model;
	public ValueFactory vf = new ValueFactoryImpl();
	/*****attributes for counting occurences****/
	public Map<QName, Integer> hm;
	public Map<String, Resource> codeListOccurences;
	/*****registred mappers and code list****/
	public static Map<QName,Mapper> mappers = new HashMap<QName,Mapper>();
	public static List <String>registredCodeList = new ArrayList<String>();
	//*******ns******//
	public static final String nsDatalift = "http://www.datalift.org/ont/inspire#";
	public static final String nsSmod = "https://www.w3.org/2015/03/inspire/ef#";
	public static final String nsRDF="https://www.w3.org/TR/rdf-schema/";
	public static final String nsProject="http://localhost:9091/project/BRGM/source/testInspire/emf/";
	public static final String nsIGN="http://data.ign.fr/def/geometrie#";
	public static final String nsGeoSparql="http://www.opengis.net/ont/geosparql#";
	public static final String nsRDF2="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String nsRDFS="http://www.w3.org/2000/01/rdf-schema#";
	public static final String nsFoaf="http://xmlns.com/foaf/0.1/";
	public static final String  nsDcTerms ="http://purl.org/dc/terms/";
	public static final String nsSkos="http://www.w3.org/2008/05/skos#";
	public static final String nsOml="http://def.seegrid.csiro.au/ontology/om/om-lite#";
	public static final String nsIsoTP="http://def.seegrid.csiro.au/isotc211/iso19108/2002/temporal#"; //should be replaced by a local version of the ontology  
	/*****new RDF classe definitions ****/
	public static final QName referencedObjectType=new QName(nsDatalift, "ReferencedObject");
	public static final QName referencedCodeListType=new QName(nsDatalift, "ReferencedCodeList");
	public static final QName observationType=new QName(nsOml, "Observation");
	public URI rdfTypeURI;
	static {
		mappers.put(null, new BaseMapper());
		Mapper m=new StringOrRefTypeMapper();
		mappers.put(new QName("geometry"),new GeomMapper());
		mappers.put(Const.string, m);
		mappers.put(Const.StringOrRefType, m);
		mappers.put(Const.ReferenceType, new ReferenceTypeMapper());
		mappers.put(Const.EnvironmentalMonitoringFacilityType, new EmfMapper());
		mappers.put(Const.TimePeriodType, new TimePeriodMapper());
		Mapper m2= new AnyTypeMapper(); 
		mappers.put(Const.anyType,m2);
		mappers.put(Const.AbstractMemberType,m2);
		mappers.put(Const.OM_ObservationPropertyType,new ObservationPropertyTypeMapper());
		mappers.put(Const.bool,new MobileMapper());
		mappers.put(Const.anyURI,new AnyURIMapper());
		mappers.put(Const.inspireCodeList,new CodeListMapper());
		//add codeList to be considered
		registredCodeList.add(Const.clInspire);
		registredCodeList.add(Const.clSandre);
		
	}
	public Context()
	{
		
		hm=new HashMap <QName,Integer>();
		codeListOccurences=new HashMap <String,Resource>();
		vf = new ValueFactoryImpl();
		model = new LinkedHashModel();
		
		model.setNamespace("dl_ef", nsDatalift);
		model.setNamespace("pjt", nsProject);
		model.setNamespace("ign", nsIGN);
		model.setNamespace("geo", nsGeoSparql);
		model.setNamespace("rdf", nsRDF2);
		model.setNamespace("rdfs", nsRDFS);
		model.setNamespace("foaf", nsFoaf);
		model.setNamespace("dcterms", nsDcTerms);
		model.setNamespace("skos", nsSkos);
		model.setNamespace("oml", nsOml);
		model.setNamespace("tp", nsIsoTP);
		model.setNamespace("smod", nsSmod);
		rdfTypeURI=vf.createURI(nsRDF2+"type");
		DefaultSubjectURI=vf.createURI(nsProject+"root");
	}

	public Mapper getMapper(QName type) {
		Mapper m = mappers.get(type);
		return (m != null)? m: mappers.get(null);
	}

	public int getInstanceOccurences(QName name)
	{
		int count=0;
		if(hm.get(name)==null)
		{
			hm.put(name, 1);
			return 1;
		}

		count=hm.get(name);
		count++;
		hm.remove(name);			
		hm.put(name, count);
		return count;

	}
	public void exportTtl(String filePath) throws FileNotFoundException, RDFHandlerException
	{
		FileOutputStream out = new FileOutputStream(filePath);

		Rio.write(model, out,RDFFormat.TURTLE);
	}


//	public static void main(String[] args) throws Exception {
//		// Default conversion
//		String fileExport="C:/Users/A631207/Documents/my_resources/";
//		String fileData="src/main/resources/wfs_response.xml";
//		
//		Map <String,String> piezoToConvert= new HashMap <String,String>();
//		piezoToConvert.put("geoservices_rem_napp_socl.ttl"	,"http://ids.craig.fr/wxs/public/wfs?request=getCapabilities&version=2.0.0");
////			piezoToConvert.put("geoservices_rem_napp_socl.ttl"	,"http://localhost:8081/geoserver/hanane_workspace/ows?service=WFS&version=2.0.0&request=GetFeature&typeName=hanane_workspace:regions_nouvelles_rest");
//	//piezoToConvert.put("geoservices_rem_napp_socl.ttl"	,"http://ids.craig.fr/wxs/public/wfs?request=getfeature&version=2.0.0&typename=public:ARDTA_PNR_2015_GEOFLA");
////		piezoToConvert.put("Piezometre/00463X0036/H1/PZ/2.ttl"	,"http://ressource.brgm-rec.fr/data/Piezometre/00463X0036/H1/PZ/2");
////		piezoToConvert.put("Piezometre/00487X0015/S1/PZ/2.ttl","http://ressource.brgm-rec.fr/data/Piezometre/00487X0015/S1/PZ/2");
////		piezoToConvert.put("Piezometre/00636X0020/P/PZ/2.ttl"	,"http://ressource.brgm-rec.fr/data/Piezometre/00636X0020/P/PZ/2");
////		piezoToConvert.put("Piezometre/06288X0096/SB/PZ/2.ttl"	,"http://ressource.brgm-rec.fr/data/Piezometre/06288X0096/SB/PZ/2");
////		piezoToConvert.put("Piezometre/06987A0186/S/PZ/2.ttl","http://ressource.brgm-rec.fr/data/Piezometre/06987A0186/S/PZ/2");
////		piezoToConvert.put("Piezometre/06993X0087/F6/PZ/2.ttl","http://ressource.brgm-rec.fr/data/Piezometre/06993X0087/F6/PZ/2");
//
//		try {
//			
//			for (String piezo : piezoToConvert.keySet()) {
//				Map<QName,Mapper> mappers = new HashMap<QName,Mapper>();
//				WFS2Parser mp=new WFS2Parser();
//				Context ctx=new Context();
//
//				InputStream in= doGet(piezoToConvert.get(piezo));
//				//mp.doParse(in, ctx);
//				mp.getCapabilities(piezoToConvert.get(piezo), ctx);
//				piezo=piezo.replace("/", "_");
//				ctx.exportTtl(fileExport+piezo);				
//			}
//			
//			//System.out.println(in);
//		} catch (Exception e) {
//			// TODO: handle exception
//			throw new RuntimeException(e);
//		}
//	}
	public static InputStream doGet(String getUrl) throws ClientProtocolException, IOException
	{
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(getUrl);

		// add request header
		//request.addHeader("User-Agent", USER_AGENT);
		HttpResponse response = client.execute(request);

		System.out.println("Response Code : " 
	                + response.getStatusLine().getStatusCode());

		return response.getEntity().getContent();
//		 InputStream in = new FileInputStream("src/main/resources/wfs_response.xml");
//		 return in;
	}
	
}
