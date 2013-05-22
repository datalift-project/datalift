package org.datalift.owl.toolkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Provides a basic in-memory Sesame repository. There is a flag to activate
 * RDFS inference if needed.
 * @author mondeca
 *
 */
public class LocalMemoryRepositoryProvider extends AbstractRepositoryProvider implements RepositoryProviderIfc {
	
	private boolean rdfsAware;
	private boolean rdfsWithDirectTypeAware;

	public LocalMemoryRepositoryProvider() {
		super();
	}
	
	/**
	 * Returns an initialized provider, with no data loaded.
	 * @return
	 */
	public static LocalMemoryRepositoryProvider initNewProvider() 
	throws RepositoryProviderException {
		LocalMemoryRepositoryProvider provider = new LocalMemoryRepositoryProvider();
		provider.init();
		return provider;
	}
	
	/**
	 * Returns an initialized provider, with data loaded from the given file path.
	 * @return
	 */
	public static LocalMemoryRepositoryProvider initNewProvider(String rdfFileOrDirectory) 
	throws RepositoryProviderException {
		LocalMemoryRepositoryProvider provider = new LocalMemoryRepositoryProvider();
		provider.addListener(new FileDataInjector(rdfFileOrDirectory));
		provider.init();
		return provider;
	}
	
	/**
	 * Returns an initialized provider, with data loaded from the given input stream of the given format
	 * @return
	 */
	public static LocalMemoryRepositoryProvider initNewProvider(
			InputStream dataStream,
			RDFFormat format,
			String defaultNamespace
	) throws RepositoryProviderException {
		LocalMemoryRepositoryProvider provider = new LocalMemoryRepositoryProvider();
		provider.addListener(new InputStreamDataInjector(dataStream, format, defaultNamespace));
		provider.init();
		return provider;
	}
	
	/**
	 * Returns an initialized provider, with data loaded from the given url
	 * @return
	 */
	public static LocalMemoryRepositoryProvider initNewProvider(URL url) 
	throws RepositoryProviderException {
		try {
			return initNewProvider(
					url.openStream(),
					RDFFormat.forFileName(url.toString()),
					// TODO : prendre en compte le cas des URL avec des #
					url.toString().substring(0,url.toString().lastIndexOf('/')+1)
			);
		} catch (IOException e) {
			throw new RepositoryProviderException(e);
		}
	}
	
	@Override
	protected Repository doInit() throws RepositoryProviderException {
		// init repository
		Repository repository = null;
		try {
			if(this.rdfsAware && this.rdfsWithDirectTypeAware) {
				// TODO : log a message
			}
			
			if(rdfsWithDirectTypeAware) {
				repository = new SailRepository(new DirectTypeHierarchyInferencer(new MemoryStore()));
			} else if(this.rdfsAware) {
				repository = new SailRepository(new ForwardChainingRDFSInferencer(new MemoryStore()));
			} else {
				repository = new SailRepository(new MemoryStore());
			}
			
			repository.initialize();
		} catch (RepositoryException e) {
			throw new RepositoryProviderException("Repository config exception", e);
		}
		return repository;	
	}
	
	public boolean isRdfsAware() {
		return rdfsAware;
	}

	public void setRdfsAware(boolean rdfsAware) {
		this.rdfsAware = rdfsAware;
	}

	public boolean isRdfsWithDirectTypeAware() {
		return rdfsWithDirectTypeAware;
	}

	public void setRdfsWithDirectTypeAware(boolean rdfsWithDirectTypeAware) {
		this.rdfsWithDirectTypeAware = rdfsWithDirectTypeAware;
	}

}