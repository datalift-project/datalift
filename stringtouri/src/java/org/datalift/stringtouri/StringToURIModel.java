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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import me.assembla.stringtouri.SesameApp;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.TransformedRdfSource;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * A {@link ProjectModule project module} that replaces RDF object fields from 
 * a {@link RdfFileSource RDF file source} by URIs to RDF entities.
 * This class handles StringToURI's interconnection constraints.
 *
 * @author tcolas
 * @version 15082012
 */
public class StringToURIModel extends InterlinkingModel
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new StringToURIModel instance.
     * @param name Name of the module.
     */
    public StringToURIModel(String name) {
    	super(name);
    }

    //-------------------------------------------------------------------------
    // Sources management.
    //-------------------------------------------------------------------------
    
    /**
     * Checks if a given {@link Source} contains valid RDF-structured data.
     * @param src The source to check.
     * @return True if src is {@link TransformedRdfSource} or {@link SparqlSource}.
     */
    protected boolean isValidSource(Source src) {
    	return src.getType().equals(SourceType.TransformedRdfSource) 
        	|| src.getType().equals(SourceType.SparqlSource);
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
    										String update,
    										boolean validateAll) {
    	 LinkedList<LinkedList<String>> ret = new LinkedList<LinkedList<String>>();
    	 
    	if (!validateAll || validateAll(proj, sourceContext, targetContext, sourceClass, targetClass, sourcePredicate, targetPredicate)) {
    		try {
    			// If the data is going to be updated, we have to create a new Datalift source.
    			String finalTargetContext = update.equals("preview") ? targetContext : targetContext + "-stu";
    			if (!update.equals("preview")) {
    				Source parent = proj.getSource(targetContext);
    				addResultSource(proj, parent, parent.getTitle() + "-stu", new URI(finalTargetContext));
    				RepositoryConnection cnx = INTERNAL_REPO.newConnection();
    				
    				// Copy all of the data to the new graph.
    				String updateQy = "INSERT {GRAPH <" + finalTargetContext + "> {?s ?p ?o}} WHERE {GRAPH <" + targetContext + "> {?s ?p ?o}}";
    				Update up = cnx.prepareUpdate(QueryLanguage.SPARQL, updateQy);
    				up.execute();
    			}
    			
	    		// Launches a new StringToURI process.
    			
	            SesameApp stu = new SesameApp(INTERNAL_URL, INTERNAL_URL, sourceContext, finalTargetContext);
	           
	            if (sourceClass.isEmpty() && targetClass.isEmpty()) {
	            	stu.useSimpleLinkage(sourcePredicate, targetPredicate);
	            }
	            else {
	            	stu.useTypedLinkage(sourcePredicate, targetPredicate, sourceClass, targetClass);
	            }
            
	            stu.useSPARQLOutput(update.equals("new") ? targetPredicate + "_URI" : "");
	            ret = stu.getOutputAsList();
	            
	            if (!update.equals("preview")) {
	            	LOG.debug("{} - the data is going to be updated.", this.moduleName);
					stu.updateData();
	            }
			} catch (RepositoryException e) {
				LOG.fatal("{} - Update failed:", e, this.moduleName);
			} catch (UpdateExecutionException e) {
				LOG.fatal("{} - Update failed:", e, this.moduleName);
			} catch (MalformedQueryException e) {
				LOG.fatal("{} - Update failed:", e, this.moduleName);
			} catch (QueryEvaluationException e) {
				LOG.fatal("{} - Update failed:", e, this.moduleName);
			} catch (IOException e) {
				LOG.fatal("{} - Update failed:", e, this.moduleName);
			} catch (URISyntaxException e) {
				LOG.fatal("{} - Update failed:", e, this.moduleName);
			}
            LOG.info("{} - Interconnection OK.", this.moduleName);
    	}
    	else {
    		// Should never happen.
    		LinkedList<String> error = new LinkedList<String>();
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		ret = new LinkedList<LinkedList<String>>();
    		ret.add(error);

            LOG.info("{} - Interconnection failed.", this.moduleName);
    	}
    	return ret;
    }
}
