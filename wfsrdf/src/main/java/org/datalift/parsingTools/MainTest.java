package org.datalift.parsingTools;



public class MainTest {
	
	public static void main(String[] args)
	{
		WfsParser webParser=new WfsParser();
		webParser.getDataWFS("http://127.0.0.1:8081/geoserver/wfs?REQUEST=GetCapabilities&version=1.0.0");
	}


}
