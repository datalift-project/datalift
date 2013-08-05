package org.datalift.owl.toolkit;

import java.util.Collections;
import java.util.List;

import org.openrdf.repository.Repository;

/**
 * Read and load data from a file passed as a parameter. If the file is actually a
 * directory, it will try to recursively load all the files inside it.
 * 
 * @author mondeca
 *
 */
public class FileDataInjector implements ProviderListenerIfc {

	// les chemins vers les repertoires/dossiers a charger
	private List<String> rdfFiles;
	
	private boolean namedGraphAware = false;
	
	private int numberOfProcessedFiles = 0;
	
	/**
	 * Will load the data from the file/directory referenced by the path passed as parameters.
	 * @param rdfFile
	 */
	public FileDataInjector(String rdfFileorDirectory) {
		this(Collections.singletonList(rdfFileorDirectory));
	}

	/**
	 * Will load the data from every file/directory referenced by the list of paths passed as parameters.
	 * @param rdfFile
	 */
	public FileDataInjector(List<String> rdfFileorDirectories) {
		super();
		this.rdfFiles = rdfFileorDirectories;
	}

	@Override
	public void afterInit(Repository repository) 
	throws RepositoryProviderException {
		FileRepositoryLoader loader = new FileRepositoryLoader(repository);
		loader.setAutoNamedGraphs(this.namedGraphAware);
		loader.load(this.rdfFiles);
		this.numberOfProcessedFiles += loader.getNumberOfProcessedFiles();
	}

	public List<String> getRdfFiles() {
		return rdfFiles;
	}

	public void setRdfFiles(List<String> rdfFiles) {
		this.rdfFiles = rdfFiles;
	}

	/**
	 * Returns the total number of files that were loaded by this DataInjector. This allows to test
	 * that at least one file has been found in the directory for exemple.
	 * 
	 * @return
	 */
	public int getNumberOfProcessedFiles() {
		return numberOfProcessedFiles;
	}

	public boolean isNamedGraphAware() {
		return namedGraphAware;
	}

	/**
	 * Sets whether this data injector should store every loaded file in a separate named graph
	 * built from the file path. Default is false.
	 * @param namedGraphAware
	 */
	public void setNamedGraphAware(boolean namedGraphAware) {
		this.namedGraphAware = namedGraphAware;
	}

}