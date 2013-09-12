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

import java.io.ObjectStreamException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source;

/**
 * A {@link ProjectModule project module} that replaces RDF object fields from 
 * a {@link RdfFileSource RDF file source} by URIs to RDF entities.
 * This class is a middle man between our front-end interface & back-end logic.
 *
 * @author sugliac,tcolas 
 * @version 18062013
 */
@Path(StringToURIController.MODULE_NAME)
public class StringToURIController extends InterlinkingController
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module's name. */
    public static final String MODULE_NAME = "stringtouri";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------
    
    /** The module's back-end logic handler. */
    protected StringToURIModel model;
    
    /**web service identifier path to get the sources of a project*/
    private static final String SOURCE_PATH = "sources";
    
    /**web service identifier path to get the list of predicates */
    private static final String PREDICATE_IDENTIFIER = "predicates";
    
    /**web service identifier path to get the classes of a datasource */
    private static final String CLASSES_IDENTIFIER = "classes";
    
    /**web service identifier path to get the linking preview */
    private static final String PREVIEW_PATH="preview";
    
    /**web service identifier path to save the interlinking result */
    private static final String SAVE_PATH="save";
    
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new InterconnectionController instance.
     */
    public StringToURIController() {
        //TODO Switch to the right position.
        super(MODULE_NAME, 13371337);

        label = getTranslatedResource(MODULE_NAME + ".button");
        model = new StringToURIModel(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Project management
    //-------------------------------------------------------------------------

    /**
     * Tells the project manager to add a new button to projects with at least 
     * two sources.
     * @param p Our current project.
     * @return The URI to our project's main page.
     */
    public final UriDesc canHandle(Project p) {
        UriDesc uridesc = null;

        try {           
            // The project can be handled if it has at least two RDF sources.
            if (model.hasMultipleRDFSources(p, 2)) {
            	// link URL, link label
                uridesc = new UriDesc(this.getName() + "?project=" + p.getUri(), this.label); 
                
                if (this.position > 0) {
                    uridesc.setPosition(this.position);
                }
                LOG.debug("Project {} can use StringToURI", p.getTitle());
            }
            else {
                LOG.debug("Project {} can not use StringToURI", p.getTitle());
            }
        }
        catch (URISyntaxException e) {
            LOG.fatal("Uh!", e);
            throw new RuntimeException(e);
        }
        return uridesc;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * Index page handler of the StringToURI module.
     * @param projectId the project using StringToURI
     * @return Our module's interface.
     * @throws ObjectStreamException
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getIndexPage(@QueryParam("project") URI projectId) throws ObjectStreamException {
        // Retrieve the current project and its sources.
        Project proj = this.getProject(projectId);
        HashMap<String, Object> args = new HashMap<String, Object>();
        List<String> sources = model.getSourcesName(proj);
        args.put("sources", sources);
        args.put("it", proj);
        return Response.ok(this.newViewable("/stringtouri-form.vm", args)).build();
    }
    
    /**
     * Get the triples resulting from the interlinking of the sources
     * @param project project URL
     * @param sourceDataSet source dataset URL
     * @param sourceClass selected source class
     * @param sourcePredicate selected source predicate
     * @param targetDataSet target dataset URL
     * @param targetClass selected target class
     * @param targetPredicate selected target predicate
     * @param linkingPredicate predicate of the new triples
     * @return the JSON rappresentation of the new triples got by the interlinking module
     * @throws ObjectStreamException 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PREVIEW_PATH)
    public Response getLinkPreview(
    		@QueryParam("project") URI projectId,
    		@QueryParam("datasetSource") String sourceDataSet,
    		@QueryParam("classSource") String sourceClass,
    		@QueryParam("predicateSource") String sourcePredicate,
    		@QueryParam("datasetTarget") String targetDataSet,
    		@QueryParam("classTarget") String targetClass,
    		@QueryParam("predicateTarget") String targetPredicate,
    		@QueryParam("predicateLinking") String linkingPredicate) throws ObjectStreamException{
    	LinkedList<LinkedList<String>> result = model.getInterlinkedTriples(this.getProject(projectId),sourceDataSet, targetDataSet, sourceClass, targetClass, 
    			sourcePredicate, targetPredicate, linkingPredicate);
    	return this.getOkResponse(this.getJsonTriplesMatrix(result));
    }
    
    /**
     * Get the list of the datasets
     * @param projectId project URL
     * @return the list of the sources that belong to the project
     * @throws ObjectStreamException
     */
    @GET
    @Path(SOURCE_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasetList(
    		@QueryParam("project") URI projectId) throws ObjectStreamException{
		Project proj = this.getProject(projectId);
	    List<Source> linkableSources = model.getSources(proj);
    	return this.getOkResponse(this.getJsonSourceArray(linkableSources));
    }
    
    /**
     * Get the list of the predicates of a source
     * @param sourceId URL of the Source dataset
     * @param type URL of the class of the predicate
     * @return the predicates of a datasource that belong to a class
     * @throws ObjectStreamException
     */
    @GET
    @Path(PREDICATE_IDENTIFIER)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPredicates(
    		@QueryParam("source") String sourceId,
    		@QueryParam("class") String type) throws ObjectStreamException{
    	List<String> predicates; 
    	if(type==null){
    		predicates= model.getPredicates(sourceId);
    	}else{
    		predicates= model.getPredicatesOfClass(sourceId, type);
    	}
    	String jsonPredicates = this.getJsonArray(predicates);
    	return this.getOkResponse(jsonPredicates);
    }
    
    /**
     * @param sourceId URL of the source dataset
     * @return a list of all the classes that belong to a dataset
     * @throws ObjectStreamException
     */
    @GET
    @Path(CLASSES_IDENTIFIER)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClasses(
    		@QueryParam("source") String sourceId) throws ObjectStreamException{
    	List<String> classes = model.getClasses(sourceId);
    	String jsonClasses = this.getJsonArray(classes);
    	return this.getOkResponse(jsonClasses);
    }
   
    /**
     * Given interlinking parameters, save the result to a new source, that will have a specific URL 
     * @param projectId project URL
     * @param sourceDataSet context of our source (reference) data.
     * @param sourceClass class in source data
     * @param sourcePredicate predicate in source data
     * @param targetDataSet context of our target (updated) data.
     * @param targetClass class in target data
     * @param targetPredicate predicate in target data
     * @param linkingPredicate the predicate of the new triples got by the module
     * @param targetContext The target URL that will have the new source
     * @param newSourceName the name of the new source
     * @param newSourceDescr the description of the new source
     * @throws ObjectStreamException
     */
    @POST
    @Path(SAVE_PATH)
    public void saveLinkingResult(
    		@QueryParam("project") URI projectId,
    		@QueryParam("datasetSource") String sourceDataSet,
    		@QueryParam("classSource") String sourceClass,
    		@QueryParam("predicateSource") String sourcePredicate,
    		@QueryParam("datasetTarget") String targetDataSet,
    		@QueryParam("classTarget") String targetClass,
    		@QueryParam("predicateTarget") String targetPredicate,
    		@QueryParam("predicateLinking") String linkingPredicate,
    		@QueryParam("newSourceContext") String targetContext,
    		@QueryParam("newSourceName") String newSourceName,
    		@QueryParam("newSourceDescription") String newSourceDescr) throws ObjectStreamException{
    	Project prj =this.getProject(projectId);
    	model.saveInterlinkedSource(prj, sourceDataSet, targetDataSet, sourceClass, targetClass, 
    			sourcePredicate, targetPredicate,linkingPredicate, targetContext,newSourceName, newSourceDescr);
    	
    }
    
}
