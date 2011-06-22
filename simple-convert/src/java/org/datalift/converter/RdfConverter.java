package org.datalift.converter;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;

import com.sun.jersey.api.view.Viewable;

public class RdfConverter extends BaseModule implements ProjectModule
{	
	private final static String MODULE_NAME = "rdfconverter";
	
	private ProjectManager	projectManager = null;
	
    private SparqlEndpoint sparqlEndpoint = null;
	
	private Repository internRepository = null;
		
	@Override
	public URI canHandle(Project p) {
        boolean hasRdfSource = false;
        for (Source s : p.getSources()) {
            if (s instanceof TransformedRdfSource) {
                hasRdfSource = true;
                break;
            }
        }
        URI projectPage = null;
        if (hasRdfSource) {
            try {
                return new URI(this.getName() + "?project=" + p.getUri());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return projectPage;
	}

	public RdfConverter() {
		super(MODULE_NAME, true);
	}
	
	@Override
	public void init(Configuration configuration) {	
		this.internRepository = configuration.getInternalRepository();
        this.projectManager = configuration.getBean(ProjectManager.class);
        if (this.projectManager == null) {
            throw new RuntimeException("Could not retrieve Project Manager");
        }
        this.sparqlEndpoint = configuration.getBean(SparqlEndpoint.class);
        if (this.sparqlEndpoint == null) {
            throw new RuntimeException("Could not retrieve SPARQL endpoint");
        }
	}	
	
	@GET
	public Response getIndexPage(@QueryParam("project") String projectId, @Context UriInfo uriInfo) {
		Project p;
		try {
			p = this.projectManager.findProject(new URL(projectId).toURI());
		} 
		catch (Exception e) {
			throw new RuntimeException("Could not find project with URI " + projectId, e);
		}
		TransformedRdfSource src = null;
		for (Source s : p.getSources()) {
			if (s instanceof TransformedRdfSource) {
				src = (TransformedRdfSource)s;
			}
		}
		if (src == null)
			throw new RuntimeException("Could not find rdf source in project with URI " + projectId);
		return Response.ok(this.newViewable("/constructQueries.vm", p)).build();

	}
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response convertRdfSource(@FormParam("query") List<String> query, 
									 @FormParam("project") URI projectId, 
									 @Context UriInfo uriInfo,
									 @Context Request request,
									 @HeaderParam(ACCEPT) String acceptHdr) {
		Response response = null;
		if (projectId != null && query != null) {
			TransformedRdfSource src = null;
	        Project p = this.projectManager.findProject(projectId);
	        if (p != null) {
	            for (Source s : p.getSources()) {
	                if (s instanceof TransformedRdfSource) {
	                    src = (TransformedRdfSource)s;
	                    // Continue to find last RDF source available...
	                }
	            }
	        }
	        if (src == null) {
	            throw new EntityNotFoundException("No RDF source found");
	        }
			try {
				URI newNamedGraph = new URL(src.getUri() + "-rdf" + p.getSources().size()).toURI();
				RdfUtils.convert(this.internRepository, query,
				        this.internRepository, newNamedGraph);
				TransformedRdfSource newSrc = this.projectManager.newTransformedRdfSource(
						newNamedGraph, src.getTitle() + "-rdf" + p.getSources().size(), 
						newNamedGraph);
				p.addSource(newSrc);
				this.projectManager.saveProject(p);
				response = this.sparqlEndpoint.executeQuery(
                        "SELECT * WHERE { GRAPH <"
                            + newNamedGraph + "> { ?s ?p ?o . } }",
                        uriInfo, request, acceptHdr).build();
			} 
			catch (Exception e) {
			    	response = Response.status(Status.INTERNAL_SERVER_ERROR)
			                               .entity(e.getLocalizedMessage())
			                               .type(MediaType.TEXT_PLAIN_TYPE)
			                               .build();
			}
		}
        return response;
	}
	
	  /**
     * Return a viewable for the specified template, populated with the
     * specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated viewable.
     */
    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }
	
}