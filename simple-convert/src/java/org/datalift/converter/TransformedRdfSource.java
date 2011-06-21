package org.datalift.converter;

import java.net.URI;

import org.datalift.fwk.project.Source;

import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;

public class TransformedRdfSource implements Source{

	@RdfId
	private String	uri;
	@RdfProperty("title")
	private String	title;
	@RdfProperty("targetGraph")
	private URI		targetGraph;
	
	public TransformedRdfSource() {
		
	}

	public TransformedRdfSource(String uri) {
		this.uri = uri;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public SourceType getType() {
		return null;
	}

	@Override
	public String getUri() {
		return uri;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	public void setTargetGraph(URI targetGraph) {
		this.targetGraph = targetGraph;
	}

	public URI getTargetGraph() {
		return targetGraph;
	}

}
