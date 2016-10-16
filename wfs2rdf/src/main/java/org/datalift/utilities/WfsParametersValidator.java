package org.datalift.utilities;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.datalift.fwk.log.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;

public class WfsParametersValidator {

	/*
	 * check if the wfs url, version and strategy lead to a correct service
	 * 
	 */
	private final static Logger log = Logger.getLogger();
	public static boolean checkWfsSource(String url, String version, String strategyServer)
	{
		//checking TODO later ...
    	String getCapabilities = url;  
		Map connectionParameters = new HashMap();
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilities );
		if(!version.equals("2.0.0") && !strategyServer.equals("none"))
			//then take into account care about the strategy type
		{
			connectionParameters.put("WFSDataStoreFactory:WFS_STRATEGY", strategyServer);
		}
			
		
		//map.put (WFSDataStoreFactory.URL.key, "....");

		// try connection to the server with given parameters
		try {
			DataStore data = DataStoreFinder.getDataStore( connectionParameters );
			if (data==null)
			{
				log.warn("The connection to WFS server failed ! Bad parameters : URL {} and/or version {} and/or strategy {}", url,version, strategyServer);
				return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.warn("The connection to WFS server {} failed", url);
			log.error(e.getMessage());
			return false;
			
		}

		return true;
	}
}
