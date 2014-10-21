package org.datalift.lov;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.datalift.fwk.util.StringUtils.urlify;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;
import org.datalift.lov.exception.LovCatalogueException;
import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.http.HTTPRepository;


/**
 * This module lets end user select ontologies by browsing the 
 * LOV catalog.
 * 
 */
@Path(LovCatalogue.MODULE_NAME)
public class LovCatalogue extends BaseModule {

	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger(LovCatalogue.class);

	//TODO see if this has to be changed
	public final static String PROJECT_RESOURCE_PATH = "project";

	public final static String MODULE_NAME = "lov-catalogue";

	private final static String LOV_CONTEXT = "http://lov.okfn.org/endpoint/lov_aggregator";
	private final static String LOV_CONTEXT_SPARQL = "<" + LOV_CONTEXT + ">";
	
    /** The path prefix for HTML page Velocity templates. */
    private final static String TEMPLATE_PATH = "/" + MODULE_NAME  + '/';
    
    private final static int DAYS_TO_UPDATE = 7;
    
	//-------------------------------------------------------------------------
	// Instance members
	//-------------------------------------------------------------------------

	/** The DataLift configuration. */
	private Configuration configuration = null;
	/** Project Manager bean. */
	private ProjectManager projectManager = null;

	private Date nextLovUpdate = null;
	private List<Statement> statements = null;
	private Set<OntologyDesc> cache = null;
	private boolean cacheExists = false;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	public LovCatalogue(){
		super(MODULE_NAME);
		
		cache = new HashSet<OntologyDesc>();
		
		if(nextLovUpdate == null) {
			nextLovUpdate = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(nextLovUpdate);
			cal.add(Calendar.DATE, DAYS_TO_UPDATE);
			nextLovUpdate = cal.getTime();
		}
		
	}

	//-------------------------------------------------------------------------
	// Module contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public void postInit(Configuration configuration) {
		this.configuration  = configuration;
		this.projectManager = configuration.getBean(ProjectManager.class);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------

	/**
	 * Display the LOV catalog. They are obtained by querying the LOV SPARQL
	 * endpoint and caching them in the internal store. They are re-queried
	 * when user asks for a refresh
	 */
	@GET
	@Path("{id}/ontologyBrowse")
	public Response ontologyUpload(@PathParam("id") String id,
	                               @Context UriInfo uriInfo)
						 throws WebApplicationException {
		log.trace("Entering ontologyBrowse()");
		Response response = null;
		boolean dataChanged = false;
		try {
			// Checking if data needs an update
			Date now = new Date();
			if(now.after(nextLovUpdate)) {
				log.info("LOV data are too old. Updating LOV catalog...");
				loadLOVCatalogue();
				dataChanged = true;
				Calendar cal = Calendar.getInstance();
				cal.setTime(now);
				cal.add(Calendar.DATE, DAYS_TO_UPDATE);
				nextLovUpdate = cal.getTime();
				log.info("Next LOV catalog update: {}", nextLovUpdate);
			}
			else {
				// Checking if data exists in repository
				long repositorySize = 0;
				URI lovContextURI = new URI(LOV_CONTEXT);
				RepositoryConnection internalRepositoryConnection = this.configuration.getInternalRepository().newConnection();
				org.openrdf.model.URI ctx = null;
				ctx = internalRepositoryConnection.getValueFactory().createURI(lovContextURI.toString());
				repositorySize = internalRepositoryConnection.size(ctx);
				if(repositorySize <= 0) {
					log.debug("No data found. Loading LOV catalog...");
					loadLOVCatalogue();
					dataChanged = true;
				}
				else {
				    log.trace("Repository size for LOV catalog context: {}",
				              Long.valueOf(repositorySize));
				}
			}
		} catch (Exception e1) {
			this.handleInternalError(e1, "Some serious error occurred while synchronizing the catalog");
		}

		URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
		Project proj = this.projectManager.findProject(projectUri);
		if (proj != null) {
			//TODO to be done only if the project exists!
		}

		try {
			if ( ! cacheExists || dataChanged) {
				log.trace("No cache or new data. Caching now...");
				cacheLov();
			}
			if (cache.size() > 0) {
				log.trace("Cache of LOV catalog already exists");
				TemplateModel view = this.newView("/ontologyBrowse.vm", cache);
				view.put("projectId", id);
				response = Response.ok(view, TEXT_HTML).build();
			}
			else {
				log.warn("Loading view with no data.");
				TemplateModel view = this.newView("/ontologyBrowse.vm",
						new HashSet<OntologyDesc>());
				view.put("projectId", id);
				response = Response.ok(view, TEXT_HTML).build();
			}
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load LOV catalog");
		}
		return response;
	}

