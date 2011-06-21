package org.datalift.projectmanager;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.persistence.Entity;

import org.datalift.fwk.project.TransformedRdfSource;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

@Entity
@RdfsClass("datalift:TransformedRdfSource")
public class TransformedRdfSourceImpl extends BaseSource implements TransformedRdfSource{

	@RdfProperty("datalift:targetGraph")
	private URI		targetGraph;
	
	public TransformedRdfSourceImpl() {
		super(SourceType.TransformedRdfSource);
	}

	public TransformedRdfSourceImpl(String uri) {
		super(SourceType.TransformedRdfSource, uri);
	}

	@Override
	public SourceType getType() {
		return SourceType.TransformedRdfSource;
	}

	public void setTargetGraph(URI targetGraph) {
		this.targetGraph = targetGraph;
	}

	public URI getTargetGraph() {
		return targetGraph;
	}

	@Override
	public void init(File docRoot, URI baseUri) throws IOException {
		// Nothing to init
	}

}
