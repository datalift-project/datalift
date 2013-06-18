/*
 * Copyright / LIRMM 2011-2012
 * Contributor(s) : T. Colas, F. Scharffe
 *
 * Contact: thibaud.colas@etud.univ-montp2.fr
 */

package org.datalift.stringtouri;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * An abstract class for all of the interlinking modules, combining default 
 * operations and values.
 * 
 * @author tcolas, sugliac
 * @version 18062013
 */
public abstract class InterlinkingModel {
	
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Base name of the resource bundle for converter GUI. */
    protected static String GUI_RESOURCES_BUNDLE = InterlinkingController.GUI_RESOURCES_BUNDLE;
    
    /** Binding for the default subject var in SPARQL. */
    protected static final String SB = "s";
    /** Binding for the default predicate var in SPARQL. */
    protected static final String PB = "p";
    /** Binding for the default object var in SPARQL. */
    protected static final String OB = "o";
    
    
    /** Default WHERE SPARQL clause to retrieve all classes. */
    private static final String CLASS_WHERE = "{?" + SB + " a ?" + OB + "}";
    /** Default WHERE SPARQL clause to retrieve all predicates. */
    private static final String PREDICATE_WHERE = "{?" + SB + " ?" + PB + " ?" + OB + "}";
    
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** Datalift's internal Sesame {@link Repository repository}. **/
    protected static final Repository INTERNAL_REPO = Configuration.getDefault().getInternalRepository();
    /** Datalift's internal Sesame {@link Repository repository} URL. */
    protected static final String INTERNAL_URL = INTERNAL_REPO.getEndpointUrl();
    /** Datalift's logging system. */
    protected static final Logger LOG = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The module name. */
    protected final String moduleName;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new InterconnectionModel instance.
     * @param module Name of the module.
     */
    public InterlinkingModel(String module) {
        this.moduleName = module;
    }

