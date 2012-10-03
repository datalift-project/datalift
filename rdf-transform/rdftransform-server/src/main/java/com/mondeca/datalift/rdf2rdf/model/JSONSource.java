package com.mondeca.datalift.rdf2rdf.model;

import org.datalift.fwk.project.Source;

public class JSONSource {

	protected String graphURI;
	// Default to empty string
	protected String graphName = "";
	
	public JSONSource(Source source) {
		this.setGraphName(source.getTitle());
		this.setGraphURI(source.getUri());
	}

	public String getGraphName() {
		return graphName;
	}

	public void setGraphName(String graphName) {
		this.graphName = graphName;
	}

	public String getGraphURI() {
		return graphURI;
	}

	public void setGraphURI(String graphURI) {
		this.graphURI = graphURI;
	}
		
	
}
