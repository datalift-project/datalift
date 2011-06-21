package org.datalift.fwk.project;

import java.net.URI;

public interface TransformedRdfSource extends Source {
	
	public void setTargetGraph(URI targetGraph);
	public URI getTargetGraph();
}
