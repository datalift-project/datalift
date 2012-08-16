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

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import java.io.ObjectStreamException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;

import com.sun.jersey.api.view.Viewable;


/**
 * A {@link ProjectModule project module} that replaces RDF object fields from 
 * a {@link RdfFileSource RDF file source} by URIs to RDF entities.
 * This class is a middle man between our front-end interface & back-end logic.
 * TODO Add a way to set form fields via GET.
 *
 * @author tcolas
 * @version 15072012
 */
@Path(InterconnectionController.MODULE_NAME)
public class InterconnectionController extends BaseModule implements ProjectModule {
    
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module's name. */
    public static final String MODULE_NAME = "stringtouri";
    /** Base name of the resource bundle for converter GUI. */
    protected static final  String GUI_RESOURCES_BUNDLE = "resources";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------
    
    /** Datalift's logger. */
    private static final Logger LOG = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The requested module position in menu. */
    private final int position;
    /** The requested module label in menu. */
    private final String label;
    /** The module's back-end logic handler. */
    private final InterconnectionModel model;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new InterconnectionController instance.
     */
    public InterconnectionController() {
        super(MODULE_NAME);
        //TODO Switch to the right position.
        position = 99999999;
        label = PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, this).getString(this.getName() + ".button");
        
        model = new InterconnectionModel();
    }

    //-------------------------------------------------------------------------
    // Project management
    //-------------------------------------------------------------------------

    /**
     * Retrieves a {@link Project} using its URI.
     * @param projuri the project URI.
     *
     * @return the project.
     * @throws ObjectStreamException if the project does not exist.
     */
    private Project getProject(URI projuri) throws ObjectStreamException {
        ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        Project p = pm.findProject(projuri);
                
        return p;
    }

    /**
     * Handles our Velocity templates.
     * @param templateName Name of the template to parse.
     * @param it Parameters for the template.
     * @return A new viewable in Velocity Template Language
     */
    private Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }

    /**
     * Tells the project manager to add a new button to projects with at least 
     * two sources.
     * @param p Our current project.
     * @return The URI to our project's main page.
     */
    @Override
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
                if (LOG.isDebugEnabled()) {
                	LOG.debug(MODULE_NAME + " Project " + p.getTitle() + " can use StringToURI.");
                }
            }
            else {
            	if (LOG.isDebugEnabled()) {
                	LOG.debug(MODULE_NAME + " Project " + p.getTitle() + " can't use StringToURI.");
                }
            }
            
        }
        catch (URISyntaxException e) {
            LOG.fatal("Uh !", e);
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
        LinkedList<String> sourcesURIs = model.getSourcesURIs(proj);
        
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("it", proj);
        
        args.put("sources", sourcesURIs);
        args.put("classes", model.getAllClasses(sourcesURIs));
        args.put("predicates", model.getAllPredicates(sourcesURIs));
        
        return Response.ok(this.newViewable("/stringtouri-form.vm", args)).build();
    }
    
    /**
     * Form submit handler : launching StringToURI.
     * @param projectId the project using StringToURI.
     * @param sourceDataset context of our source (reference) data.
     * @param targetDataset context of our target (updated) data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @return Our module's post-process page.
     * @throws ObjectStreamException
     */
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response doSubmit(@QueryParam("project") URI projectId,
    	            	@FormParam("sourcedataset") String sourceDataset,
    		            @FormParam("targetdataset") String targetDataset,
    		            @FormParam("sourcepredicate") String sourcePredicate,
    		            @FormParam("targetpredicate") String targetPredicate,
    		            @FormParam("sourceclass") String sourceClass,
    		            @FormParam("targetclass") String targetClass,
    		            @FormParam("update") String update) throws ObjectStreamException {
    	// Retrieves our project and its sources.
        Project proj = this.getProject(projectId);
        
        sourceDataset = sourceDataset.trim();
        targetDataset = targetDataset.trim();
        sourcePredicate = sourcePredicate.trim();
        targetPredicate = targetPredicate.trim();
        sourceClass = sourceClass.trim();
        targetClass = targetClass.trim();
        boolean modifyPermanently = Boolean.parseBoolean(update.trim());
        
        String view;
        HashMap<String, Object> args = new HashMap<String, Object>();
	    args.put("it", proj);
        
	    // We first validate all of the fields.
        LinkedList<String> errorMessages = model.getErrorMessages(proj, sourceDataset, targetDataset, sourceClass, targetClass, sourcePredicate, targetPredicate);
        
        if (errorMessages.isEmpty()) {
      	    args.put("sourcedataset", sourceDataset);
    	    args.put("targetdataset", targetDataset);
    	    args.put("sourcepredicate", sourcePredicate);
    	    args.put("targetpredicate", targetPredicate);
    	    args.put("sourceclass", sourceClass);
    	    args.put("targetclass", targetClass);
    	    // StringToURI is launched if and only if our values are all valid.
            args.put("newtriples", model.launchStringToURI(proj, sourceDataset, targetDataset, sourceClass, targetClass, sourcePredicate, targetPredicate, modifyPermanently, false));
            view = "stringtouri-success.vm";
        }
        else {
        	args.put("errormessages", errorMessages);
        	view = "stringtouri-error.vm";
        }

        return Response.ok(this.newViewable("/" + view, args)).build();
	}
    
    /**
     * Remote submit handler : launching StringToURI.
     * @param projectId the project using StringToURI.
     * @param sourceDataset context of our source (reference) data.
     * @param targetDataset context of our target (updated) data.
     * @param sourcePredicate predicate in source data.
     * @param targetPredicate predicate in target data.
     * @param sourceClass class in source data.
     * @param targetClass class in target data.
     * @param update tells if the data is to be updated or not.
     * @return Our module's post-process page.
     * @throws ObjectStreamException
     */
    @GET
    @Path("go")
    @Produces(MediaType.TEXT_HTML)
    public Response doRemoteSubmit(@QueryParam("project") URI projectId,
    		@QueryParam("sourcedataset") String sourceDataset,
    		@QueryParam("targetdataset") String targetDataset,
    		@QueryParam("sourcepredicate") String sourcePredicate,
    		@QueryParam("targetpredicate") String targetPredicate,
            @QueryParam("sourceclass") String sourceClass,
            @QueryParam("targetclass") String targetClass,
            @FormParam("update") String update) throws ObjectStreamException {
    	// Remote call example : http://localhost:8080/datalift/stringtouri/go
    	// ?project=http://localhost:8080/datalift/project/world
    	// &targetdataset=http://localhost:8080/datalift/project/world/source/countries-tolink-rdf-rdf
    	// &targetclass=http://www.geonames.org/ontology%23Country
    	// &targetpredicate=http://telegraphis.net/ontology/geography/geography%23onContinent
    	// &sourcedataset=http://localhost:8080/datalift/project/world/source/continents-rdf-rdf
    	// &sourceclass=http://www.telegraphis.net/ontology/geography/geography%23Continent
    	// &sourcepredicate=http://www.geonames.org/ontology%23name
    	return doSubmit(projectId, sourceDataset, targetDataset, sourcePredicate, targetPredicate, sourceClass, targetClass, update);
    }
    
    /**
     * Traps accesses to module static resources and redirect them
     * toward the default {@link ResourceResolver} for resolution.
     * @param  path        the relative path of the module static
     *                     resource being accessed.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response JAX-RS response} to download the
     *         content of the specified public resource.
     * @throws WebApplicationException complete with status code and
     *         plain-text error message if any error occurred while
     *         accessing the requested resource.
     */
    @GET
    @Path("static/{path: .*$}")
    public Object getStaticResource(@PathParam("path") String path,
                                    @Context UriInfo uriInfo,
                                    @Context Request request,
                                    @HeaderParam(ACCEPT) String acceptHdr) {
        return Configuration.getDefault()
                            .getBean(ResourceResolver.class)
                            .resolveModuleResource(this.getName(),
                                                   uriInfo, request, acceptHdr);
    }

}
