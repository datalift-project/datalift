package org.datalift.lov;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.datalift.fwk.util.StringUtils.urlify;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
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
import org.datalift.lov.exception.LovModuleException;
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

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;


/**
 * This module lets end user select ontologies by browsing the 
 * LOV catalogue.
 * 
 */
@Path(BrowseOntologiesModule.MODULE_NAME)
public class BrowseOntologiesModule extends BaseModule {

	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger(BrowseOntologiesModule.class);

	//TODO see if this has to be changed
	public final static String PROJECT_RESOURCE_PATH = "project";

	public final static String MODULE_NAME = "lovbrowser";

	private final static String LOV_CONTEXT = "http://lov.okfn.org/endpoint/lov";
	private final static String LOV_CONTEXT_SPARQL = "<" + LOV_CONTEXT + ">";
	
    /** The path prefix for HTML page Velocity templates. */
    private final static String TEMPLATE_PATH = "/" + MODULE_NAME  + '/';

	//-------------------------------------------------------------------------
	// Instance members
	//-------------------------------------------------------------------------

	/** The DataLift configuration. */
	private Configuration configuration = null;
	/** Project Manager bean. */
	private ProjectManager projectManager = null;

	private List<Statement> statements = null;
	private Set<OntologyDesc> cache = null;
	private boolean cacheExists = false;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	public BrowseOntologiesModule(){
		super(MODULE_NAME);
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
	 * <i>[Resource method]</i> Redirects the client to the
	 * {@link ProjectResource project resource} index page, using a
	 * 301 (Moved permanently) HTTP status.
	 * @param  uriInfo   the requested URI.
	 *
	 * @return a 301 redirection response to the project resource
	 *         index page.
	 */
	//    @GET
	//    public Response getIndex(@Context UriInfo uriInfo) {
	//        URI target = uriInfo.getRequestUriBuilder()
	//                            .path(PROJECT_RESOURCE_PATH).build();
	//        return Response.status(Status.MOVED_PERMANENTLY)
	//                       .location(target)
	//                       .build();
	//    }

	/**
	 * Display the LOV catalog. They are obtained by querying the LOV sparql
	 * end point and caching them in the internal store. They are re-queried
	 * when user asks for a refresh
	 */
	@GET
	@Path("{id}/ontologyBrowse")
	public Response ontologyUpload(@PathParam("id") String id,
								   @Context UriInfo uriInfo)
										   throws WebApplicationException {

		log.info("Entering ontologyBrowse()");
		Response response = null;

		URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
		Project proj = this.projectManager.findProject(projectUri);

		try {

			loadLOVCatalogue();

		} catch (Exception e1) {

			log.fatal("Some serious error occured while syncing the catalog");
			e1.printStackTrace();

		}

		if (proj != null) {
			//TODO to be done only if the project exists!
		}

		try {

			if (!cacheExists) {
				log.debug("No cache of the LOV catalog exists. Will cache a copy now...");
				cacheLov();
			}

			if (cache != null && cache.size() > 0) {

				log.debug("Cache of LOV catalog already exists");
				
				TemplateModel view = this.newView("/ontologyBrowse.vm", cache);
				view.put("projectId", id);
				
				response = Response.ok(view, TEXT_HTML).build();
			}
			else {
				//TODO else -> an error page?
				response = Response.serverError().build();
			}

		} catch (Exception e) {
			this.handleInternalError(e, "Failed to add ontology {}");
		}

		return response;
	}

	/**
	 * Upload the ontologies selected(can be multiple) by the browsing the LOV
	 * catalogue
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
		log.debug("source_url : {}, title : {}", srcUrl, title);

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
			
			log.debug("Building response. Redirect URL : {}", redirectUrl);
			response = Response
					.ok(this.newViewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
			
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to add browse ontology {}", srcUrl);
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
	protected final Viewable newViewable(String templateName, Object it) {
		return new Viewable("/" + this.getName() + templateName, it);
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
	 * @throws LovModuleException
	 * @throws URISyntaxException
	 */
	private void cacheLov() throws Exception{
		log.info("Caching lov ontology catalogue from the internal RDF repository...");
		cache = new HashSet<OntologyDesc>();

		RepositoryConnection connection = this.configuration.getInternalRepository().newConnection();

		// Query to select some info from the internal repository with the lov context
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?vocabURI ?vocabPrefix ?vocabTitle ?vocabDescr ");
		query.append("FROM " + LOV_CONTEXT_SPARQL);
		query.append(" WHERE { ");
		query.append(" ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocabPrefix.");
		query.append(" ?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle.");
		query.append(" ?vocabURI <http://purl.org/dc/terms/description> ?vocabDescr.");
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
			ontologyDesc.setDescription(set.getValue(bindingFields.get(3)).toString());
			cache.add(ontologyDesc);

		}
		
		log.info("Caching is done. Size : {}", cache.size());
		cacheExists = true;
		
	}

