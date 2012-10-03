package com.mondeca.datalift.rdf2rdf.model;

import java.util.List;

public class JSONOntologyElements {

	protected List<JSONClass> classes;
	protected List<JSONProperty> properties;
	
	public JSONOntologyElements(List<JSONClass> classes, List<JSONProperty> properties) {
		super();
		this.classes = classes;
		this.properties = properties;
	}

	public List<JSONClass> getClasses() {
		return classes;
	}

	public void setClasses(List<JSONClass> classes) {
		this.classes = classes;
	}

	public List<JSONProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<JSONProperty> properties) {
		this.properties = properties;
	}
	
}