	/**
	 * Upload the ontologies selected(can be multiple) by the browsing the LOV
	 * catalog
	 */
	@POST
	@Path("{id}/ontologyBrowseUpload")
	//@Consumes("application/x-www-form-urlencoded")
	public Response ontologyBrowseUpload(@PathParam("id") String projectId,
	                                     @FormParam("source_url") List<URL> srcUrl,
	                                     @FormParam("title") List<String> title,
	                                     @Context UriInfo uriInfo)
							 throws WebApplicationException {
		log.debug("ontologyBrowseUpload web service");
		log.debug("source_url: {}, title: {}", srcUrl, title);

		Response response = null;
		try {
			// Retrieve project.
			log.debug("Loading project {} with base uri {}", projectId, uriInfo.getBaseUri());
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
			Project p = this.loadProject(projectUri);
			
			Iterator<URL> urlIt = srcUrl.iterator();
			Iterator<String> titleIt = title.iterator();
			
			//Let's just hope that they are still in the same order...
			while(urlIt.hasNext() && titleIt.hasNext()) {
				
				URL url = urlIt.next();
				String t = titleIt.next();
				
				// If it does not already exist
				if(p.getOntology(t) == null) {
					// Add ontology to project.
					p.addOntology(this.projectManager.newOntology(p, url.toURI(), t));
				}
			}
			
			// Persist new ontologies.
			this.projectManager.saveProject(p);
			
			String redirectUrl = projectUri.toString() + "#ontology";
			
			log.trace("Building response. Redirect URL: {}", redirectUrl);
			response = Response.ok(this.newView("/redirect.vm", redirectUrl))
			                   .type(TEXT_HTML).build();
			
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to add ontology {}", srcUrl);
		}
		return response;
	}

