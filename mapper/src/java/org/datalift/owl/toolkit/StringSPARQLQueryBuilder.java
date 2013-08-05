package org.datalift.owl.toolkit;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of SPARQLQueryBuilderIfc that wraps a String containing the SPARQL query.
 * @author mondeca
 *
 */
public class StringSPARQLQueryBuilder implements SPARQLQueryBuilderIfc {

	protected String sparql;	
		
	
	public static List<StringSPARQLQueryBuilder> fromStringList(List<String> strings) {
		if(strings == null) {
			return null;
		}
		
		ArrayList<StringSPARQLQueryBuilder> result = new ArrayList<StringSPARQLQueryBuilder>();
		for (String aString : strings) {
			result.add(new StringSPARQLQueryBuilder(aString));
		}
		return result;
	}
	
	public StringSPARQLQueryBuilder(String sparql) {
		super();
		this.sparql = sparql;
	}

	@Override
	public String getSPARQL() {
		return sparql;
	}

}