    /**
     * Resource getter.
     * @param key The key to retrieve.
     * @return The value of key.
     */
    protected String getTranslatedResource(String key) {
    	return PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, InterlinkingModel.class).getString(key);
    }

    //-------------------------------------------------------------------------
    // Sources management.
    //-------------------------------------------------------------------------

    /**
     * Checks if a given {@link Source} is valid for our uses.
     * @param src The source to check.
     * @return True if src is {@link TransformedRdfSource} or {@link SparqlSource}.
     */
    protected abstract boolean isValidSource(Source src);
    
    /**
     * Checks if a {@link Project proj} contains valid RDF sources.
     * @param proj The project to check.
     * @param minvalid The number of RDF sources we want to have.
     * @return True if there are more than number valid sources.
     */
    protected final boolean hasMultipleRDFSources(Project proj, int minvalid) {
    	int cpt = 0;
    	Iterator<Source> sources = proj.getSources().iterator();
    	
    	while (sources.hasNext() && cpt < minvalid) {
    		if (isValidSource(sources.next())) {
    			cpt++;
    		}
    	}
    	return cpt >= minvalid;
    }
    
    /**
     * Get the name of every source that belong to a project
     * @param proj the Project URL
     * @return a list with every name of the sources
     */
    protected final List<String> getSourcesName(Project proj){
    	List<Source> sources = getSources(proj);
    	List<String> sourcesName = new ArrayList<String>();
    	for(Source src: sources){
    		sourcesName.add(src.getTitle());
    	}
    	return sourcesName;
    } 
    
    /**
     * Returns all of the URIs (as strings) from the {@link Project project}.
     * @param proj The project to use.
     * @return A LinkedList containing source file's URIs as strings.
     */
    protected final List<Source> getSources(Project proj) {
    	List<Source> sources = new ArrayList<Source>();
    	
    	for (Source src : proj.getSources()) {
    		if (isValidSource(src)) {
    			sources.add(src);
    		}
    	}
    	return sources;
    }
    
    //-------------------------------------------------------------------------
    // Queries management.
    //-------------------------------------------------------------------------
    
    /**
	 * Tells if the bindings of the results are well-formed.
	 * @param tqr The result of a SPARQL query.
	 * @param bind The result one and only binding.
	 * @return True if the results contains only bind.
	 * @throws QueryEvaluationException Error while closing the result.
	 */
	protected boolean hasCorrectBindingNames(TupleQueryResult tqr, String bind) throws QueryEvaluationException {
		return tqr.getBindingNames().size() == 1 && tqr.getBindingNames().contains(bind);
	}
    
	/**
	 * Sends and evaluates a SPARQL select query on the data set, then returns
	 * the results (which must be one-column only) as a list of Strings.
	 * @param query The SPARQL query without its prefixes.
	 * @param bind The result one and only binding.
	 * @return The query's result as a list of Strings.
	 */
    protected List<String> selectQuery(String query, String bind) {
		TupleQuery tq;
		TupleQueryResult tqr;
		List<String> ret = new LinkedList<String>();
		
		LOG.debug("Processing query: \"{}\"", query);
		RepositoryConnection cnx = INTERNAL_REPO.newConnection();
		try {
			tq = cnx.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			
			if (!hasCorrectBindingNames(tqr, bind)) {
				throw new MalformedQueryException("Wrong query result bindings - " + query);
			}
			
			while (tqr.hasNext()) {
				ret.add(tqr.next().getValue(bind).stringValue());
			}
		}
		catch (MalformedQueryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (QueryEvaluationException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (RepositoryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		}
		finally {
		    try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
		}
	    return ret;
	}
    
    /**
     * Writes a query given a bind to retrieve, a context and a WHERE clause.
     * @param context Context on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return A full query.
     */
    protected String writeQuery(String context, String where, String bind) {
    	String ret = "SELECT DISTINCT ?" + bind
    			+ (context.isEmpty() ? "" : " FROM <" + context + ">")
    			+ " WHERE " + where;
    	return ret;
    }
    
   
    /**
     * Retrieves multiple queries results based on a query pattern executed on
     * multiple contexts.
     * @param sourceContext Context on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return Results as a List of Strings.
     */
    protected final List<String> getQueryResult(String sourceContext, String where, String bind) {
    	String query = writeQuery(sourceContext, where, bind);
    	return selectQuery(query, bind);
    }
    

    /**
     * Retrieves all of the classes used inside a given context.
     * @param context The contexts to use.
     * @return A List of all of the classes used inside the context.
     */
    protected final List<String> getClasses(String context) {
		return getQueryResult(context, CLASS_WHERE, OB);
	}
	
	/**
     * Retrieves the predicates of a source.
     * @param uriContext The uri of the source.
     * @return A List of all of the predicates used inside the contexts.
     */
	protected final List<String> getPredicates(String uriContext) {
		return getQueryResult(uriContext, PREDICATE_WHERE, PB);
	}
	
	/**
	 * Get every predicate of a certain class
	 * @param uriSource Uri of the source
	 * @param uriClass Uri of the class
	 * @return a List of predicates that belong to a source and their type is of the selected class
	 */
	protected final List<String> getPredicatesOfClass(String uriSource, String uriClass){
		String sourceQuery = this.writeQuery(uriSource, "{ ?"+SB + " a <"+ uriClass + ">}", SB);
		Set<String> predicates = new HashSet<String>();
		List<String> subjects = this.selectQuery(sourceQuery, SB);
		for(String subj:subjects){
			String predicateQuery = this.writeQuery(uriSource, "{ <" + subj + "> ?" + PB + " ?"+ OB+" }", PB);
			predicates.addAll(this.selectQuery(predicateQuery, PB));
		}
		return new ArrayList<String>(predicates);
		
	}
	
    
    //-------------------------------------------------------------------------
    // Launcher management.
    //-------------------------------------------------------------------------

    /**
     * Creates a new transformed RDF source and attaches it to a project.
     * @param  p        the owning project.
     * @param  parent   the parent source object.
     * @param  name     the new source name.
     * @param  uri      the new source URI.
     *
     * @return the newly created transformed RDF source.
     * @throws IOException if any error occurred creating the source.
     */
    protected void addResultSource(Project p, Source parent, String name, String description, URI uri) throws IOException {
    	ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        pm.newTransformedRdfSource(p, uri, name, description, uri, parent);
        pm.saveProject(p);
    }
    

	/**
     * Checks whether a value is empty, eg. "", or the values corresponding 
     * to a field that wasn't filled.
     * @param val Value to check.
     * @return True if val is empty.
     */
	protected boolean isEmptyValue(String val) {
    	return val.isEmpty() 
    		|| val.equals(getTranslatedResource("field.none"))
    		|| val.equals(getTranslatedResource("field.optional"))
    		|| val.equals(getTranslatedResource("field.mandatory"));
    }
	

    /**
     * Checks whether the given class exists inside a given context.
     * @param val Class to find.
     * @param context Context where to search for the class.
     * @return True if the class exists in the given context.
     */
    protected boolean isValidClass(String val, String context) {
    	return !val.isEmpty() && !context.isEmpty() 
    		&& isValidValue(val, selectQuery(writeQuery(context, CLASS_WHERE, OB), OB));
    }
    
    /**
     * Checks whether the given predicate exists inside a given context.
     * @param val Class to find.
     * @param context Context where to search for the predicate.
     * @return True if the predicate exists in the given context.
     */
    protected boolean isValidPredicate(String val, String context) {
    	return !val.isEmpty() && !context.isEmpty()
    		&& isValidValue(val, selectQuery(writeQuery(context, PREDICATE_WHERE, PB), PB));
    }
    
    /**
     * Checks whether a value is valid, eg. is inside a list. The value must be
     * trimmed first.
     * @param val Value to check.
     * @param values List where the value must be.
     * @return True if the value is valid.
     */
    protected boolean isValidValue(String val, List<String> values) {
    	return !val.isEmpty() && values.contains(val);
    }
    
}

