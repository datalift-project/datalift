package org.datalift.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class Store {
	public List<ComplexFeature> getCapParsed;
	public Map <String, List<ComplexFeature>> getFtParsed;
	public Store()
	{
		getFtParsed=new HashMap<String, List<ComplexFeature>>(); 
	}
}
