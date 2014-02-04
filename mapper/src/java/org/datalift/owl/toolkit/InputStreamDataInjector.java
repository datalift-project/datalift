package org.datalift.owl.toolkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

/**
 * Read and load data from an InputStream, that can be a resource in the classpath, a URL, etc.
 * The format of the RDF data to be read must be passed along with the stream.
 * 
 * @author mondeca
 *
 */
public class InputStreamDataInjector implements ProviderListenerIfc {

	private InputStream stream;
	private RDFFormat format;
	private String defaultNamespace = RDF.NAMESPACE;
	
	// the named graph in which to load the data. Can be null
	protected URI namedGraph;

	public InputStreamDataInjector(InputStream stream, RDFFormat format) {
		super();
		this.stream = stream;
		this.format = format;
	}

	/**
	 * Specifies the default namespace for the data to be read. By default it is RDF.NAMESPACE.
	 * 
	 * @param stream
	 * @param format
	 * @param defaultNamespace
	 */
	public InputStreamDataInjector(InputStream stream, RDFFormat format, String defaultNamespace) {
		super();
		this.stream = stream;
		this.format = format;
		this.defaultNamespace = defaultNamespace;
	}

	@Override
	public void afterInit(Repository repository)
	throws RepositoryProviderException {
		try {
			RepositoryConnection connection = repository.getConnection();
			try {
				connection.add(
						this.stream,
						this.defaultNamespace,
						this.format,
						(this.namedGraph != null)?repository.getValueFactory().createURI(this.namedGraph.toString()):null
				);
			} finally {
				ConnectionUtil.closeQuietly(connection);
			}
		} catch (RDFParseException e) {
			throw new RepositoryProviderException("Bad RDF format in stream. "+this.format.getName()+" was expected", e);
		} catch (IOException e) {
			throw new RepositoryProviderException("Cannot read from stream", e);
		} catch (RepositoryException e) {
			throw new RepositoryProviderException(e);
		}
	}

	public URI getNamedGraph() {
		return namedGraph;
	}

	public void setNamedGraph(URI namedGraph) {
		this.namedGraph = namedGraph;
	}	

}
