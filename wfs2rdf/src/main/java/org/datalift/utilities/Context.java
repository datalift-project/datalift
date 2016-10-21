package org.datalift.utilities;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.namespace.QName;

import org.datalift.exceptions.TechnicalException;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.BatchStatementAppender;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.datalift.wfs.wfs2.mapping.GeomMapper;
import org.datalift.wfs.wfs2.mapping.Mapper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.sail.memory.MemoryStore;



public class Context {

	private final static Logger log = Logger.getLogger();
	private final String QUDT_UNITS="qudt-units-1.1.ttl";
	public static URI DefaultSubjectURI = null;
	public RDFHandler model;
	public ValueFactory vf;
	//attributes needs to avoid storing statements in memory
	/*****attributes for counting occurrences****/
	public Map<QName, Integer> hm;
	public Map<String, Resource> codeListOccurences;
	/*****registered mappers and code list****/
	public Map<QName,Mapper> mappers = new HashMap<QName,Mapper>();
	public static List <String>registredCodeList = new ArrayList<String>();
	/****handle cross referencing using gml ids*/
	public Map<String,String> referenceCatalogue;
	public final String baseURI="http://changeMe.org/";
	/***utils for sos****/
	public Map<QName, String> sosMetaData=new HashMap<QName, String>();
	public Map<String, String> unitsSymbUri=new TreeMap<String, String>();
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
	public static final String nsw3Time="http://www.w3.org/2006/time#";
	public static final String nsXsd="http://www.w3.org/2001/XMLSchema#";
	public static final String nsgeof="http://www.opengis.net/ont/sf#";
	public static final String nsUnit= "http://qudt.org/vocab/unit#";
	/*****new RDF classe definitions ****/
	public static final QName referencedObjectType=new QName(nsDatalift, "ReferencedObject");
	public static final QName referencedCodeListType=new QName(nsDatalift, "ReferencedCodeList");
	public static final QName observationType=new QName(nsOml, "Observation");
	public URI rdfTypeURI;
	public static List <String> codeList = new ArrayList<String>();
	static
	{
		InputStream is=null;
		try {
			Properties prop = new Properties();
			String propFileName = "codeList.properties";
			is = CreateGeoStatement.class.getClassLoader().getResourceAsStream(propFileName);
			if (is != null) {
				prop.load(is);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
			for (Object k : prop.keySet()) {
				if (k instanceof String) {
					String code = (String)k;
					codeList.add(prop.getProperty(code));
				}
			}
		} catch (Exception e) {
			log.error("An error has occured while attempting to  load the properties file of predefined code list " + e);
		} finally {
			if(is!=null)
				{
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		}
	}
	public Context(Repository target, java.net.URI targetGraph)	{
		try {
		hm=new HashMap <QName,Integer>();
		codeListOccurences=new HashMap <String,Resource>();
		final RepositoryConnection cnx = target.newConnection();
		vf = new UriCachingValueFactory(cnx.getValueFactory());
		URI ctx = null;
        // Clear target named graph, if any.
        if (targetGraph != null) {
            ctx = vf.createURI(targetGraph.toString());
            cnx.clear(ctx);
        }
		model = new RDFHandlerWrapper(new BatchStatementAppender(cnx, ctx),
									  new TurtleWriter(new FileOutputStream("C:/Users/A631207/Documents/my_resources/emf1.ttl"))){
			@Override
			public void endRDF() throws RDFHandlerException {
				super.endRDF();
				try {
					cnx.close();
				}
				catch (RepositoryException e) {
					throw new RDFHandlerException(e);
				}
			}
		};
		model.handleNamespace("dl_ef", nsDatalift);
		model.handleNamespace("pjt", nsProject);
		model.handleNamespace("ign", nsIGN);
		model.handleNamespace("geo", nsGeoSparql);
		model.handleNamespace("rdf", nsRDF2);
		model.handleNamespace("rdfs", nsRDFS);
		model.handleNamespace("foaf", nsFoaf);
		model.handleNamespace("dcterms", nsDcTerms);
		model.handleNamespace("skos", nsSkos);
		model.handleNamespace("oml", nsOml);
		model.handleNamespace("tp", nsIsoTP);
		model.handleNamespace("smod", nsSmod);
		model.handleNamespace("time", nsw3Time);
		model.handleNamespace("xsd", nsXsd);
		model.handleNamespace("geof", nsgeof);
		model.handleNamespace("unit", nsUnit);
		rdfTypeURI=vf.createURI(nsRDF2+"type");
		DefaultSubjectURI=vf.createURI(nsProject+"root");
		/***Initialize default mappers****/
		mappers.put(null, new BaseMapper());
		mappers.put(new QName("geometry"),new GeomMapper());
		this.referenceCatalogue = new HashMap<String, String>();
		this.referenceCatalogue.put(null, baseURI);
		//initialize bindings units table
		try {
			this.uploadQdut();
		} catch (Exception e) {
			log.warn("error downloading QDUT ontology");
		}
		}
		catch (Exception e) {
			throw new TechnicalException(e);
		}
	}
	

	private void uploadQdut() throws Exception {
		org.openrdf.repository.Repository r = new SailRepository(new MemoryStore());
		r.initialize();
		RepositoryConnection cnx = r.getConnection();
		cnx.add(Context.class.getClassLoader().getResourceAsStream(QUDT_UNITS),
				"", Rio.getParserFormatForFileName(QUDT_UNITS));
		TupleQueryResult rs = cnx.prepareTupleQuery(QueryLanguage.SPARQL,
				"PREFIX qudt:    <http://qudt.org/schema/qudt#> " +
						"PREFIX unit:    <http://qudt.org/vocab/unit#> " +
						"SELECT DISTINCT ?s ?u WHERE { " +
						"?u a ?t ; qudt:symbol ?s " +
						"FILTER(! STRENDS(STR(?t), \"PrefixUnit\"))" +
				"}").evaluate();
		while (rs.hasNext()) {
			BindingSet b = rs.next();
			String symbol = b.getValue("s").stringValue();
			String uri    = b.getValue("u").stringValue();
			unitsSymbUri.put(symbol, uri);
		}
		cnx.close();
		r.shutDown();
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
//	public void exportTtl(String filePath) throws FileNotFoundException, RDFHandlerException
//	{
//		FileOutputStream out = new FileOutputStream(filePath);
//
//		Rio.write(model, out,RDFFormat.TURTLE);
//	}	
//	public boolean exportTS(Repository target, java.net.URI targetGraph, java.net.URI baseUri, String targetType) {
//		final RepositoryConnection cnx = target.newConnection();
//		org.openrdf.model.URI ctx = null;
//
//		try {
//			final ValueFactory vf =
//					new UriCachingValueFactory(cnx.getValueFactory());
//
//			// Clear target named graph, if any.
//			if (targetGraph != null) {
//				ctx = vf.createURI(targetGraph.toString());
//				cnx.clear(ctx);
//			}
//			// Create URIs for subjects and predicates.
//			if (baseUri == null) {
//				baseUri = targetGraph;
//			}
//			String sbjUri  = RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '/');
//			//"http://localhost:9091/initkiosques/regions-nouvelles-shp/";
//
//			String typeUri = RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '#');
//			//"http://localhost:9091/initkiosques/regions-nouvelles-shp#";
//
//
//			org.openrdf.model.URI rdfType = null;
//			try {
//				// Assume target type is an absolute URI.
//				rdfType = vf.createURI(targetType);
//			}
//			catch (Exception e) {
//				// Oops, targetType is a relative URI. => Append namespace URI.
//				rdfType = vf.createURI(typeUri, targetType);
//			}
//
//			long startTime = System.currentTimeMillis();
//			long duration = -1L;
//			long statementCount = 0L;
//			int  batchSize = Env.getRdfBatchSize();
//
//			try {
//				// Prevent transaction commit for each triple inserted.
//				cnx.begin();
//			}
//			catch (RepositoryException e) {
//				throw new RuntimeException("RDF triple insertion failed", e);
//			}
//
//			for (Statement at:model){
//				try {
//					cnx.add(at, ctx);
//					// Commit transaction according to the configured batch size.
//					statementCount++;
//					if ((statementCount % batchSize) == 0) {
//						cnx.commit();
//						cnx.begin();
//					}
//				}
//				catch (RepositoryException e) {
//					throw new RuntimeException("RDF triple insertion failed", e);
//				}
//			}
//			try {
//				cnx.commit();
//				duration = System.currentTimeMillis() - startTime;
//			}
//			catch (RepositoryException e) {
//				throw new RuntimeException("RDF triple insertion failed", e);
//
//			}
//		}
//		catch (TechnicalException e) {
//			throw e;
//
//		}
//		catch (Exception e) {
//			try {
//				// Forget pending triples.
//				cnx.rollback();
//				// Clear target named graph, if any.
//				if (ctx != null) {
//					cnx.clear(ctx);
//				}
//			}
//			catch (Exception e2) { /* Ignore... */ }
//
//			throw new TechnicalException("wfs.conversion.failed", e);
//		}
//		finally {
//			// Commit pending data (including graph removal in case of error).
//			try { cnx.commit(); } catch (Exception e) { /* Ignore... */}
//			// Close repository connection.
//			try { cnx.close();  } catch (Exception e) { /* Ignore...  */}
//		}			
//		return true; //other cases to be handled later...
//	}
}
