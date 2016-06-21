package org.datalift.parsingTools;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;


import org.apache.velocity.app.VelocityEngine;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;


import fr.ign.datalift.model.AbstractFeature;

public class WfsToRdf /*extends BaseConverterModule*/ {


	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------
	private static Logger log;

	/** The prefix for the URI of the project objects. */
	public final static String PROJECT_URI_PREFIX = "project";
	/** The prefix for the URI of the source objects, within projects. */
	public final static String SOURCE_URI_PREFIX  = "source";

	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "wfstordf";
	
	/***Deprecated****/
	//public final static  String URLWFS= "https://wfspoc.brgm-rec.fr/geoserver/ows?service=wfs&version=2.0.0&request=GetCapabilities";
	//public final static  String URLWFS="http://127.0.0.1:8081/geoserver/wfs?REQUEST=GetCapabilities&version=1.0.0";
	public final static  String URLWFS="http://ogc.geo-ide.developpement-durable.gouv.fr/cartes/mapserv?map=/opt/data/carto/geoide-catalogue/REG042A/JDD.www.map&service=WfS&request=GetCapabilities&version=1.1.0";
									
			//

	private static VelocityEngine engine = null;
	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	//private final static Logger log = Logger.getLogger();

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/** Default constructor. */
	public WfsToRdf() {
		//super(MODULE_NAME, 901, SourceType.ShpSource);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------
	
	@GET
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public String getIndexPage(@QueryParam("project") URI projectId) {
		
		testGet();
		return "well done!";
	}
	public void testGet()
	{
		URI targetGraph;
		try {
			targetGraph = new URI("toto/titi/wfsGraph");
			URI baseUri=new URI("toto/titi/wfsBaseUri");
			String targetType="targetType";
			String src="none";
			URI projectId=new URI("project/toto/titi");
			URI sourceID=new URI("");
			
			this.convertWfsToRdf(projectId,sourceID, "destination_title", targetGraph, baseUri, targetType);
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//this is a temporary prototype of what should be in the future the web service response to a conversion request
	public void convertWfsToRdf(
			 URI projectId,
			 URI sourceId,
			 String destTitle,
			 URI targetGraph,
			 URI baseUri,
			 String targetType
			)
	{
		//1-get the project by its id
		//2-get the resource by its id
		//3-create an instance of wfsParser
		WfsParser parser=new WfsParser();
		//4-call the method getwfsdata of the parser using the wfs URL of the source mentioned above. A list of features is created. 
		//each element of the list contains all the information associated to each feature member of wfs response
		//parser.getDataWFS(URLWFS);
		try {
			parser.tryGetDataStore();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<AbstractFeature> featuresToConvert=parser.getFeatures();
		String crs=parser.getCRs();
		//5-create an instance of wfsConverter 
		WfsConverter converter=new WfsConverter();
		//6-call the method convertFeaturesToRdf of WfsConverter using the list of features created in step 4
		org.datalift.fwk.rdf.Repository target = Configuration.getDefault().getInternalRepository();
		converter.ConvertFeaturesToRDF("les_regions", featuresToConvert,target , targetGraph, baseUri, targetType,crs);
		
	}

}
