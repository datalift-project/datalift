package org.datalift.lov;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.datalift.fwk.util.StringUtils.urlify;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.util.StringUtils;
import org.datalift.lov.exception.LovModuleException;
import org.openrdf.model.Resource;
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

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift configuration. */
    private Configuration configuration = null;
    /** Project Manager bean. */
    private ProjectManager projectManager = null;
    
	private GraphQueryResult result = null;
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
			
			boolean success = loadLOVCatalogue();
			
		} catch (Exception e1) {
			log.fatal("Some serious error occured while syncing the catalog");
			e1.printStackTrace();
		}
		
		if (proj != null) {
			//TODO to be done only if the project exists!
		}
		try {
			if (!cacheExists) {
				log.debug("No cache of the LOV catalog exists...will cache a copy now...");
				cacheLov();
			}
			
			if (cache != null && cache.size() > 0) {
				log.debug("Cache of LOV catalog already exists");
				response = Response.ok()
						.entity(this.newViewable("/ontologyBrowse.vm", cache))
						.type(TEXT_HTML).build();
			}
			//TODO else -> an error page?
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
	public Response ontologyBrowseUpload(@PathParam("id") String id,
			@FormParam("source_url") URL srcUrl,
			@FormParam("title") String title, @Context UriInfo uriInfo)
			throws WebApplicationException {
		
		//TODO form parameter or whatever input should be a list
		
		Response response = null;
		try {
			log.info("inside browse upload...");
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
//			Project p = this.loadProject(projectUri);
//			 p.addOntology(this.projectManager.newOntology(srcUrl.toURI(),
//			 title));
//			this.projectManager.saveProject(p);
			String redirectUrl = projectUri.toString() + "#ontology";
			response = Response
					.ok(this.newViewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
			log.info("No errors encountered...ontology page should be visible");
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
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?vocabPrefix ?vocabTitle ?vocabURI ?vocabDescr ");
		query.append(" FROM <http://labs.mondeca.com/lov>  ");
		query.append(" WHERE { ");
		query.append("?vocabURI a <http://labs.mondeca.com/vocab/voaf#Vocabulary>.");
		query.append(" ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocabPrefix.");
		query.append("?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle.");
		query.append(" ?vocabURI <http://purl.org/dc/terms/description> ?vocabDescr.");
		query.append(" }");
		query.append(" ORDER BY ?vocabPrefix ");
		
		TupleQuery sparqlQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		TupleQueryResult result = sparqlQuery.evaluate();
		List<String> bindingFields = result.getBindingNames();
		
		while(result.hasNext()){
			BindingSet set = result.next();
			OntologyDesc ontologyDesc = new OntologyDesc();
			ontologyDesc.setPrefix(formatLiteral(set.getValue(bindingFields.get(0)).toString()));
			ontologyDesc.setName(formatLiteral(set.getValue(bindingFields.get(1)).toString()));
			ontologyDesc.setUrl(set.getValue(bindingFields.get(2)).toString());
			ontologyDesc.setDescription(formatLiteral(set.getValue(bindingFields.get(3)).toString()));
			cache.add(ontologyDesc);
		}
		
		log.info("Caching is done.");
		cacheExists = true;
	}

	/**
	 * Queries the LOV sparql end point and loads a copy of the LOV catalogue in
	 * the internal store
	 * 
	 * @throws Exception
	 */
	
	private boolean loadLOVCatalogue() throws Exception{
		
		boolean isSuccessful = false;
		
		//TODO remove hard coding and put it in properties file
		log.info("Starting the LOV catalog sync process...");
		String endpointURL = "http://lov.okfn.org/endpoint/lov/repositories/";
		HTTPRepository lovEndPoint = 
		         new HTTPRepository(endpointURL, "");
		lovEndPoint.initialize();

		RepositoryConnection lovRepositoryConnection = 
		         lovEndPoint.getConnection();
		try {
			StringBuilder sparqlQuery = new StringBuilder();
			sparqlQuery.append("CONSTRUCT { ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocabPrefix. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/description> ?vocabDescr. }");
			sparqlQuery.append(" WHERE {	");
			sparqlQuery.append(" ?vocabURI a <http://labs.mondeca.com/vocab/voaf#Vocabulary>. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocabPrefix. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/title> ?vocabTitle. ");
			sparqlQuery.append(" ?vocabURI <http://purl.org/dc/terms/description> ?vocabDesc");
			sparqlQuery.append(" } ");

			log.info("Querying the sparql end point...");
			GraphQuery query = lovRepositoryConnection.prepareGraphQuery(QueryLanguage.SPARQL, sparqlQuery.toString());
			result = query.evaluate();
			
			List<Statement> statements = new ArrayList<Statement>();
			
		  
		  while (result.hasNext()) {
			 statements.add(result.next());
		  }
		  log.info("Got the result...size -> " + statements.size());
		}
		
		catch(Exception e){
			log.fatal("Failed to query the end point at {} ", endpointURL);
			isSuccessful = false;
//			throw new LovModuleException("Error querying the sparql end point", e);
			}
		finally {
		  Util.CloseQuietly(lovRepositoryConnection);
		}
		
		log.info("Finished querying the sparql end point ...adding result data to the internal repository");
		URI lovContextURI = new URI("http://labs.mondeca.com/lov");
		RepositoryConnection internalRepositoryConnection = this.configuration.getInternalRepository().newConnection();
		
		try {
			org.openrdf.model.URI ctx = null;
			if (lovContextURI != null) {
				ctx = internalRepositoryConnection.getValueFactory()
						.createURI(lovContextURI.toString());
				internalRepositoryConnection.clear(ctx);
			}

			internalRepositoryConnection.add(result, ctx);
//			internalRepositoryConnection.commit();
			System.out.println("internal repository size" + internalRepositoryConnection.size((Resource)null));
			log.info(" internal repository size" + internalRepositoryConnection.size((Resource)null));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Util.CloseQuietly(internalRepositoryConnection);
		}


		log.info("[INFO] Done. But is the querying the sparql endpoint successful?  {}", isSuccessful );
		
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
//			TechnicalException error = new TechnicalException(
//					"ws.internal.error", e, e.getMessage());
//			log.error(error.getMessage(), e);
//			throw new WebApplicationException(Response
//					.status(Status.INTERNAL_SERVER_ERROR)
//					.type(MediaTypes.TEXT_PLAIN_TYPE)
//					.entity(error.getMessage()).build());
		}
	}

	private Project loadProject(URI uri) throws WebApplicationException {
		Project p = this.findProject(uri);
		if (p == null) {
			// Not found.
			throw new NotFoundException(uri);
		}
		return p;
	}
	
	
	private URI newProjectId(URI baseUri, String name) {
		try {
			return new URL(baseUri.toURL(), "workspace/project/" + urlify(name))
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
	private String formatLiteral(String name){
		return name.substring(1, name.lastIndexOf("\""));
		
	}
}
