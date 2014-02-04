package org.datalift.owl.toolkit;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.annotations.Iri;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedOperation;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.QueryParserUtil;

/**
 * A SPARQL query template, associating a name to a SPARQL query, with some arguments.
 * The Template body can be any SPARQL query (SELECT, CONSTRUCT or UPDATE).
 * 
 * Templates can be parsed using a TemplateParser.
 * 
 * 
 * @author thomas
 *
 */
@Iri(TEMPLATE_VOCABULARY.TEMPLATE)
public class Template {
	//commentaire bidon pour le commit svn
	
	public enum SparqlQueryType {
		CONSTRUCT,
		SELECT,
		ASK,
		UPDATE
	}
	
	public Template() {
		
	}
	
	/**
	 * Le nom du template, qui correspondra a la façon de le reférencer
	 */
	@Iri(TEMPLATE_VOCABULARY.NAME)
	protected String name;

	/**
	 * Le nom d'affichage du template, pour affichage dans un écran
	 */
	@Iri(TEMPLATE_VOCABULARY.DISPLAYNAME)
	protected String displayName;
	
	/**
	 * La query SPARQL de ce template
	 */
	@Iri(TEMPLATE_VOCABULARY.BODY)
	protected String body;
	
	/**
	 * Les parametres d'entree de ce template
	 */
	@Iri(TEMPLATE_VOCABULARY.HASARGUMENT)
	protected Set<Argument> arguments;
	
	/**
	 * Add an argument to this template.
	 * 
	 * @param arg
	 */
	public void addArgument(Argument arg) {
		if(this.arguments == null) {
			this.arguments = new HashSet<Argument>();
		}
		this.arguments.add(arg);
	}
	
	/**
	 * Returns true if this template contains a parameter of the given name
	 * @param parameterName
	 */
	public boolean containsArgument(String argumentName) {
		if(this.arguments == null) {
			return false;
		}
		
		for (Argument aParam : this.arguments) {
			if(aParam.getVarName().equals(argumentName)) {
				return true;
			}
		}
		
		return false;
	}
	
	public Argument getArgumentWithOrder(int order) {
		if(this.arguments == null) {
			return null;
		}
		
		for (Argument aParam : this.arguments) {
			if(aParam.getOrder() == order) {
				return aParam;
			}
		}
		
		return null;
	}
	
	/**
	 * Dynamically determine the type of a SPARQL query base on its content
	 * @return
	 * @throws MalformedQueryException
	 */
	public SparqlQueryType getSparqlQueryType() throws MalformedQueryException {
		ParsedOperation parsedOperation = QueryParserUtil.parseOperation(QueryLanguage.SPARQL, this.getBody(), null);
		if (parsedOperation instanceof ParsedTupleQuery) {
			return SparqlQueryType.SELECT;
		} else if (parsedOperation instanceof ParsedGraphQuery) {
			return SparqlQueryType.CONSTRUCT;
		} else if (parsedOperation instanceof ParsedBooleanQuery) {
			return SparqlQueryType.ASK;
		} else if (parsedOperation instanceof ParsedUpdate) {
			return SparqlQueryType.UPDATE;
		} else {
			throw new MalformedQueryException("Unexpected query type "+ parsedOperation.getClass() + " for query " + this.getBody());
		}
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Set<Argument> getArguments() {
		return arguments;
	}

	public void setArguments(Set<Argument> arguments) {
		this.arguments = arguments;
	}

}
