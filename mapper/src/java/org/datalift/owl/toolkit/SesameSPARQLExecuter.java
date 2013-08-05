package org.datalift.owl.toolkit;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Operation;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;

/**
 * Executes SPARQL queries (given by a helper) onto a Sesame repository (given by a RepositoryProvider).
 * This can execute SELECT SPARQL queries or CONSTRUCT SPARQL queries.
 * <p />Note that by default, inferred statements WILL be included in the queries.
 * 
 * <p>Usage :
 * <code>
 * RepositoryProviderIfc provider = ...;
 * SelectSPARQLHelper helper = ...;
 * SesameSPARQLExecuter.newExecuter(provider).executeSelect(helper);
 * </code>
 * <p>Or :
 * <code>
 * RepositoryProviderIfc provider = ...;
 * ConstructSPARQLHelper helper = ...;
 * SesameSPARQLExecuter.newExecuter(provider).executeConstruct(helper);
 * </code>
 * 
 * Note that the default behavior of isIncludeInferred method is true, meaning the SPARQL
 * will be executed against the inferred RDF graph. You should set this to false explicitely
 * at the executer or the helper level if you need to make a query against the original RDF data.
 * 
 * @author mondeca
 */
public class SesameSPARQLExecuter implements SPARQLExecuterIfc {

	private Repository repository;
	
	private boolean includeInferred = true;
	
	private Set<URI> defaultGraphs = null;
	
	private Set<URI> namedGraphs = null;
	
	private URI defaultInsertGraph = null;
	
	private Set<URI> defaultRemoveGraphs = null;

	/**
	 * Convenience method to execute a SPARQL query in one line : SesameSPARQLExecuter.newExecuter(provider).executeSelect(helper).
	 * This will include inference by default.
	 * 
	 * @param repositoryProvider
	 * @return
	 */
	public static SesameSPARQLExecuter newExecuter(RepositoryProviderIfc repositoryProvider) {
		return new SesameSPARQLExecuter(repositoryProvider.getRepository());
	}

	/**
	 * Convenience method to execute a SPARQL query in one line, with the includeInferred flag.
	 * 
	 * @param repositoryProvider
	 * @return
	 */
	public static SesameSPARQLExecuter newExecuter(RepositoryProviderIfc repositoryProvider, boolean includeInferred) {
		return new SesameSPARQLExecuter(repositoryProvider.getRepository(), includeInferred);
	}
	
	public SesameSPARQLExecuter(Repository repository) {
		super();
		this.repository = repository;
	}
	
	public SesameSPARQLExecuter(Repository repository, boolean includeInferred) {
		super();
		this.repository = repository;
		this.includeInferred = includeInferred;
	}
	
	/**
	 * This is a convenience method that sets the default graph, the default insert graph,
	 * and the default remove graph to the provided graph URI. This is equivalent to the
	 * following calls :
	 * <code>
	 *  executer.setDefaultGraphs(Collections.singleton(graph));
	 *	executer.setDefaultInsertGraph(graph);
	 *	executer.setDefaultRemoveGraphs(Collections.singleton(graph));
	 * </code>
	 * 
	 * @param graph the URI of the graph to set
	 */
	public void setGraph(URI graph) {
		this.setDefaultGraphs(Collections.singleton(graph));
		this.setDefaultInsertGraph(graph);
		this.setDefaultRemoveGraphs(Collections.singleton(graph));
	}
	
	/**
	 * Execute la requete fourni par le helper, et passe les resultats de la requete au helper.
	 * 
	 */
	public void executeSelect(SelectSPARQLHelper helper) 
	throws SPARQLExecutionException {
		try {
			if(repository == null) {
				throw new SPARQLExecutionException("Repository is null. If it comes from a RepositoryProviderIfc, have you called the init() method on the RepositoryProvider ?");
			}
			
			RepositoryConnection connection = this.repository.getConnection();
			TupleQuery tupleQuery;
			try {
				String query = helper.getSPARQL();
				tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				
				// on positionne les bindings s'il y en a
				if(helper.getBindings() != null) {
					for (Map.Entry<String, Value> anEntry : helper.getBindings().entrySet()) {
						tupleQuery.setBinding(anEntry.getKey(), anEntry.getValue());
					}
				}
				
				// on inclut les inferred statements si demandé
				tupleQuery.setIncludeInferred((helper.isIncludeInferred() != null)?helper.isIncludeInferred():this.includeInferred);
				
				// on ajoute les datasets si besoin
				tupleQuery = (TupleQuery)processDataset(
						tupleQuery,
						((helper.getDefaultGraphs() != null)?helper.getDefaultGraphs():this.defaultGraphs),
						((helper.getNamedGraphs() != null)?helper.getNamedGraphs():this.namedGraphs),
						this.defaultInsertGraph,
						this.defaultRemoveGraphs
				);
				
				// on execute la query
				tupleQuery.evaluate(helper);
			} catch (MalformedQueryException e) {
				throw new SPARQLExecutionException(e);
			} finally {
				ConnectionUtil.closeQuietly(connection);
			}

		} catch (RepositoryException e) {
			throw new SPARQLExecutionException(e);
		} catch (QueryEvaluationException e) {
			throw new SPARQLExecutionException(e);
		} catch (TupleQueryResultHandlerException e) {
			throw new SPARQLExecutionException(e);
		}
	}

