package org.datalift.s4ac.utils;

import java.util.Set;

import org.datalift.fwk.Configuration;
import org.datalift.s4ac.exceptions.QueryLexicalException;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.sparql.SPARQLParser;


public class QueryAnalyser {

	public static QueryType analyse(String query){

		String baseURI = Configuration.getDefault().getProperty("BASE_URI_PROPERTY");
    	SPARQLParser par = new SPARQLParser();
    	
    	try {
    		ParsedQuery pq = par.parseQuery(query, baseURI);
    		Set<String> s = pq.getTupleExpr().getBindingNames();
    		
    		if (s.contains("ASK")) return QueryType.ASK;
    		if (s.contains("DESCRIBE")) return QueryType.DESCRIBE; 
    		if (s.contains("CONSTRUCT")) return QueryType.CONSTRUCT;
    		if (s.contains("SELECT")) return QueryType.SELECT;
    		
    		if (s.contains("LOAD")) return QueryType.LOAD;
    		if (s.contains("CLEAR")) return QueryType.CLEAR;
			if (s.contains("DROP")) return QueryType.DROP;
			if (s.contains("CREATE")) return QueryType.CREATE;
			if (s.contains("ADD")) return QueryType.ADD;
			if (s.contains("MOVE")) return QueryType.MOVE;
			if (s.contains("COPY")) return QueryType.COPY;
			
			if (s.contains("INSERT")) return QueryType.INSERT; 
			if (s.contains("DELETE")) return QueryType.DELETE; 
			
			
			else return QueryType.UNKNOWN;

						
		}
		catch (QueryLexicalException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (MalformedQueryException e2) {
			e2.printStackTrace();
		}
	
		return QueryType.UNKNOWN;
    	
	}
}
