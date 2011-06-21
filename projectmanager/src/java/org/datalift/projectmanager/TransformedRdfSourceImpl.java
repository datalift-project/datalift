package org.datalift.projectmanager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import javax.persistence.Entity;

import org.datalift.fwk.project.TransformedRdfSource;
import org.openrdf.model.Statement;

import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

@Entity
@RdfsClass("datalift:TransformedRdfSource")
public class TransformedRdfSourceImpl extends BaseFileSource<Statement> implements TransformedRdfSource{

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
	public Iterator<Statement> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
