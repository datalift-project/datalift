package org.datalift.owl.toolkit;

/**
 * A concrete implementation of a SPARQLHelper that extends the base implementation and
 * delegates SPARQL construction to a SPARQLQueryBuilderIfc.
 * 
 * @author mondeca
 *
 */
public class SPARQLHelper extends SPARQLHelperBase implements AskSPARQLHelper, UpdateSPARQLHelper {

	protected SPARQLQueryBuilderIfc builder;
	
	public SPARQLHelper(SPARQLQueryBuilderIfc builder) {
		super();
		this.builder = builder;
	}
	
	public SPARQLHelper(String sparql) {
		super();
		this.builder = new StringSPARQLQueryBuilder(sparql);
	}

	@Override
	public String getSPARQL() {
		return builder.getSPARQL();
	}

}
