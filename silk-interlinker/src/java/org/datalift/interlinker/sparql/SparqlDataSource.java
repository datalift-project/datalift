package org.datalift.interlinker.sparql;

import java.util.ArrayList;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;

import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;


/**
 * Class to query the internal Sparql Endpoint
 * @author carlo
 *
 */
public class SparqlDataSource implements DataSource{
    
    /** Datalift's internal Sesame {@link Repository repository}. **/
    protected static final Repository INTERNAL_REPO = Configuration.getDefault().getInternalRepository();
    /** Datalift's internal Sesame {@link Repository repository} URL. */
    protected static final String INTERNAL_URL = INTERNAL_REPO.getEndpointUrl();
    /** Datalift's logging system. */
    protected static final Logger LOG = Logger.getLogger();
    

   /**
    * perform a select operation
    * @param query
    * @param bind
    * @return
    */
    public SparqlCursor query(String context, String[] columns, String[] whereConditions) {
    	//create the query in here
    	TupleQuery tq;
		TupleQueryResult tqr;
		SparqlCursor cursor = null ;
		String query = writeSelectQuery(true, columns, context, whereConditions);
		LOG.debug("Processing query: \"{}\"", query);
		try {
			RepositoryConnection cnx = INTERNAL_REPO.newConnection();
			tq = cnx.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			for(String bind:columns){
				if (!hasCorrectBindingNames(tqr, bind)) {
					throw new MalformedQueryException("Wrong query result bindings - " + query);
				}
			}
			cursor = new ResultCursor(columns);
			while (tqr.hasNext()) {
				BindingSet bindSet = tqr.next();
				String[] rowValues = new String[columns.length];
				for(int i=0;i<columns.length;i++){
					rowValues[i]=bindSet.getValue(columns[i]).toString();
				}
				((ResultCursor)cursor).addRow(rowValues);
			}
			cnx.close();
		}
		catch (MalformedQueryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (QueryEvaluationException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (RepositoryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		}
	    return cursor;
	}
    
    /**
	 * Tells if the bindings of the results are well-formed.
	 * @param tqr The result of a SPARQL query.
	 * @param bind The result one and only binding.
	 * @return True if the results contains only bind.
	 * @throws QueryEvaluationException Error while closing the result.
	 */
	private boolean hasCorrectBindingNames(TupleQueryResult tqr, String bind) throws QueryEvaluationException {
		return tqr.getBindingNames().size() == 1 && tqr.getBindingNames().contains(bind);
	}
	
	/**
     * Writes a query given a bind to retrieve, a context and a WHERE clause.
     * @param context Context on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return A full query.
     */
    private String writeSelectQuery(boolean distinct, String[] bindings, String context, String[] whereConditions) {
    	StringBuffer query = new StringBuffer("SELECT ");
    	if(distinct){
    		query.append("DISTINCT ");
    	}
    	for(String bind: bindings){
    		query.append("?"+bind+" ");
    	}
    	if(context!=null){
    		query.append("FROM <"+ context +"> ");
    	}
    	query.append("WHERE { ");
    	for(String condition: whereConditions){
    		query.append(condition);
    	}
    	query.append(" }");
    	return query.toString();
    }
    
   
    private class ResultCursor extends SparqlCursor{
    	public ResultCursor(String[] cols ){
    		this.columns = cols;
    		this.content = new ArrayList<String[]>();
    	}
    	
    	/**
    	 * Add a row to the cursor
    	 * @param row an hashmap that has the name of the column as key, and its value as value
    	 */
    	public void addRow(String[] row){
    		if(row.length!=this.columns.length){
    			throw new IllegalArgumentException();
    		}
    		this.content.add(row);
    	}
    }
    
}
