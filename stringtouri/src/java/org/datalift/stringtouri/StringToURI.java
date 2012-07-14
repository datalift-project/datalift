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
import java.util.Collection;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import me.assembla.stringtouri.SesameApp;

import com.sun.jersey.api.view.Viewable;


/*
 * A {@link ProjectModule project module} that replaces RDF object fields
 * from a {@link RdfFileSource RDF file source} by URIs to RDF entities.
 *
 * @author tcolas
 */
@Path(StringToURI.MODULE_NAME)
public class StringToURI extends BaseModule implements ProjectModule {
    
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module's name. */
    public static final String MODULE_NAME = "stringtouri";
    /** Module's button label in french. */
    public static final String BTN_LABEL_FR = "Transformation des Strings en URIs";
    /** Module's button label in english. */
    public static final String BTN_LABEL_EN = "String to URI Transform";
    /** Binding for the default subject var in SPARQL. */
    public static final String sbind = "s";
    /** Binding for the default predicate var in SPARQL. */
    public static final String pbind = "p";
    /** Binding for the default object var in SPARQL. */
    public static final String obind = "o";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The requested module position in menu. */
    private final int position;
    /** The requested module label in menu. */
    private final String label;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new StringToURI instance.
     */
    public StringToURI() {
        super(MODULE_NAME);
        //TODO Déterminer la position utilisable.
        position = 99999999;
        //TODO Internationalization
        label = (true) ? BTN_LABEL_FR : BTN_LABEL_EN;
    }

    //-------------------------------------------------------------------------
    // Project management
    //-------------------------------------------------------------------------

    /**
     * Retrieves a {@link Project} using its URI.
     * @param  projuri the project URI.
     *
     * @return the project.
     * @throws ObjectStreamException if the project does not exist.
     */
    private final Project getProject(URI projuri) throws ObjectStreamException {
        ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        Project p = pm.findProject(projuri);
                
        return p;
    }

    /**
     * Handles our Velocity templates.
     * @param templateName Name of the template to parse.
     * @param it Parameters for the template.
     * @return
     */
    private final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }

    /**
     * Tells the project manager to add a new button to projects with at least 
     * two sources.
     */
    @Override
    public UriDesc canHandle(Project p) {
        UriDesc uridesc = null;

        try {           
            // The project can be handled if it has at least two RDF sources.
            if (hasMultipleRDFSources(p.getSources(), 2)) {
            	// link URL, link label
                uridesc = new UriDesc(this.getName() + "?project=" + p.getUri(),this.label); 
                
                if (this.position > 0) {
                    uridesc.setPosition(this.position);
                }
            }
            
        }
        catch (Exception e) {
            log.fatal("Uh !", e);
            throw new RuntimeException(e);
        }
        return uridesc;
    }
    
    //-------------------------------------------------------------------------
    // Back-end
    //-------------------------------------------------------------------------
    
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
     * Checks whether a value is empty, eg. "", "Aucune" or "None". The value 
     * must be trimmed first.
     * @param val
     * @return
     */
    private boolean isEmptyValue(String val) {
    	return val.isEmpty() || val.equals("Aucune") || val.equals("None");
    }
    
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
     * Checks if a given {@link Source} collection contains valid RDF sources.
     * @param sources The sources to check.
     * @param number The number of RDF sources we want to have.
     * @return True if sources are at least two {@link RdfFileSource}, 
     * {@link TransformedRdfSource} or {@link SparqlSource} sources.
     */
    private boolean hasMultipleRDFSources(Collection<Source> sources, int number) {
    	int cpt=0;
    	for(Source src : sources) {
    		if (isValidSource(src)) {
    			cpt++;
    		}
    	}
    	
    	return cpt >= number;
    }
    
    /**
     * Returns all of the URIs (as strings) from the {@link Source} sources 
     * collection.
     * @param sources sources of our project.
     * @return A LinkedList containing source file's URIs as strings.
     */
    private LinkedList<String> getSourcesURIs(Collection<Source> sources) {
    	LinkedList<String> ret = new LinkedList<String>();
    	for(Source src : sources) {
    		if(isValidSource(src)) {
    			ret.add(src.getUri());
    		}
    	}
    	return ret;
    }
    
    /**
	 * Tels if the bindings of the results are wel-formed.
	 * @param tqr The result of a SPARQL query.
	 * @param bind The result one and only binding.
	 * @return True if the results contains only bind.
	 * @throws QueryEvaluationException Error while closing the result.
	 */
	public final boolean hasCorrectBindingNames(TupleQueryResult tqr, String bind) throws QueryEvaluationException {
		return tqr.getBindingNames().size() == 1 && tqr.getBindingNames().contains(bind);
	}
    
	/**
	 * Sends and evaluates a SPARQL select query on the data set, then returns
	 * the results (which must be one-column only) as a list of Strings.
	 * @param co The {@link RepositoryConnection} to use. 
	 * @param query The SPARQL query without its prefixes.
	 * @param bind The result one and only binding.
	 * @return The query's result as a list of Strings.
	 * @throws RepositoryException Error while accessing the repository.
	 */
    private LinkedList<String> selectQuery(RepositoryConnection co, String query, String bind) {
		TupleQuery tq;
		TupleQueryResult tqr;
		BindingSet bs;
		LinkedList<String> ret = new LinkedList<String>();
		
		if (log.isInfoEnabled()) {
			log.info(MODULE_NAME + " SELECT Query - " + query);
		}
		
		try {
			tq = co.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			
			if (!hasCorrectBindingNames(tqr, bind)) {
				throw new MalformedQueryException("Wrong query result bindings - " + query);
			}
			
			while(tqr.hasNext()) {
				bs = tqr.next();
				ret.add(bs.getValue(bind).stringValue());
			}
		}
		catch (MalformedQueryException e) {
			log.fatal(MODULE_NAME + " SELECT Query - " + query + " - " + e);
		} catch (QueryEvaluationException e) {
			log.fatal(MODULE_NAME + " SELECT Query - " + query + " - " + e);
		} catch (RepositoryException e) {
			log.fatal(MODULE_NAME + " SELECT Query - " + query + " - " + e);
		}
	    return ret;
	}

    //TODO Gérer les versions préfixées des prédicats / classes.
