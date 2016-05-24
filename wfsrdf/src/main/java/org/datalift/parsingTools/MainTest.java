package org.datalift.parsingTools;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.datalift.core.DefaultConfiguration;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.util.DefaultUriBuilder;

public class MainTest {
	
	public static void main(String[] args)
	{
		initDataliftConfig();
		//WfsParser webParser=new WfsParser();
		//webParser.getDataWFS("http://127.0.0.1:8081/geoserver/wfs?REQUEST=GetCapabilities&version=1.0.0");
		
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
