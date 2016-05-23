package org.datalift.parsingTools;

import java.net.URI;
import java.util.ArrayList;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.geomrdf.BaseConverterModule;

import fr.ign.datalift.model.AbstractFeature;

public class WfsToRdf extends BaseConverterModule {


	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------


	/** The prefix for the URI of the project objects. */
	public final static String PROJECT_URI_PREFIX = "project";
	/** The prefix for the URI of the source objects, within projects. */
	public final static String SOURCE_URI_PREFIX  = "source";

	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "wfstordf";
	
	/***Deprecated****/
	public final static  String URLWFS="http://127.0.0.1:8081/geoserver/wfs?REQUEST=GetCapabilities&version=1.0.0";

	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	//private final static Logger log = Logger.getLogger();

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/** Default constructor. */
	public WfsToRdf() {
		super(MODULE_NAME, 901, SourceType.ShpSource);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------
	//this is a temporary prototype of what should be in the future the web service responding to a conversion request
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
		parser.getDataWFS(URLWFS);
		ArrayList<AbstractFeature> featuresToConvert=parser.getFeatures();
		//5-create an instance of wfsConverter 
		WfsConverter converter=new WfsConverter();
		//6-call the method convertFeaturesToRdf of WfsConverter using the list of features created in step 4
		converter.ConvertFeaturesToRDF("les_régions", featuresToConvert, Configuration.getDefault().getInternalRepository(), targetGraph, baseUri, targetType);
		
	}
}
