package org.datalift.geoutility;

import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.TimeUtils.asSeconds;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.xml.namespace.QName;



import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import org.apache.http.impl.client.HttpClientBuilder;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.UriBuilder;
import org.datalift.model.Const;
import org.datalift.wfs.TechnicalException;
import org.datalift.wfs.wfs2.mapping.AnyURIMapper;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.datalift.wfs.wfs2.mapping.GeomMapper;
import org.datalift.wfs.wfs2.mapping.Mapper;
import org.datalift.wfs.wfs2.mapping.MobileMapper;
import org.datalift.wfs.wfs2.mapping.SimpleTypeMapper;
import org.datalift.wfs.wfs2.mapping.StatementSaver;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;



public class Context {

	//private final static Logger log = Logger.getLogger();

	public static URI DefaultSubjectURI = null;
	public Model model;
	public ValueFactory vf = new ValueFactoryImpl();
	//attributes needs to avoid storing statements in memory
	public StatementSaver saver;
	/*****attributes for counting occurences****/
	public Map<QName, Integer> hm;
	public Map<String, Resource> codeListOccurences;
	/*****registred mappers and code list****/
	public Map<QName,Mapper> mappers = new HashMap<QName,Mapper>();
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
//	static {
//		mappers.put(null, new BaseMapper());
//		mappers.put(new QName("geometry"),new GeomMapper());
//	}
	public Context()
	{
		saver= new StatementSaver();
		
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
		/***Initialize default mappers****/
		Mapper m=new SimpleTypeMapper();
		mappers.put(null, new BaseMapper());
		mappers.put(new QName("geometry"),new GeomMapper());
		mappers.put(Const.string, m);
		mappers.put(Const.xsdDate, m);
		mappers.put(Const.xsdDouble, m);
		mappers.put(Const.xsdFloat, m);
		mappers.put(Const.xsdInteger, m);
		mappers.put(Const.xsdDecimal, m);
		mappers.put(Const.xsdBoolan,new MobileMapper());
		mappers.put(Const.anyURI,new AnyURIMapper());
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
//			 InputStream in = new FileInputStream("src/main/resources/sos_capabilities.xml");
//		     return in;
	}

	public boolean exportTS(Repository target, java.net.URI targetGraph, java.net.URI baseUri, String targetType) {
		final UriBuilder uriBuilder = Configuration.getDefault()
				.getBean(UriBuilder.class);
		final RepositoryConnection cnx = target.newConnection();
		org.openrdf.model.URI ctx = null;

		try {
			final ValueFactory vf =
					new UriCachingValueFactory(cnx.getValueFactory());

			// Clear target named graph, if any.
			if (targetGraph != null) {
				ctx = vf.createURI(targetGraph.toString());
				cnx.clear(ctx);
			}
			// Create URIs for subjects and predicates.
			if (baseUri == null) {
				baseUri = targetGraph;
			}
			String sbjUri  = RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '/');
			//"http://localhost:9091/initkiosques/regions-nouvelles-shp/";

			String typeUri = RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '#');
			//"http://localhost:9091/initkiosques/regions-nouvelles-shp#";


			org.openrdf.model.URI rdfType = null;
			try {
				// Assume target type is an absolute URI.
				rdfType = vf.createURI(targetType);
			}
			catch (Exception e) {
				// Oops, targetType is a relative URI. => Append namespace URI.
				rdfType = vf.createURI(typeUri, targetType);
			}

			long startTime = System.currentTimeMillis();
			long duration = -1L;
			long statementCount = 0L;
			int  batchSize = Env.getRdfBatchSize();

			try {
				// Prevent transaction commit for each triple inserted.
				cnx.begin();
			}
			catch (RepositoryException e) {
				throw new RuntimeException("RDF triple insertion failed", e);
			}

			for (Statement at:model){
				try {
					cnx.add(at, ctx);

					// Commit transaction according to the configured batch size.
					statementCount++;
					if ((statementCount % batchSize) == 0) {
						cnx.commit();
						cnx.begin();
					}
				}
				catch (RepositoryException e) {
					throw new RuntimeException("RDF triple insertion failed", e);
				}
			}
			try {
				cnx.commit();
				duration = System.currentTimeMillis() - startTime;
			}
			catch (RepositoryException e) {
				throw new RuntimeException("RDF triple insertion failed", e);

			}

//			log.info("Inserted {} RDF triples into <{}> in {} seconds",
//					wrap(statementCount), targetGraph,
//					wrap(asSeconds(duration)));
		}
		catch (TechnicalException e) {
			throw e;

		}

		catch (Exception e) {
			try {
				// Forget pending triples.
				cnx.rollback();
				// Clear target named graph, if any.
				if (ctx != null) {
					cnx.clear(ctx);
				}
			}
			catch (Exception e2) { /* Ignore... */ }

			throw new TechnicalException("wfs.conversion.failed", e);
		}
		finally {
			// Commit pending data (including graph removal in case of error).
			try { cnx.commit(); } catch (Exception e) { /* Ignore... */}
			// Close repository connection.
			try { cnx.close();  } catch (Exception e) { /* Ignore...  */}
		}			
		return true; //other cases to be handled later...


	}

}
