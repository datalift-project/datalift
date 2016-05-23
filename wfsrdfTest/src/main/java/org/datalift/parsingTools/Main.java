package org.datalift.parsingTools;

import java.net.URI;
import java.net.URISyntaxException;

public class Main {
	
	public static void main(String[] args)
	{
		//WfsParser webParser=new WfsParser();
		//webParser.getDataWFS("http://127.0.0.1:8081/geoserver/wfs?REQUEST=GetCapabilities&version=1.0.0");
		
		URI targetGraph;
		try {
			targetGraph = new URI("toto/titi/wfsGraph");
			URI baseUri=new URI("toto/titi/wfsBaseUri");
			String targetType="targetType";
			String src="none";
			URI projectId=new URI("project/toto/titi");
			URI sourceID=new URI("");
			
			
			WfsToRdf wfsToRdf=new WfsToRdf();
			wfsToRdf.convertWfsToRdf(projectId,sourceID, "destination_title", targetGraph, baseUri, targetType);
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