	public void executeConstruct(ConstructSPARQLHelper helper) 
	throws SPARQLExecutionException {
		try {
			if(repository == null) {
				throw new SPARQLExecutionException("Repository is null. If it comes from a RepositoryProviderIfc, have you called the init() method on the RepositoryProvider ?");
			}
			
			RepositoryConnection connection = this.repository.getConnection();
			GraphQuery graphQuery;
			try {
				String query = helper.getSPARQL();
				graphQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
				
				// on positionne les bindings s'il y en a
				if(helper.getBindings() != null) {
					for (Map.Entry<String, Value> anEntry : helper.getBindings().entrySet()) {
						graphQuery.setBinding(anEntry.getKey(), anEntry.getValue());
					}
				}
				
				// on inclut les inferred statements si demandé
				graphQuery.setIncludeInferred((helper.isIncludeInferred() != null)?helper.isIncludeInferred():this.includeInferred);
				
				// on ajoute les datasets si besoin
				graphQuery = (GraphQuery)processDataset(
						graphQuery,
						((helper.getDefaultGraphs() != null)?helper.getDefaultGraphs():this.defaultGraphs),
						((helper.getNamedGraphs() != null)?helper.getNamedGraphs():this.namedGraphs),
						this.defaultInsertGraph,
						this.defaultRemoveGraphs
				);
				
				// on execute la query
				graphQuery.evaluate(helper);
			} catch (MalformedQueryException e) {
				throw new SPARQLExecutionException(e);
			} finally {
				ConnectionUtil.closeQuietly(connection);
			}

		} catch (RepositoryException e) {
			throw new SPARQLExecutionException(e);
		} catch (QueryEvaluationException e) {
			throw new SPARQLExecutionException(e);
		} catch (RDFHandlerException e) {
			throw new SPARQLExecutionException(e);
		}
	}

