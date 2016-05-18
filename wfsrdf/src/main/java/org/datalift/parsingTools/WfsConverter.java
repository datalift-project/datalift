package org.datalift.parsingTools;

import java.util.ArrayList;

import fr.ign.datalift.model.AbstractFeature;

public class WfsConverter {
	
	private WfsParser wfsParser;
	private String URLWFS="http://127.0.0.1:8081/geoserver/wfs?REQUEST=GetCapabilities&version=1.0.0";
	
	public WfsConverter()
	{
		wfsParser = new WfsParser();
	}
	
	public void ConvertFeaturesToRDF()
	{
		wfsParser.getDataWFS(URLWFS);
		ArrayList<AbstractFeature> features=wfsParser.getFeatures();
		
	}

}
