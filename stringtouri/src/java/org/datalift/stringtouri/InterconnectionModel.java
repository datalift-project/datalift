/*
 * Copyright / LIRMM 2012
 * Contributor(s) : T. Colas, F. Scharffe
 *
 * Contact: thibaud.colas@etud.univ-montp2.fr
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.stringtouri;

import java.util.Iterator;
import java.util.LinkedList;

import me.assembla.stringtouri.SesameApp;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.Repository;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * A {@link ProjectModule project module} that replaces RDF object fields from 
 * a {@link RdfFileSource RDF file source} by URIs to RDF entities.
 * This class handles StringToURI's interconnection constraints.
 *
 * @author tcolas
 * @version 15072012
 */
public class InterconnectionModel {

	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

	/** The module's name. */
    public static final String MODULE_NAME = InterconnectionController.MODULE_NAME;
    /** Base name of the resource bundle for converter GUI. */
    protected static final String GUI_RESOURCES_BUNDLE = InterconnectionController.GUI_RESOURCES_BUNDLE;
    
	/** Binding for the default subject var in SPARQL. */
    private static final String SB = "s";
    /** Binding for the default predicate var in SPARQL. */
    private static final String PB = "p";
    /** Binding for the default object var in SPARQL. */
    private static final String OB = "o";
    