//    /**
//     * Retrieves all of the namespaces used inside a given {@link Repository}.
//     * @param co The RepositoryConnection to be used.
//     * @return HashMap of namespaces ordered by their URI.
//     */
//    private HashMap<String, String> getAllNamespaces(RepositoryConnection co) {
//    	HashMap<String, String> ret = new HashMap<String, String>();
//    	
//    	try {
//			for (Namespace n : co.getNamespaces().asList()) {
//				ret.put(n.getName(), n.getPrefix());
//			}
//    	}
//    	catch (RepositoryException e) {
//    		log.fatal(MODULE_NAME + " Handling namespaces - " + e);
//    	}
//		
//		return ret;
//    }
	
    /**
     * Retrieves all of the classes used inside the repository.
     * @param co The {@link RepositoryConnection} to use. 
     * @return A LinkedList of all of the classes used inside the repository.
     */
	private LinkedList<String> getAllClasses(RepositoryConnection co, LinkedList<String> sourcesURIs) {
		LinkedList<String> ret = new LinkedList<String>();
		String classesQuery;
		for (String srcuri : sourcesURIs) {
			classesQuery = "SELECT DISTINCT ?" + obind + " FROM <" + srcuri + "> WHERE {?" + sbind + " a ?" + obind + "}";
			ret.addAll(selectQuery(co, classesQuery, obind));
		}
		
//		HashMap<String, String> namespaces = getAllNamespaces(co);
//		for (String r : ret) {
//			ret.add(namespaces.get(r.split("#")[0]));
//		}
		
		return ret;
	}
	
	/**
     * Retrieves all of the predicates used inside the repository.
     * @param co The {@link RepositoryConnection} to use. 
     * @return A LinkedList of all of the predicates used inside the repository.
     */
	private LinkedList<String> getAllPredicates(RepositoryConnection co, LinkedList<String> sourcesURIs) {
		LinkedList<String> ret = new LinkedList<String>();
		String predicatesQuery;
		for (String srcuri : sourcesURIs) {
			// SELECT DISTINCT ?o WHERE {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o}
			predicatesQuery = "SELECT DISTINCT ?" + pbind + " FROM <" + srcuri + "> WHERE {?" + sbind + " ?" + pbind + " ?" + obind + "}";
			ret.addAll(selectQuery(co, predicatesQuery, pbind));
			log.info(ret);
		}
		
//		HashMap<String, String> namespaces = getAllNamespaces(co);
//		for (String r : ret) {
//			ret.add(namespaces.get(r.split("#")[0]));
//		}
		
		return ret;
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
        LinkedList<String> sourcesURIs = getSourcesURIs(proj.getSources());
        
        // Retrieve Datalift's internal repository.
        Repository internal = Configuration.getDefault().getInternalRepository();
        RepositoryConnection internalco = internal.newConnection();
        
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("it", proj);
        args.put("sources", sourcesURIs);
        args.put("classes", getAllClasses(internalco, sourcesURIs));
        args.put("predicates", getAllPredicates(internalco, sourcesURIs));
        
        return Response.ok(this.newViewable("/interface.vm", args)).build();
    }
    
    /**
     * Form submit handler : launching StringToURI.
     * @param projectId the project using StringToURI
     * @param sourceDataset 
     * @param targetDataset
     * @param sourcePredicate
     * @param targetPredicate
     * @param sourceType
     * @param targetType
     * @return Our module's completion message.
     * @throws ObjectStreamException
     */
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response doCreate(@QueryParam("project") URI projectId,
    	            	@FormParam("sourcedataset") String sourceDataset,
    		            @FormParam("targetdataset") String targetDataset,
    		            @FormParam("sourcepredicate") String sourcePredicate,
    		            @FormParam("targetpredicate") String targetPredicate,
    		            @FormParam("sourceclass") String sourceClass,
    		            @FormParam("targetclass") String targetClass) throws ObjectStreamException {
    	
        Project proj = this.getProject(projectId);
        
        String repositoryURL = Configuration.getDefault().getInternalRepository().getUrl();
        
        SesameApp test = new SesameApp(repositoryURL, repositoryURL, sourceDataset, targetDataset);
		test.useSimpleLinkage(sourcePredicate, targetPredicate);
		test.useSPARQLOutput(false);
		
        LinkedList<LinkedList<String>> newTriples = test.getOutputAsList();
        
        HashMap<String, Object> args = new HashMap<String, Object>();
	    args.put("it", proj);
	    args.put("sourcedataset", sourceDataset);
	    args.put("targetdataset", targetDataset);
	    args.put("sourcepredicate", sourcePredicate);
	    args.put("targetpredicate", targetPredicate);
	    args.put("sourceclass", sourceClass);
	    args.put("targetclass", targetClass);
	    args.put("newtriples",newTriples);
        return Response.ok(this.newViewable("/result.vm", args)).build();
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
                                    @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        return Configuration.getDefault()
                            .getBean(ResourceResolver.class)
                            .resolveModuleResource(this.getName(),
                                                   uriInfo, request, acceptHdr);
    }

}