	public boolean executeAsk(AskSPARQLHelper helper) 
	throws SPARQLExecutionException {
		try {
			if(repository == null) {
				throw new SPARQLExecutionException("Repository is null. If it comes from a RepositoryProviderIfc, have you called the init() method on the RepositoryProvider ?");
			}
			
			RepositoryConnection connection = this.repository.getConnection();
			BooleanQuery booleanQuery;
			try {
				String query = helper.getSPARQL();
				booleanQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL, query);
				
				// on positionne les bindings s'il y en a
				if(helper.getBindings() != null) {
					for (Map.Entry<String, Value> anEntry : helper.getBindings().entrySet()) {
						booleanQuery.setBinding(anEntry.getKey(), anEntry.getValue());
					}
				}
				
				// on inclut les inferred statements si demandé
				booleanQuery.setIncludeInferred((helper.isIncludeInferred() != null)?helper.isIncludeInferred():this.includeInferred);
				
				// on ajoute les datasets si besoin
				booleanQuery = (BooleanQuery)processDataset(
						booleanQuery,
						((helper.getDefaultGraphs() != null)?helper.getDefaultGraphs():this.defaultGraphs),
						((helper.getNamedGraphs() != null)?helper.getNamedGraphs():this.namedGraphs),
						this.defaultInsertGraph,
						this.defaultRemoveGraphs
				);
				
				// on execute la query
				return booleanQuery.evaluate();
			} catch (MalformedQueryException e) {
				throw new SPARQLExecutionException(e);
			} finally {
				ConnectionUtil.closeQuietly(connection);
			}

		} catch (RepositoryException e) {
			throw new SPARQLExecutionException(e);
		} catch (QueryEvaluationException e) {
			throw new SPARQLExecutionException(e);
		}
	}
	
	public void executeUpdate(UpdateSPARQLHelper helper) 
	throws SPARQLExecutionException {
		try {
			if(repository == null) {
				throw new SPARQLExecutionException("Repository is null. If it comes from a RepositoryProviderIfc, have you called the init() method on the RepositoryProvider ?");
			}
			
			RepositoryConnection connection = this.repository.getConnection();
			Update update;
			try {
				String updateString = helper.getSPARQL();
				update = connection.prepareUpdate(QueryLanguage.SPARQL, updateString);
				
				// on positionne les bindings s'il y en a
				if(helper.getBindings() != null) {
					for (Map.Entry<String, Value> anEntry : helper.getBindings().entrySet()) {
						update.setBinding(anEntry.getKey(), anEntry.getValue());
					}
				}
				
				// on inclut les inferred statements si demandé
				update.setIncludeInferred((helper.isIncludeInferred() != null)?helper.isIncludeInferred():this.includeInferred);
				
				// on ajoute les datasets si besoin
				update = (Update)processDataset(
						update,
						((helper.getDefaultGraphs() != null)?helper.getDefaultGraphs():this.defaultGraphs),
						((helper.getNamedGraphs() != null)?helper.getNamedGraphs():this.namedGraphs),
						this.defaultInsertGraph,
						this.defaultRemoveGraphs
				);
				
				// on execute l'update
				update.execute();
			} catch (MalformedQueryException e) {
				throw new SPARQLExecutionException(e);
			} finally {
				ConnectionUtil.closeQuietly(connection);
			}

		} catch (RepositoryException e) {
			throw new SPARQLExecutionException(e);
		} catch (UpdateExecutionException e) {
			throw new SPARQLExecutionException(e);
		}
	}
	
	// an Operation is either a Query or an Update
	private Operation processDataset(
			Operation o,
			Set<URI> defaultGraphs,
			Set<URI> namedGraphs,
			URI defaultInsertGraph,
			Set<URI> defaultRemoveGraphs
	) {
		if(
				(
						namedGraphs != null
						&&
						namedGraphs.size() > 0
				)
				||
				(
						defaultGraphs != null
						&&
						defaultGraphs.size() > 0
				)
				||
					defaultInsertGraph != null
				||
				(
						defaultRemoveGraphs != null
						&&
						defaultRemoveGraphs.size() > 0
				)
		) {
			DatasetImpl dataset = new DatasetImpl();
			ValueFactory vf = this.repository.getValueFactory();
			if(
					namedGraphs != null
			) {
				for (URI uri : namedGraphs) {
					dataset.addNamedGraph(vf.createURI(uri.toString()));
				}
			}
			if(
					defaultGraphs != null
			) {
				for (URI uri : defaultGraphs) {
					dataset.addDefaultGraph(vf.createURI(uri.toString()));
				}
			}
			if(
				defaultInsertGraph != null	
			) {
				dataset.setDefaultInsertGraph(vf.createURI(defaultInsertGraph.toString()));
			}
			if(
					defaultRemoveGraphs != null	
			) {
				for (URI uri : defaultRemoveGraphs) {
					dataset.addDefaultRemoveGraph(vf.createURI(uri.toString()));
				}
			}
			o.setDataset(dataset);
		}
		return o;
	}
	

	public boolean isIncludeInferred() {
		return includeInferred;
	}

	/**
	 * Sets whether the queries executed will include the inferred statements, if nothing is set at the helper level.
	 * If something is set at the helper level, this value on the executer will be ignored. Defaults to true. 
	 * 
	 * @param includeInferred
	 */
	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	public Set<URI> getDefaultGraphs() {
		return defaultGraphs;
	}

	public void setDefaultGraphs(Set<URI> defaultGraphs) {
		this.defaultGraphs = defaultGraphs;
	}

	public Set<URI> getNamedGraphs() {
		return namedGraphs;
	}

	public void setNamedGraphs(Set<URI> namedGraphs) {
		this.namedGraphs = namedGraphs;
	}

	public URI getDefaultInsertGraph() {
		return defaultInsertGraph;
	}

	public void setDefaultInsertGraph(URI defaultInsertGraph) {
		this.defaultInsertGraph = defaultInsertGraph;
	}

	public Set<URI> getDefaultRemoveGraphs() {
		return defaultRemoveGraphs;
	}

	public void setDefaultRemoveGraphs(Set<URI> defaultRemoveGraphs) {
		this.defaultRemoveGraphs = defaultRemoveGraphs;
	}
	
}