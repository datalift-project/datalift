package org.datalift.owl.toolkit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

/**
 * Loads data from a File, a Directory, or a classpath Resource into the given Repository.
 * 
 * @author mondeca
 *
 */
public class FileRepositoryLoader {

	// if true, generates automatically a named graph for each loaded file based on the file path. (false by default)
	protected boolean autoNamedGraphs = false;
	
	// the named graph in which to load the data. Can be null
	protected URI namedGraph;
	
	// the repository in which we want to load the data
	protected Repository repository;
	
	// the number of files that were processed by this instance
	private int numberOfProcessedFiles = 0;
	
	public FileRepositoryLoader(Repository repository) {
		this.repository = repository;
	}
	
	public void load(List<String> rdfFileorDirectories) {
		if(rdfFileorDirectories != null) {
			for (String anRdf : rdfFileorDirectories) {
				if(anRdf != null) {
					File anRdfFile = new File(anRdf);
					try {
						if(!anRdfFile.exists() && Thread.currentThread().getContextClassLoader().getResource(anRdf) != null) {
							repository.getConnection().add(
									Thread.currentThread().getContextClassLoader().getResource(anRdf),
									// TODO : ici mettre le namespace par defaut comme un parametre ?
									RDF.NAMESPACE,
									// on suppose que c'est du RDF/XML par defaut
									RDFFormat.forFileName(anRdf, RDFFormat.RDFXML),
									(autoNamedGraphs)?repository.getValueFactory().createURI(anRdfFile.toURI().toString()):((this.namedGraph != null)?repository.getValueFactory().createURI(this.namedGraph.toString()):null)
							);
							numberOfProcessedFiles++;
						} else {
							this.loadFileOrDirectory(new File(anRdf), repository, new File(anRdf).toURI());
						}						
					} catch (RDFParseException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (RepositoryException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	
	/**
	 * Charge le RDF contenu dans le fichier. Si le fichier est un répertoire, tente
	 * d'itérer sur tous les sous-fichiers et sosu-répertoires.
	 * 
	 * @param aFileOrDirectory
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	private void loadFileOrDirectory(File aFileOrDirectory, Repository repository, URI context)
	throws RDFParseException, RepositoryException, IOException {
		
		// don't process hidden files or directories
		if(aFileOrDirectory.isHidden()) {
			return;
		}
		
		if(aFileOrDirectory.isDirectory()) {
			for (File f : aFileOrDirectory.listFiles()) {
				try {
					loadFileOrDirectory(f, repository, context);
				} catch (Exception e) {
					// on attrape l'exception et on passe au suivant
					e.printStackTrace();
				}
			}
		} else {
			RepositoryConnection connection = repository.getConnection();
			try {
				connection.add(
						aFileOrDirectory,
						// TODO : ici mettre le namespace par defaut comme un parametre ?
						RDF.NAMESPACE,
						RDFFormat.forFileName(aFileOrDirectory.getName(), RDFFormat.RDFXML),
						(autoNamedGraphs)?repository.getValueFactory().createURI(context.toString()):((this.namedGraph != null)?repository.getValueFactory().createURI(this.namedGraph.toString()):null)
				);
				numberOfProcessedFiles++;
			} catch (Exception e) {
				// on attrape l'exception et on la print - si on n'a que le finally, l'exception passe a la trappe
				e.printStackTrace();
			} finally {
				ConnectionUtil.closeQuietly(connection);
			}
		}
	}

	public boolean isAutoNamedGraphs() {
		return autoNamedGraphs;
	}

	/**
	 * if true, generates automatically a named graph for each loaded file based on the file path. (false by default)
	 * @param autoNamedGraphs
	 */
	public void setAutoNamedGraphs(boolean autoNamedGraphs) {
		this.autoNamedGraphs = autoNamedGraphs;
	}

	public URI getNamedGraph() {
		return namedGraph;
	}

	/**
	 * The named graph in which to load the data. Can be null, in which case data will be loaded in the default graph.
	 * @param namedGraph
	 */
	public void setNamedGraph(URI namedGraph) {
		this.namedGraph = namedGraph;
	}

	/**
	 * @return the number of files that were processed by this instance
	 */
	public int getNumberOfProcessedFiles() {
		return numberOfProcessedFiles;
	}
	
}