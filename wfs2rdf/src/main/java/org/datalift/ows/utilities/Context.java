package org.datalift.ows.utilities;


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

import org.datalift.ows.exceptions.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.BatchStatementAppender;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.ows.wfs.wfs2.mapping.BaseMapper;
import org.datalift.ows.wfs.wfs2.mapping.GeomMapper;
import org.datalift.ows.wfs.wfs2.mapping.Mapper;

import static org.datalift.fwk.util.PrimitiveUtils.*;

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

/**
 * This class loads some usueful elements needed for the mapping of WFS or SOS response (2.0.0)
 * @author Hanane Eljabiri
 *
 */
public class Context {

	private final static Logger log = Logger.getLogger();
	private final String QUDT_UNITS="qudt-units-1.1.ttl";
	private final String debugOutputFile="target/out/debugOutput.ttl";
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
			try {
				Configuration cfg = Configuration.getDefault();
				boolean debugOutput = Boolean.parseBoolean(
				        cfg.getProperty("ows2rdf.debug.output", "false"));
				List<RDFHandler> handlers = new ArrayList<>(2);
				handlers.add(new BatchStatementAppender(cnx, ctx));
				if (debugOutput) {
				    handlers.add(new TurtleWriter(
				            new FileOutputStream(debugOutputFile)));
				}
				model = new RDFHandlerWrapper(handlers.toArray(new RDFHandler[handlers.size()])) {
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
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
		int count = (hm.containsKey(name))? unwrap(hm.get(name)): 0;
		count++;
		hm.put(name, wrap(count));
		return count;
	}
}
