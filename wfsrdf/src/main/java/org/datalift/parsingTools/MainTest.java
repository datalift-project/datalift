package org.datalift.parsingTools;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.datalift.core.DefaultConfiguration;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.DefaultUriBuilder;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class MainTest {
	
	public static void main(String[] args)
	{	
		Logger log = Logger.getLogger();
		log.trace(">>>>> Coucou");
		initDataliftConfig();

		URI targetGraph;
		try {
			targetGraph = new URI("http://localhost:9091/project/initkiosques/source/regions-nouvelles-shp-1");
			URI baseUri=new URI("http://localhost:9091/initkiosques/regions-nouvelles-shp");
			String targetType="regions-nouvelles-shp";
			String src="none";
			URI projectId=new URI("project/toto/titi");
			URI sourceID=new URI("");
			
			
			WfsToRdf wfsToRdf=new WfsToRdf();
			wfsToRdf.convertWfsToRdf(projectId,sourceID, "destination_title", targetGraph, baseUri, targetType);
			System.out.println("done");
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*try {
			tryGetDataStore();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}
	
	public static void tryGetDataStore() throws IOException
	{
		//String getCapabilities = "http://ogc.geo-ide.developpement-durable.gouv.fr/cartes/mapserv?map=/opt/data/carto/geoide-catalogue/REG042A/JDD.www.map&service=WfS&request=GetCapabilities&version=1.1.0";
		String getCapabilities = "http://ows.region-bretagne.fr/geoserver/rb/wfs?service=wfs&request=getcapabilities&version=1.0.0";
		
		//String getCapabilities = "http://cartographie.aires-marines.fr/wfs?service=wfs&request=getcapabilities&version=2.0.0";
			Map connectionParameters = new HashMap();
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilities );
		connectionParameters.put("WFSDataStoreFactory:WFS_STRATEGY", "geoserver");

		// Step 2 - connection
		DataStore data = DataStoreFinder.getDataStore( connectionParameters );

		// Step 3 - discouvery
		String typeNames[] = data.getTypeNames();
		String typeName = typeNames[0];
		SimpleFeatureType schema = data.getSchema( typeName );

		// Step 4 - target
		SimpleFeatureSource source = data.getFeatureSource(typeName);
		//System.out.println( "Metadata Bounds:"+ source.getBounds() );

		// Step 5 - query
		

		Query query = new Query(  );
		 SimpleFeatureCollection fc = source.getFeatures(query); //describeft
		    SimpleFeatureIterator  fiterator=fc.features();
		    while(fiterator.hasNext()){
		    	SimpleFeature sf = fiterator.next();
		    	
		    	System.out.println(sf.getName());		         
		    }
		   
	}

	public final static String REPOSITORY_URIS = "datalift.rdf.repositories";
	private final static String RDF_STORE = "test";
	/** The property suffix for repository URL. */
	public final static String REPOSITORY_URL           = ".repository.url";
	   
	/** The property suffix for repository default flag. */
	public final static String REPOSITORY_DEFAULT_FLAG  = ".repository.default";
	/** The property to define the private file storage directory path. */
	public final static String PRIVATE_STORAGE_PATH =
	                                            "datalift.private.storage.path";
	public static void initDataliftConfig()
	{
		// Configure Datalift with an in-memory RDF store.
       Properties config = new Properties();
       config.put(REPOSITORY_URIS, RDF_STORE + ", internal");
       config.put(RDF_STORE + REPOSITORY_URL, "http://localhost:9091/openrdf-sesame/repositories/lifted");
       config.put(RDF_STORE + REPOSITORY_DEFAULT_FLAG, "true");
       config.put("internal" + REPOSITORY_URL, "http://localhost:9091/openrdf-sesame/repositories/internal");
       config.put(PRIVATE_STORAGE_PATH, ".");
       org.datalift.core.DefaultConfiguration cfg = new DefaultConfiguration(config);
       Configuration.setDefault(cfg);
       cfg.init();
       cfg.registerBean(new DefaultUriBuilder());
	}
}
