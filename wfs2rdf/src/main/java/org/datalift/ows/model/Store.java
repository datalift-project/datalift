package org.datalift.ows.model;

import java.util.HashMap;
import java.util.Map;
/**
 * A store for OGC services response. 
 * The store contains the response of the operation "GetCapabilities", "GetFeatureType" and "GetObservation" 
 * @author Hanane Eljabiri
 *
 */
public class Store {
	public ComplexFeature getCapParsed;
	public Map <String, ComplexFeature> getFtParsed;
	public Store()
	{
		getFtParsed=new HashMap<String, ComplexFeature>(); 
	}
}
