package org.datalift.lov;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.datalift.fwk.MediaTypes.APPLICATION_JSON_UTF8;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;
import org.datalift.lov.service.CheckQueryParam;
import org.datalift.lov.service.LovService;
import org.datalift.lov.service.OfflineLovService;
import org.datalift.lov.service.SearchQueryParam;

/**
 * This modules provide method to search and check vocabularies
 * within the LOV.
 */
@Path(LovModule.MODULE_NAME)
public class LovModule extends BaseModule {

	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------
	private final static Logger log = Logger.getLogger(LovModule.class);

	//TODO see if this has to be changed
	public final static String PROJECT_RESOURCE_PATH = "project";

	public final static String MODULE_NAME = "lov";

    /** The path prefix for HTML page Velocity templates. */
    private final static String TEMPLATE_PATH = "/" + MODULE_NAME  + '/';
    
	//-------------------------------------------------------------------------
	// Instance members
	//-------------------------------------------------------------------------

	/** The DataLift configuration. */
	private Configuration configuration = null;
	/** Project Manager bean. */
	private ProjectManager projectManager = null;
	/** LOV Service */
	private LovService lovService = null;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	public LovModule(){
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
//		lovService = new OnlineLovService();
		log.info("Lov service is using offline repository.");
		lovService = new OfflineLovService(configuration);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------

	/**
	 * The search API allows a user to search over Linked Open Vocabularies
	 * ecosystem for a vocabulary or a vocabulary element (Class or property).
	 * @param query Full text query.
	 * @param type Filter query results on their type.
	 * @param vocSpace Filter query results on a Vocabulary Space an
	 * 				   element/vocabulary belongs to.
	 * @param voc Filter query results on a Vocabulary an element belongs to.
	 * @param offset Offset this number of rows.
	 * @param limit Maximum number of rows to return
	 * @return
	 */
	@GET
	@Path("search")
	@Produces(APPLICATION_JSON)
	public Response searchLov(
			@DefaultValue("") @QueryParam("q") String query,
			@DefaultValue("") @QueryParam("type") String type,
			@DefaultValue("") @QueryParam("vocSpace") String vocSpace,
			@DefaultValue("") @QueryParam("voc") String voc,
			@DefaultValue("0") @QueryParam("offset") int offset,
			@DefaultValue("0") @QueryParam("limit") int limit) {
		
		SearchQueryParam params = new SearchQueryParam();
		params.setQuery(query);
		params.setType(type);
		params.setVocSpace(vocSpace);
		params.setVoc(voc);
		params.setOffset(offset);
		params.setLimit(limit);
		
		lovService.checkLovData();
		
		log.trace("Lov search with parameters : {}", params.getQueryParameters());
		
		return Response.ok(lovService.search(params),
				APPLICATION_JSON_UTF8).build();
	}
	
	/**
	 * The check API allows a user to run the LOV BOT over a distant vocabulary.
	 * @param uri Vocabulary URI to process.
	 * @param timeout Number of seconds after which the process stop.
	 * @return
	 */
	@GET
	@Path("check")
	@Produces(APPLICATION_JSON)
	public Response checkVocab(
			@DefaultValue("") @QueryParam("uri") String uri,
			@DefaultValue("15") @QueryParam("timeout") int timeout) {
		
		if (timeout > 60) timeout = 60;
		CheckQueryParam params = new CheckQueryParam(uri, timeout);
		
		lovService.checkLovData();
		
		log.trace("Lov check with parameters : {}", params.getQueryParameters());
		
		return Response.ok(lovService.check(params),
				APPLICATION_JSON_UTF8).build();
	}
	
	/**
	 * The vocabs API allows a user to get a single vocabulary or the full list
	 * of vocabularies in LOV along with basic information.
	 * @param uri Vocabulary URI to fetch, "" for full list
	 * @return
	 */
	@GET
	@Path("vocabs")
	@Produces(APPLICATION_JSON)
	public Response getAllVocabs(
			@DefaultValue("") @QueryParam("uri") String uri) {
		
		String response = "{}";
		
		lovService.checkLovData();
		
		if (uri.trim().isEmpty()) {
			log.trace("Fetching LOV vocabularies");
			response = lovService.vocabs();
		}
		else {
			log.trace("Fetching LOV vocabulary with uri : {}", uri);
			response = ((OfflineLovService) lovService).vocabWithUri(uri);
		}
		
		return Response.ok(response,
				APPLICATION_JSON_UTF8).build();
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
    
}
