package org.datalift.owl.toolkit;

public class TemplateCallSparqlBuilder implements SPARQLQueryBuilderIfc {

	protected String templateName;
	
	public TemplateCallSparqlBuilder(String templateName) {
		super();
		this.templateName = templateName;
	}

	@Override
	public String getSPARQL() {
		if(!TemplateRegistry.hasTemplate(templateName)) {
			return null;
		}
		return TemplateRegistry.getTemplate(this.templateName).getBody();
	}
	
}