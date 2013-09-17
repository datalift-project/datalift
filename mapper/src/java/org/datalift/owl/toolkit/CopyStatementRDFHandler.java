package org.datalift.owl.toolkit;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * A handler that copies the resulting statements to a target repository. If the target repository
 * is set to be the same as the original/source repository on which the SPARQL queries are executed,
 * this means the resulting triples of a CONSTRUCT query will be inserted into the same repository.
 * This can form the base of a SPARQL-based inference engine or transformation engine.
 * 
 * <p/>This handler can contain "postProcessors" that will apply a transformation on the resulting
 * statements before inserting them in the target repository.
 * 
 * @author mondeca
 *
 */
public class CopyStatementRDFHandler implements RDFHandler {

	private Repository targetRepository;
	private List<StatementProcessorIfc> postProcessors;
	private int previousResultStatementsCount = -1;
	// number of resulting statements
	private int resultStatementsCount = 0;
	
	private Set<URI> targetGraphs;
	
	// used internally
	private RepositoryTransaction transaction;
	// used internally
	private Resource[] targetGraphsResources;
	
	
	/**
	 * Creates a CopyStatementRDFHandler by setting the target repository.
	 * 
	 * @param targetRepository the target repository in which the triples will be inserted.
	 */
	public CopyStatementRDFHandler(Repository targetRepository) {
		super();
		this.targetRepository = targetRepository;
	}
	
	/**
	 * A protected empty constructor to be called from subclasses that need to initialize a repository
	 * in their constructor;
	 */
	protected CopyStatementRDFHandler() {
		
	}
	

	@Override
	public void startRDF() throws RDFHandlerException {
		try {
			RepositoryConnection connection = this.targetRepository.getConnection();
			this.transaction = new RepositoryTransaction(connection);
		} catch (RepositoryException e) {
			throw new RDFHandlerException(e);
		}
		// on traduit les URIs des graphes cibles en Value Sesame
		// une bonne fois pour toute
		this.targetGraphsResources = toResourceArray(this.targetGraphs);
		// for the moment we don't have results, reset the number and keep it in previous count
		this.previousResultStatementsCount = this.resultStatementsCount;
		this.resultStatementsCount = 0;
	}
	
	@Override
	public void handleStatement(Statement s) throws RDFHandlerException {
		// post process the statement
		if(this.postProcessors != null) {
			for (StatementProcessorIfc aProcessor : this.postProcessors) {
				if(aProcessor.accept(s)) {
					s = aProcessor.process(s);
				}
			}
		}
		
		try {
			if(this.targetGraphsResources == null) {
				this.transaction.add(s);
			} else {
				this.transaction.add(s, this.targetGraphsResources);
			}
			// increment statement count
			this.resultStatementsCount++;
		} catch (RepositoryException e) {
			throw new RDFHandlerException(e);
		}
	}	

	@Override
	public void endRDF() throws RDFHandlerException {
		try {
			this.transaction.commit();
		} catch (RepositoryException e) {
			throw new RDFHandlerException(e);
		} finally {
			ConnectionUtil.closeQuietly(this.transaction.getConnection());
		}
	}

	
	private Resource[] toResourceArray(Set<URI> contexts) {
		if(contexts != null) {
			List<Resource> result = new ArrayList<Resource>();
			ValueFactory vf = this.targetRepository.getValueFactory();
			for (URI uri : contexts) {
				result.add(vf.createURI(uri.toString()));
			}
			return result.toArray(new Resource[]{});
		} else {
			return null;
		}
	}

	@Override
	public void handleComment(String c) throws RDFHandlerException {
		// nothing
	}

	@Override
	public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
		// nothing
	}

	public List<StatementProcessorIfc> getPostProcessors() {
		return postProcessors;
	}

	/**
	 * Sets the list of post processors.
	 * @param postProcessors
	 */
	public void setPostProcessors(List<StatementProcessorIfc> postProcessors) {
		this.postProcessors = postProcessors;
	}	
	
	/**
	 * Utility class to set only one post processor.
	 * @param processor
	 */
	public void setPostProcessor(StatementProcessorIfc processor) {
		setPostProcessors(Collections.singletonList(processor));
	}

	public Set<URI> getTargetGraphs() {
		return targetGraphs;
	}

	/**
	 * Sets the target graphs in which the resulting statements will be inserted. Be default
	 * the statements will be added in the default graph.
	 * 
	 * @param targetGraphs a set of URI containing all the graphs in which the statements will be inserted.
	 */
	public void setTargetGraphs(Set<URI> targetGraphs) {
		this.targetGraphs = targetGraphs;
	}

	public int getResultStatementsCount() {
		return resultStatementsCount;
	}

	public int getPreviousResultStatementsCount() {
		return previousResultStatementsCount;
	}

	/**
	 * A protected method to be called by subclasses that need to initialize a repository and pass it in
	 * their constructor
	 * 
	 * @param targetRepository
	 */
	protected void setTargetRepository(Repository targetRepository) {
		this.targetRepository = targetRepository;
	}

	/**
	 * Returns the target repository inwhich the statements are copied.
	 * @return
	 */
	public Repository getTargetRepository() {
		return targetRepository;
	}

}
