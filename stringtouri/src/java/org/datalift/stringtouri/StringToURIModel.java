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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.assembla.stringtouri.SesameApp;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.TransformedRdfSource;

import org.openrdf.OpenRDFException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.RepositoryConnection;



/**
 * A {@link ProjectModule project module} that replaces RDF object fields from 
 * a {@link RdfFileSource RDF file source} by URIs to RDF entities.
 * This class handles StringToURI's interconnection constraints.
 *
 * @author tcolas, csuglia
 * @version 18062013
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
    	return isValidSource(sourceContext, proj)
    		&& isValidSource(targetContext, proj)
    		&& (isEmptyValue(sourceClass) || isValidClass(sourceClass, sourceContext))
    		&& (isEmptyValue(targetClass) || isValidClass(targetClass, targetContext))
    		&& isValidPredicate(sourcePredicate, sourceContext)
    		&& isValidPredicate(targetPredicate, targetContext);
    }
    
    
    /**
     * Checks whether the given source exists for a given project.
     * @param val Source to find.
     * @param proj Project where to search for the source.
     * @return True if the source exists in the given project.
     */
    protected boolean isValidSource(String val, Project proj) {
    	List<Source> sources = getSources(proj);
    	List<String> sourcesUrl = new ArrayList<String>();
    	for(Source s: sources){
    		sourcesUrl.add(s.getUri());
    	}
    	return !isEmptyValue(val) && sourcesUrl.contains(val);
    }
    
    /**
     * Checks whether a value is valid, eg. is inside a list. The value must be
     * trimmed first.
     * @param val Value to check.
     * @param values List where the value must be.
     * @return True if the value is valid.
     */
    protected boolean isValidValue(String val, LinkedList<String> values) {
    	return !val.isEmpty() && values.contains(val);
    }
   
    /**
     * Get the new triples obtained by the interlinking
     * @param prj URL of the project
     * @param sourceContext context of our source (reference) data.
     * @param targetContext context of our target (updated) data.  
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @param linkingPredicate predicate of the new triples
     * @param limit numbers of triples to return
     * @return a list where every element is a list that represents a triple, containing the interlinked triples
     */
    public final List<LinkedList<String>> getInterlinkedTriples(Project prj,
    		String sourceContext, 
			String targetContext, 
			String sourceClass, 
			String targetClass, 
			String sourcePredicate, 
			String targetPredicate,
			String linkingPredicate,
			int limit){
    	SesameApp stu = getLinkingApp(prj,sourceContext, targetContext, sourceClass, targetClass, sourcePredicate, targetPredicate, linkingPredicate, targetContext);
    	if(stu==null){
    		throw new TechnicalException("module not available");
    	}
    	LinkedList<LinkedList<String>> newTriples = stu.getOutputAsList();
    	if(limit>newTriples.size()){
    		limit = newTriples.size();
    	}
    	return stu.getOutputAsList().subList(0, limit);
    }
    
    /**
     * Create a new source which will consist of the novel interlinked triples and those that belongs to the dataset to be
     * modified
     * @param proj URL of the project
     * @param sourceContext context of our source (reference) data.
     * @param targetContext context of our target (updated) data.  
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @param linkingPredicate predicate of the new triples
     * @param newSourceContext target URL of the new source
     * @param newSourceName Name of the new source
     * @param newSourceDescription description of the new source
     */
    public final void saveInterlinkedSource(Project proj,
    		String sourceContext,
    		String targetContext,
    		String sourceClass,
    		String targetClass, 
			String sourcePredicate, 
			String targetPredicate,
			String linkingPredicate,
			String newSourceContext,
			String newSourceName,
			String newSourceDescription, 
			boolean keepTargetTriples){
    	LOG.info("{} is about to interconnect the sources located at {} and {} to put the interlinking result within the context {}", this.moduleName, sourceContext, 
    			targetContext, newSourceContext);
    	SesameApp app = getLinkingApp(proj,sourceContext, targetContext, sourceClass, targetClass, sourcePredicate, targetPredicate, linkingPredicate, targetContext);
    	RepositoryConnection cnx = INTERNAL_REPO.newConnection();
		// Query to copy the triples of the dataset to the new graph.
		String copyDsQuery = "COPY <" + targetContext + "> TO <" + newSourceContext + ">";
		// Query to insert the interlinked triples into the new graph
		StringBuilder addTriplesQuery = new StringBuilder();
		LinkedList<LinkedList<String>> interlinkedTriples = app.getOutputAsList();
		for(LinkedList<String> triple : interlinkedTriples){
			addTriplesQuery.append("INSERT DATA { GRAPH <"+ newSourceContext +"> { ");
			// put every element of the triple between < >, since you will only get uris
			for(String tripleItem: triple){
				addTriplesQuery.append("<" + tripleItem +"> ");
			}
			addTriplesQuery.append("}}; ");
		}
		try {
			if(keepTargetTriples){
				Update upCopy = cnx.prepareUpdate(QueryLanguage.SPARQL, copyDsQuery);
				upCopy.execute();
			}
			Update upInsert = cnx.prepareUpdate(QueryLanguage.SPARQL, addTriplesQuery.toString());
			upInsert.execute();
			//now link the new graph to a datalift source, so it can be referenced easily
			Source parent = proj.getSource(targetContext);
			addResultSource(proj,parent,newSourceName, newSourceDescription, URI.create(newSourceContext));
			LOG.info("{} - Interconnection OK.", this.moduleName);
		} catch (OpenRDFException e) {
			LOG.fatal("{} - Update failed:", e, this.moduleName);
		} catch (IOException e) {
			LOG.fatal("{} - Update failed:", e, this.moduleName);
		}
	

    }

    /**
     * Initialize the object to perform interlinking
     * @param proj URL of the project
     * @param sourceContext context of our source (reference) data.
     * @param targetContext context of our target (updated) data.  
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @param linkingPredicate predicate of the new triples
     * @param newContext target URL of the new source
     * @return the Sesame Application object to perform interlinking
     */
    private SesameApp getLinkingApp(
    		Project proj,
			String sourceContext, 
			String targetContext, 
			String sourceClass, 
			String targetClass, 
			String sourcePredicate, 
			String targetPredicate,
			String linkingPredicate,
			String newContext) {
    	SesameApp stu = null;
    	if(validateAll(proj, sourceContext, targetContext, sourceClass, targetClass, sourcePredicate, targetPredicate)){
			try {
				stu = new SesameApp(INTERNAL_URL, INTERNAL_URL, sourceContext, newContext);
				if (sourceClass.isEmpty() && targetClass.isEmpty()) {
					stu.useSimpleLinkage(sourcePredicate, targetPredicate);
				}else {
					stu.useTypedLinkage(sourcePredicate, targetPredicate, sourceClass, targetClass);
				}	
				stu.useSPARQLOutput(linkingPredicate, true);
			} catch (OpenRDFException e) {
				LOG.fatal("{} - Update failed:", e, this.moduleName);
			} 
    	}else{
    		 throw new TechnicalException("An error occurred during interlinking");
    	}
    	return stu;
	}
}