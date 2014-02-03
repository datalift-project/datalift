package org.datalift.owl.toolkit;

import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Sparql;

/**
 * An argument of a SPARQL query Template
 * @author thomas
 *
 */
@Iri(TEMPLATE_VOCABULARY.ARGUMENT)
public class Argument {

	public Argument() {
		
	}
	
	/**
	 * Le nom de la variable a binder dans le SPARQL
	 */
	@Iri(TEMPLATE_VOCABULARY.VARNAME)
	protected String varName;
	
	/**
	 * Le nom d'affichage du paramètre dans un écran
	 */
	@Iri(TEMPLATE_VOCABULARY.DISPLAYNAME)
	protected String displayName;
	
	/**
	 * Un booleen indiquant si le parametre est obligatoire ou pas
	 */
	@Iri(TEMPLATE_VOCABULARY.MANDATORY)
	protected boolean mandatory;
	
	/**
	 * Le numero d'ordre de ce parametre dans la liste des parametres
	 */
	@Iri(TEMPLATE_VOCABULARY.ORDER)
	protected int order = 0;
	
	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
	
	@Sparql("SELECT ?rangeString WHERE { $this <"+TEMPLATE_VOCABULARY.ARGUMENTRANGE+"> ?range BIND(STR(?range) AS ?rangeString) }")
	public Object getArgumentRange() {
		return null;
	}
	
}