	@POST
	@Path("{id}/ontologySearch")
	public Response ontologySearchUpload(@PathParam("id") String id,
			@FormParam("source_url") URL srcUrl,
			@FormParam("title") String title, @Context UriInfo uriInfo)
					throws WebApplicationException {

		Response response =  null;

		return response;
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

	//-------------------------------------------------------------------------
	// Specific implementation
	//-------------------------------------------------------------------------

	/**
	 * Returns the {@link ProjectManager} module used for accessing the
	 * DataLift projects.
	 * @return the {@link ProjectManager} object.
	 */
	public ProjectManager getProjectManager() {
		return this.projectManager;
	}

	/**
	 * Returns the DataLift {@link Configuration}.
	 * @return the DataLift {@link Configuration}.
	 */
	public Configuration getConfiguration() {
		return this.configuration;
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
    protected final TemplateModel newView(String templateName, Object it) {
        return ViewFactory.newView(TEMPLATE_PATH + templateName, it);
    }

	/**
	 * Caches a copy of the LOV catalog from the internal store
	 * @throws MalformedQueryException 
	 * @throws Exception 
	 * @throws LovCatalogueException
	 * @throws URISyntaxException
	 */
	private void cacheLov() throws Exception{
		log.trace("Caching LOV ontology catalog from the internal repository...");

		cache = new HashSet<OntologyDesc>();
		RepositoryConnection connection = this.configuration.getInternalRepository().newConnection();

		// Query to select some info from the internal repository with the lov context
		StringBuilder query = new StringBuilder();
		query.append("SELECT DISTINCT ?vocabURI ?vocabPrefix ?vocabTitle ?vocabDescr ");
		query.append("FROM " + LOV_CONTEXT_SPARQL);
		query.append(" WHERE { ");
		query.append(" ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocabPrefix.");
		query.append(" ?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle.");
		query.append(" OPTIONAL {");
		query.append(" ?vocabURI <http://purl.org/dc/terms/description> ?vocabDescr.");
		query.append(" }");
		query.append(" }");
		query.append(" ORDER BY ?vocabPrefix ");

		TupleQuery sparqlQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		TupleQueryResult result = sparqlQuery.evaluate();
		List<String> bindingFields = result.getBindingNames();
		
		while(result.hasNext()) {
			BindingSet set = result.next();
			OntologyDesc ontologyDesc = new OntologyDesc();
			ontologyDesc.setUrl(set.getValue(bindingFields.get(0)).toString());
			ontologyDesc.setPrefix(formatLiteral(set.getValue(bindingFields.get(1)).toString()));
			ontologyDesc.setName(set.getValue(bindingFields.get(2)).toString());
			
			// Description might not exist (foaf for instance)
			if(set.getValue(bindingFields.get(3)) != null) {
				ontologyDesc.setDescription(set.getValue(bindingFields.get(3)).toString());
			}
			else {
				ontologyDesc.setDescription("");
			}
			cache.add(ontologyDesc);
		}
		
		if (cache.size() > 0) {
			log.info("LOV memory cache loaded ({} triples)", Integer.valueOf(cache.size()));
			cacheExists = true;
		}
	}

	/**
	 * Queries the LOV SPARQL endpoint and loads a copy of the LOV catalog in
	 * the internal store
	 * 
	 * @throws Exception
	 */
	private boolean loadLOVCatalogue() throws Exception{

		boolean isSuccessful = true;

		//TODO remove hard coding and put it in properties file
//		String endpointURL = "http://lov.okfn.org/endpoint/lov/repositories/";
		String endpointURL = this.configuration.getInternalRepository().getEndpointUrl();
		HTTPRepository lovEndPoint = new HTTPRepository(endpointURL);
		lovEndPoint.initialize();
		log.debug("Starting the LOV catalog sync process using SPARQL endpoint \"{}\"...", endpointURL);
	
		RepositoryConnection lovRepositoryConnection = lovEndPoint.getConnection();
		try {
			StringBuilder sparqlQuery = new StringBuilder();
			sparqlQuery.append("PREFIX dcterms:<http://purl.org/dc/terms/>");
			sparqlQuery.append("PREFIX voaf:<http://purl.org/vocommons/voaf#>");
			sparqlQuery.append("PREFIX vann:<http://purl.org/vocab/vann/>");
			sparqlQuery.append("CONSTRUCT { ");
			sparqlQuery.append(" ?vocabURI vann:preferredNamespacePrefix ?vocabPrefix. ");
			sparqlQuery.append(" ?vocabURI dcterms:title ?vocabTitle. ");
			sparqlQuery.append(" ?vocabURI dcterms:description ?vocabDescr. }");
			sparqlQuery.append(" WHERE {	");
			sparqlQuery.append(" ?vocabURI a voaf:Vocabulary. ");
			sparqlQuery.append(" ?vocabURI vann:preferredNamespacePrefix ?vocabPrefix. ");
			sparqlQuery.append(" ?vocabURI dcterms:title ?vocabTitle. ");
			sparqlQuery.append(" OPTIONAL {");
			sparqlQuery.append(" ?vocabURI dcterms:description ?vocabDescr.");
			sparqlQuery.append(" } ");
			sparqlQuery.append(" } ");

			GraphQuery query = lovRepositoryConnection.prepareGraphQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
			GraphQueryResult result = query.evaluate();
			
			statements = new ArrayList<Statement>();
			// Note: result can't be iterated over another time
			while (result.hasNext()) {
				statements.add(result.next());
			}
			result.close();
		}
		catch(Exception e) {
			log.fatal("Failed to query SPARQL endpoint at {}", e, endpointURL);
			isSuccessful = false;
			//throw new LovModuleException("Error querying the SPARQL endpoint", e);
		}
		finally {
			Util.CloseQuietly(lovRepositoryConnection);
		}

		log.trace("Finished querying the SPARQL endpoint. Adding result data to the internal repository.");
		URI lovContextURI = new URI(LOV_CONTEXT);
		RepositoryConnection internalRepositoryConnection = this.configuration.getInternalRepository().newConnection();
		try {
			org.openrdf.model.URI ctx = null;
			ctx = internalRepositoryConnection.getValueFactory()
					.createURI(lovContextURI.toString());
			internalRepositoryConnection.clear(ctx);

			log.debug("Adding {} statements to context <{}>",
			          Integer.valueOf(statements.size()), ctx);
			internalRepositoryConnection.add(statements, ctx); // auto commit
			log.info("Internal repository size for context <{}>: {}", ctx,
			         Long.valueOf(internalRepositoryConnection.size(ctx)));

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Util.CloseQuietly(internalRepositoryConnection);
		}
		return isSuccessful;
	}

	private Project findProject(URI uri) throws WebApplicationException {
		try {
			return this.projectManager.findProject(uri);
		} catch (Exception e) {
			throw new WebApplicationException(Response
					.status(Status.INTERNAL_SERVER_ERROR)
					.type(MediaTypes.TEXT_PLAIN_TYPE)
					.entity(e.getMessage()).build());
		}
	}

	private Project loadProject(URI uri) throws WebApplicationException {
		log.trace("Loading project: <{}>", uri);
		Project p = this.findProject(uri);
		if (p == null) {
			// Not found.
			log.warn("Project not found <{}> :(", uri);
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		return p;
	}


	private URI newProjectId(URI baseUri, String name) {
		try {
			URI u = new URL(baseUri.toURL(), "project/" + urlify(name)).toURI();
			log.trace("new Project Id: {}", u);
			return u;
		} catch (Exception e) {
			throw new RuntimeException("Invalid base URI: " + baseUri);
		}
	}

	private void handleInternalError(Exception e, String logMsg,
			Object... logArgs) throws WebApplicationException {
		if (e instanceof WebApplicationException) {
			throw (WebApplicationException) e;
		}
		else {
			if (StringUtils.isSet(logMsg)) {
				log.fatal(logMsg, e, logArgs);
			} else {
				log.fatal(e.getMessage(), e);
			}
			//			TechnicalException error = new TechnicalException(
			//					"ws.internal.error", e, e.getMessage());
			throw new WebApplicationException(Response
					.status(Status.INTERNAL_SERVER_ERROR)
					.type(MediaTypes.TEXT_PLAIN_TYPE)
					.entity(e.getMessage()).build());
		}
	}

	//Remove quotes and language information from literals
	private String formatLiteral(String name) {
		return name.substring(1, name.length() - 1);
	}
}