	/**
	 * Queries the LOV sparql end point and loads a copy of the LOV catalogue in
	 * the internal store
	 * 
	 * @throws Exception
	 */
	private boolean loadLOVCatalogue() throws Exception{

		boolean isSuccessful = true;

		//TODO remove hard coding and put it in properties file
		log.info("Starting the LOV catalog sync process...");
		String endpointURL = "http://lov.okfn.org/endpoint/lov/repositories/";
		HTTPRepository lovEndPoint = new HTTPRepository(endpointURL);
		lovEndPoint.initialize();

		RepositoryConnection lovRepositoryConnection = lovEndPoint.getConnection();

		try {

			StringBuilder sparqlQuery = new StringBuilder();
			sparqlQuery.append("CONSTRUCT { ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocabPrefix. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/description> ?vocabDescr. }");
			sparqlQuery.append(" WHERE {	");
			sparqlQuery.append(" ?vocabURI a <http://purl.org/vocommons/voaf#Vocabulary>. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocabPrefix. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/description> ?vocabDescr.");
			sparqlQuery.append(" } ");

			log.info("Querying the sparql end point...");
			GraphQuery query = lovRepositoryConnection.prepareGraphQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
			GraphQueryResult result = query.evaluate();
			
			statements = new ArrayList<Statement>();

			// note : result can't be iterated over another time
			while (result.hasNext()) {
				statements.add(result.next());
			}
			
			result.close();

		}
		catch(Exception e) {

			log.fatal("Failed to query the end point at {} ", endpointURL);
			isSuccessful = false;
			//throw new LovModuleException("Error querying the sparql end point", e);

		}
		finally {

			Util.CloseQuietly(lovRepositoryConnection);

		}

		log.info("Finished querying the sparql end point. Adding result data to the internal repository.");
		URI lovContextURI = new URI(LOV_CONTEXT);
		RepositoryConnection internalRepositoryConnection = this.configuration.getInternalRepository().newConnection();

		try {

			org.openrdf.model.URI ctx = null;
			ctx = internalRepositoryConnection.getValueFactory()
					.createURI(lovContextURI.toString());
			internalRepositoryConnection.clear(ctx);


			log.info("Adding {} statements.", statements.size());
			internalRepositoryConnection.add(statements, ctx); // auto commit
			log.info("Internal repository size for context {} : {}", ctx, internalRepositoryConnection.size(ctx));

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			Util.CloseQuietly(internalRepositoryConnection);

		}

		return isSuccessful;

	}
	//	private Map LovData() throws Exception {
	//
	//		Map<String, List> args = new HashMap<String, List>();
	//
	//		SailRepository repo = new SailRepository(new MemoryStore());
	//		repo.initialize();
	//		RepositoryConnection conn = repo.getConnection();
	//		conn.add(new InputStreamReader(new FileInputStream(
	//				"C:\\Users\\karthik\\Downloads\\lov_aggregator.rdf"), "UTF-8"),
	//				RDF.NAMESPACE, RDFFormat.RDFXML);
	//
	//		String sparqlQuery = "SELECT distinct ?vocabURI ?prefix ?vocabTitle WHERE{ ?vocabURI a <http://labs.mondeca.com/vocab/voaf#Vocabulary>.  ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?prefix.  ?vocabURI <http://purl.org/vocab/vann/preferredNamespaceUri> ?vocabNspUri.  ?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle.  FILTER(LANG(?vocabTitle)=\"en\") }ORDER BY ?prefix ";
	//		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
	//				sparqlQuery);
	//
	//		TupleQueryResult result = query.evaluate();
	//		List<String> headerList = result.getBindingNames();
	//
	//		List<BindingSet> ontologies = new ArrayList<BindingSet>();
	//
	//		while (result.hasNext())
	//			ontologies.add(result.next());
	//
	//		args.put("headers", headerList);
	//		args.put("data", ontologies);
	//
	//		return args;
	//
	//	}


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
		log.debug("Loading project - uri : {}", uri);
		Project p = this.findProject(uri);
		if (p == null) {
			// Not found.
			log.debug("project not found :(");
			throw new NotFoundException(uri);
		}
		return p;
	}


	private URI newProjectId(URI baseUri, String name) {
		log.debug("new Project Id");
		try {
			return new URL(baseUri.toURL(), "project/" + urlify(name))
			.toURI();
		} catch (Exception e) {
			throw new RuntimeException("Invalid base URI: " + baseUri);
		}
	}

	private void handleInternalError(Exception e, String logMsg,
			Object... logArgs) throws WebApplicationException {
		if (e instanceof WebApplicationException) {
			throw (WebApplicationException) e;
		}
		if (e instanceof EntityNotFoundException) {
			throw new NotFoundException();
		} else {
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