    /** Default WHERE SPARQL clause to retrieve all classes. */
    private static final String CLASS_WHERE = "{?" + SB + " a ?" + OB + "}";
    /** Default WHERE SPARQL clause to retrieve all predicates. */
    private static final String PREDICATE_WHERE = "{?" + SB + " ?" + PB + " ?" + OB + "}";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** Datalift's internal Sesame {@link Repository repository}. **/
    private static final Repository INTERNAL_REPO = Configuration.getDefault().getInternalRepository();
    /** Datalift's internal Sesame {@link Repository repository} URL. */
    private static final String INTERNAL_URL = INTERNAL_REPO.getEndpointUrl();
    /** Datalift's logging system. */
    private static final Logger LOG = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Connection to the internal Sesame {@link Repository repository}. **/
    private RepositoryConnection internal;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new InterconnectionModel instance.
     */
    public InterconnectionModel() {
    	internal =  INTERNAL_REPO.newConnection();
    }
    
    
    /**
     * Resource getter.
     * @param key The key to retrieve.
     * @return The value of key.
     */
    private String getTranslatedResource(String key) {
    	return PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, this).getString(key);
    }
    
    //-------------------------------------------------------------------------
    // Sources management.
    //-------------------------------------------------------------------------
    
    /**
     * Checks if a given {@link Source} contains valid RDF-structured data.
     * @param src The source to check.
     * @return True if src is {@link TransformedRdfSource} or {@link SparqlSource}.
     */
    private boolean isValidSource(Source src) {
    	return src.getType().equals(SourceType.TransformedRdfSource) 
        	|| src.getType().equals(SourceType.SparqlSource);
    }
    
    /**
     * Checks if a {@link Project proj} contains valid RDF sources.
     * @param proj The project to check.
     * @param minvalid The number of RDF sources we want to have.
     * @return True if there are more than number valid sources.
     */
    public final boolean hasMultipleRDFSources(Project proj, int minvalid) {
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
     * Returns all of the URIs (as strings) from the {@link Project project}.
     * @param proj The project to use.
     * @return A LinkedList containing source file's URIs as strings.
     */
    public final LinkedList<String> getSourcesURIs(Project proj) {
    	LinkedList<String> ret = new LinkedList<String>();
    	
    	for (Source src : proj.getSources()) {
    		if (isValidSource(src)) {
    			ret.add(src.getUri());
    		}
    	}
    	return ret;
    }
    
    //-------------------------------------------------------------------------
    // Queries management.
    //-------------------------------------------------------------------------
    
    /**
	 * Tels if the bindings of the results are wel-formed.
	 * @param tqr The result of a SPARQL query.
	 * @param bind The result one and only binding.
	 * @return True if the results contains only bind.
	 * @throws QueryEvaluationException Error while closing the result.
	 */
	private boolean hasCorrectBindingNames(TupleQueryResult tqr, String bind) throws QueryEvaluationException {
		return tqr.getBindingNames().size() == 1 && tqr.getBindingNames().contains(bind);
	}
    
	/**
	 * Sends and evaluates a SPARQL select query on the data set, then returns
	 * the results (which must be one-column only) as a list of Strings.
	 * @param query The SPARQL query without its prefixes.
	 * @param bind The result one and only binding.
	 * @return The query's result as a list of Strings.
	 */
    private LinkedList<String> selectQuery(String query, String bind) {
		TupleQuery tq;
		TupleQueryResult tqr;
		LinkedList<String> ret = new LinkedList<String>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug(MODULE_NAME + " " + query);
		}
		
		try {
			tq = internal.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			
			if (!hasCorrectBindingNames(tqr, bind)) {
				throw new MalformedQueryException("Wrong query result bindings - " + query);
			}
			
			while (tqr.hasNext()) {
				ret.add(tqr.next().getValue(bind).stringValue());
			}
		}
		catch (MalformedQueryException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		} catch (QueryEvaluationException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		} catch (RepositoryException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
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
    private String writeQuery(String context, String where, String bind) {
    	String ret = "SELECT DISTINCT ?" + bind
    			+ (context.isEmpty() ? "" : " FROM <" + context + ">")
    			+ " WHERE " + where;
    	return ret;
    }
    
    /**
     * Retrieves multiple queries results based on a query pattern executed on
     * multiple contexts.
     * @param contexts Contexts on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return Results as a LinkedList of Strings.
     */
    private LinkedList<String> getMultipleResults(LinkedList<String> contexts, String where, String bind) {
    	LinkedList<String> ret = new LinkedList<String>();
    	
    	for (String context : contexts) {
    		ret.addAll(selectQuery(writeQuery(context, where, bind), bind));
    	}
    	return ret;
    }

    /**
     * Retrieves all of the classes used inside given contexts.
     * @param contexts The contexts to use.
     * @return A LinkedList of all of the classes used inside the contexts.
     */
	public final LinkedList<String> getAllClasses(LinkedList<String> contexts) {
		return getMultipleResults(contexts, CLASS_WHERE, OB);
	}
	
	/**
     * Retrieves all of the predicates used inside given contexts.
     * @param contexts The contexts to use.
     * @return A LinkedList of all of the predicates used inside the contexts.
     */
	public final LinkedList<String> getAllPredicates(LinkedList<String> contexts) {
		return getMultipleResults(contexts, PREDICATE_WHERE, PB);
	}
	
	//-------------------------------------------------------------------------
    // Value validation.
    //-------------------------------------------------------------------------
    
	/**
     * Checks whether the given sources are different.
     * @param one Source to check.
     * @param two Source to check.
     * @return True if the sources are different.
     */
    private boolean isDifferentSources(String one, String two) {
    	return !one.equals(two);
    }
    
	/**
     * Checks whether the given source exists for a given project.
     * @param val Source to find.
     * @param proj Project where to search for the source.
     * @return True if the source exists in the given project.
     */
    private boolean isValidSource(String val, Project proj) {
    	return !val.isEmpty() && getSourcesURIs(proj).contains(val);
    }
    
	/**
     * Checks whether a value is empty, eg. "", "Aucune" or "None". The value 
     * must be trimmed first.
     * @param val Value to check.
     * @return True if val is empty.
     */
    private boolean isEmptyValue(String val) {
    	return val.isEmpty() || val.equals(getTranslatedResource("field.none"));
    }
    
    /**
     * Checks whether a value is valid, eg. is inside a list. The value must be
     * trimmed first.
     * @param val Value to check.
     * @param values List where the value must be.
     * @return True if the value is valid.
     */
    private boolean isValidValue(String val, LinkedList<String> values) {
    	return !val.isEmpty() && values.contains(val);
    }
   
    /**
     * Checks whether the given class exists inside a given context.
     * @param val Class to find.
     * @param context Context where to search for the class.
     * @return True if the class exists in the given context.
     */
    private boolean isValidClass(String val, String context) {
    	return !val.isEmpty() && !context.isEmpty() 
    		&& isValidValue(val, selectQuery(writeQuery(context, CLASS_WHERE, OB), OB));
    }
    
    /**
     * Checks whether the given predicate exists inside a given context.
     * @param val Class to find.
     * @param context Context where to search for the predicate.
     * @return True if the predicate exists in the given context.
     */
    private boolean isValidPredicate(String val, String context) {
    	return !val.isEmpty() && !context.isEmpty()
    		&& isValidValue(val, selectQuery(writeQuery(context, PREDICATE_WHERE, PB), PB));
    }
    
    //-------------------------------------------------------------------------
    // Launcher management.
    //-------------------------------------------------------------------------
    
    /**
     * StringToURI basic error checker.
     * @param proj Our project.
     * @param sourceContext context of our source (reference) data.
     * @param targetContext context of our target (updated) data.  
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @return True if all fields are correct.
     */
    public final boolean validateAll(Project proj,
    										String sourceContext, 
											String targetContext, 
											String sourceClass, 
											String targetClass, 
											String sourcePredicate, 
											String targetPredicate) {
    	return isDifferentSources(sourceContext, targetContext)
    		&& isValidSource(sourceContext, proj)
    		&& isValidSource(targetContext, proj)
    		&& (isEmptyValue(sourceClass) || isValidClass(sourceClass, sourceContext))
    		&& (isEmptyValue(targetClass) || isValidClass(targetClass, targetContext))
    		&& isValidPredicate(sourcePredicate, sourceContext)
    		&& isValidPredicate(targetPredicate, targetContext);
    }
    
    /**
     * StringToURI error checker with error messages.
     * @param proj Our project.
     * @param sourceContext context of our source (reference) data.
     * @param targetContext context of our target (updated) data.  
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @return Newly created triples.
     */
    public final LinkedList<String> getErrorMessages(Project proj,
    										String sourceContext, 
											String targetContext, 
											String sourceClass, 
											String targetClass, 
											String sourcePredicate, 
											String targetPredicate) {
    	LinkedList<String> errors = new LinkedList<String>();
    	
    	// We have to test every value one by one in order to add the right error message.
    	// TODO Add custom errors for empty values.
       	if (!isDifferentSources(sourceContext, targetContext)) {
    		errors.add(getTranslatedResource("error.samedatasets"));
    	}
    	else {
    		if (!isValidSource(sourceContext, proj)) {
        		errors.add(getTranslatedResource("error.datasetnotfound") + " \"" + proj.getTitle() + "\" : \"" + sourceContext  + "\".");
    		}
    		if (!isValidSource(targetContext, proj)) {
        		errors.add(getTranslatedResource("error.datasetnotfound") + " \"" + proj.getTitle() + "\" : \"" + targetContext  + "\".");
        	}
    	}
    	if (!isEmptyValue(sourceClass) && !isValidClass(sourceClass, sourceContext)) {
    		errors.add(getTranslatedResource("error.classnotfound") + " \"" + sourceContext + "\" : \"" + sourceClass + "\".");
    	}
    	if (!isEmptyValue(targetClass) && !isValidClass(targetClass, targetContext)) {
    		errors.add(getTranslatedResource("error.classnotfound") + " \"" + targetContext + "\" : \"" + targetClass + "\".");
    	}
    	if (!isValidPredicate(sourcePredicate, sourceContext)) {
    		errors.add(getTranslatedResource("error.predicatenotfound") + " \"" + sourceContext + "\" : \"" + sourcePredicate  + "\".");
    	}
    	if (!isValidPredicate(targetPredicate, targetContext)) {
    		errors.add(getTranslatedResource("error.predicatenotfound") + " \"" + targetContext + "\" : \"" + targetPredicate  + "\".");
    	}
    	
    	return errors;
    }
    
    /**
     * StringToURI module launcher.
     * @param proj Our project.
     * @param sourceContext context of our source (reference) data.
     * @param targetContext context of our target (updated) data.  
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @param update tells if we want to update everything or just preview.
     * @param validateAll Tells if we need to validate everything or not.
     * @return Newly created triples.
     */
    public final LinkedList<LinkedList<String>> launchStringToURI(Project proj,
    										String sourceContext, 
    										String targetContext, 
    										String sourceClass, 
    										String targetClass, 
    										String sourcePredicate, 
    										String targetPredicate,
    										boolean update,
    										boolean validateAll) {
    	 LinkedList<LinkedList<String>> ret;
    	 
    	if (!validateAll || validateAll(proj, sourceContext, targetContext, sourceClass, targetClass, sourcePredicate, targetPredicate)) {
    		// Launches a new StringToURI process.
            SesameApp stu = new SesameApp(INTERNAL_URL, INTERNAL_URL, sourceContext, targetContext);
           
            if (sourceClass.isEmpty() || targetClass.isEmpty()) {
            	stu.useSimpleLinkage(sourcePredicate, targetPredicate);
            }
            else {
            	stu.useTypedLinkage(sourcePredicate, targetPredicate, sourceClass, targetClass);
            }
            
            stu.useSPARQLOutput(false);
            ret = stu.getOutputAsList();
            
            if (update) {
            	if (LOG.isDebugEnabled()) {
            		LOG.debug(MODULE_NAME + " - the data is going to be updated.");
            	}
            	try {
					stu.updateData();
					//TODO Management d'exceptions ?
				} catch (RepositoryException e) {
					LOG.fatal(MODULE_NAME + e);
				} catch (UpdateExecutionException e) {
					LOG.fatal(MODULE_NAME + e);
				} catch (MalformedQueryException e) {
					LOG.fatal(MODULE_NAME + e);
				}
            }
                        
            if (LOG.isInfoEnabled()) {
            	LOG.info(MODULE_NAME + " interconnection OK.");
            }
    	}
    	else {
    		// Should never happen.
    		LinkedList<String> error = new LinkedList<String>();
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		ret = new LinkedList<LinkedList<String>>();
    		ret.add(error);
    		
    		if (LOG.isInfoEnabled()) {
            	LOG.info(MODULE_NAME + " interconnection KO.");
            }
    	}
    	
    	return ret;
    }
}
