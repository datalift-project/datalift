package com.mondeca.datalift.rdf2rdf;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.TechnicalException;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.sparql.SparqlQueries;
import org.openrdf.model.Resource;
import org.openrdf.repository.Repository;

import com.mondeca.datalift.lov.JSONExecutionResult;
import com.mondeca.datalift.rdf2rdf.model.JSONOntologyElements;
import com.mondeca.datalift.rdf2rdf.model.JSONSource;
import com.mondeca.sesame.toolkit.handler.CopyStatementRDFHandler;
import com.mondeca.sesame.toolkit.query.SPARQLExecutionException;
import com.mondeca.sesame.toolkit.query.SesameSPARQLExecuter;
import com.mondeca.sesame.toolkit.script.Script;
import com.mondeca.sesame.toolkit.template.TemplateRegistry;
import com.sun.jersey.api.view.Viewable;

@Path(RDFTransformService.MODULE_NAME)
public class RDFTransformService extends BaseModule implements ProjectModule {

	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------

	/** Base name of the resource bundle for converter GUI. */
	protected final static String GUI_RESOURCES_BUNDLE = "com.mondeca.datalift.resources";

	private final static Logger log = Logger.getLogger();

	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "rdf-transform";

	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	/** The requested module position in menu. */
	protected final int position;
	/** The SPARQL queries used by this module */
	private SparqlQueries queries;
	/** The DataLift project manager. */
	protected ProjectManager projectManager = null;

	protected LabelFetcher labelFetcher;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/** Default constructor. */
	public RDFTransformService() {
		super(MODULE_NAME);
		this.queries = new SparqlQueries(this);
		this.position = 100;
	}

	//-------------------------------------------------------------------------
	// Module contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public void postInit(Configuration configuration) {
		System.out.println("RDF-Transform post init phase");
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getLogger(SesameSPARQLExecuter.class).setLevel(Level.ALL);
		
		super.postInit(configuration);

		this.projectManager = configuration.getBean(ProjectManager.class);
		if (this.projectManager == null) {
			throw new RDFTransformException("project.manager.not.available");
		}

		// create label fetcher and keep it in session
		org.datalift.fwk.rdf.Repository internal = Configuration.getDefault().getInternalRepository();
		Repository repository = internal.getNativeRepository();
		this.labelFetcher = new LabelFetcher(this.queries, repository);
		
		// register templates
		// TemplateRegistry.registerPackage("com.mondeca.datalift.rdf2rdf.templates");
		com.mondeca.sesame.toolkit.template.Template moveTypeTemplate = new com.mondeca.sesame.toolkit.template.Template();
		moveTypeTemplate.setName("MOVE_CLASS");
		moveTypeTemplate.setDisplayName("Translate class to another");
		moveTypeTemplate.setBody("DELETE { ?s a ?source } INSERT { ?s a ?target } WHERE { ?s a ?source }");
		
		com.mondeca.sesame.toolkit.template.Argument sourceParam = new com.mondeca.sesame.toolkit.template.Argument();
		sourceParam.setVarName("source");
		sourceParam.setDisplayName("Source type");
		sourceParam.setMandatory(true);
		sourceParam.setOrder(0);
		moveTypeTemplate.addArgument(sourceParam);
		
		com.mondeca.sesame.toolkit.template.Argument targetParam = new com.mondeca.sesame.toolkit.template.Argument();
		targetParam.setVarName("target");
		targetParam.setDisplayName("Target type");
		targetParam.setMandatory(true);
		targetParam.setOrder(1);
		moveTypeTemplate.addArgument(targetParam);
		
		TemplateRegistry.register(moveTypeTemplate);
		
		com.mondeca.sesame.toolkit.template.Template movePredicateTemplate = new com.mondeca.sesame.toolkit.template.Template();
		movePredicateTemplate.setName("MOVE_PREDICATE");
		movePredicateTemplate.setDisplayName("Translate predicate to another");
		movePredicateTemplate.setBody("DELETE { ?s ?source ?o } INSERT { ?s ?target ?o } WHERE { ?s ?source ?o }");
		
		com.mondeca.sesame.toolkit.template.Argument sourcePredicateParam = new com.mondeca.sesame.toolkit.template.Argument();
		sourcePredicateParam.setVarName("source");
		sourcePredicateParam.setDisplayName("Source type");
		sourcePredicateParam.setMandatory(true);
		sourcePredicateParam.setOrder(0);
		movePredicateTemplate.addArgument(sourcePredicateParam);
		
		com.mondeca.sesame.toolkit.template.Argument targetPredicateParam = new com.mondeca.sesame.toolkit.template.Argument();
		targetPredicateParam.setVarName("target");
		targetPredicateParam.setDisplayName("Target type");
		targetPredicateParam.setMandatory(true);
		targetPredicateParam.setOrder(1);
		movePredicateTemplate.addArgument(targetPredicateParam);
		
		TemplateRegistry.register(movePredicateTemplate);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------

	@Override
	public UriDesc canHandle(Project p) {
		UriDesc projectPage = null;
		try {
			if (this.findSource(p, false) != null) {
				try {
					String label = PreferredLocales.get()
							.getBundle(GUI_RESOURCES_BUNDLE, this)
							.getString(this.getName() + ".module.label");

					projectPage = new UriDesc(
							this.getName() + "/rdf2rdf.html?project=" + p.getUri(),
							HttpMethod.GET, label);
					if (this.position > 0) {
						projectPage.setPosition(this.position);
					}
				}
				catch (Exception e) {
					System.out.println("RDF-Transform exception : "+e.getMessage());
					throw new RDFTransformException(e);
				}
			}
		}
		catch (Exception e) {
			log.fatal("Failed to check status of project {}: {}", e,
					p.getUri(), e.getMessage());
		}
		System.out.println("RDF-Transform canHandle on project "+p.getTitle()+" returns "+projectPage);
		return projectPage;
	}

	/**
	 * Searches the specified project for a source matching the expected
	 * input source type of the module.
	 * @param  p          the project.
	 * @param  findLast   whether to return the last matching source or
	 *                    the first.
	 * @return a matching source of <code>null</code> if no matching
	 *         source was found.
	 */
	protected final Source findSource(Project p, boolean findLast) {
		if (p == null) {
			throw new IllegalArgumentException("p");
		}
		Source src = null;
		for (Source s : p.getSources()) {
			if (this.canHandle(s)) {
				src = s;
				if (! findLast) break;
				// Else: continue to get last source of type in project...
			}
		}
		return src;
	}

	/**
	 * Returns whether the specified source can be handled by this
	 * converter.
	 * @param  s   the source.
	 *
	 * @return <code>true</code> if this converter supports the source;
	 *         <code>false</code> otherwise.
	 */
	public boolean canHandle(Source s) {
		System.out.println("RDF-Transform canHandle "+s.getType());
		return s.getType().equals(SourceType.TransformedRdfSource);
	}

	/**
	 * Retrieves a {@link Project} using its URI.
	 * @param  projectId   the project URI.
	 *
	 * @return the project.
	 * @throws ObjectNotFoundException if the project does not exist.
	 */
	protected final Project getProject(URI projectId)
			throws RDFTransformException {
		Project p = this.projectManager.findProject(projectId);
		if (p == null) {
			throw new RDFTransformException("project.not.found", projectId);
		}
		return p;
	}


	@GET
	@Path("{path: .*$}")
	public Object getStaticResource(
			@PathParam("path") String path,
			@Context UriInfo uriInfo,
			@Context Request request,
			@HeaderParam(ACCEPT) String acceptHdr)
					throws WebApplicationException {
		return Configuration.getDefault()
				.getBean(ResourceResolver.class)
				.resolveModuleResource(
						this.getName(),
						uriInfo,
						request,
						acceptHdr
						);
	}


	public static Response serializeJSON(Object o, String callback, String JSONRoot) {
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		mapper.enable(Feature.INDENT_OUTPUT);
		mapper.disable(Feature.FAIL_ON_EMPTY_BEANS);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			mapper.writeValue(baos, o);
			System.out.println(baos.toString());
		} catch (Exception e) {
			throw new RDFTransformException("Exception when serializing to JSON", e);
		}

		String finalResult = (JSONRoot != null)?"{"+JSONRoot+" : "+baos.toString()+" }":baos.toString();
		finalResult = (callback != null)?callback+"("+finalResult+")":finalResult;
		return Response.ok(finalResult).build();
	}

	@GET
	@Path("sources")
	@Produces(MediaTypes.APPLICATION_JSON)
	public Response getSources(@QueryParam("callback") String callback, @QueryParam("project") URI projectId) {

		List<JSONSource> result = new ArrayList<JSONSource>();

		// Retrieve project.
		Project p = this.getProject(projectId);

		if(p == null) {
			return null;
		}

		for (Source source : p.getSources()) {
			if(this.canHandle(source)) {
				result.add(new JSONSource(source));
			}
		}

		return serializeJSON(result, callback, "graphs");
	}

	@GET
	@Path("ontology")
	@Produces(MediaTypes.APPLICATION_JSON)
	public Response getOntologyElements(@QueryParam("callback") String callback, @QueryParam("source") String sourceURI) {
		org.datalift.fwk.rdf.Repository internal = Configuration.getDefault().getInternalRepository();
		Repository repository = internal.getNativeRepository();

		System.out.println("Getting ontology elements on source "+sourceURI);
		
		SesameSPARQLExecuter executer = new SesameSPARQLExecuter(repository);
		if(sourceURI != null) {
			executer.setDefaultGraphs(Collections.singleton(URI.create(sourceURI)));
		}

		SPARQLHelperCommunity.AllClassesHelper allClassesHelper = (new SPARQLHelperCommunity()).new AllClassesHelper(this.queries,repository, this.labelFetcher);
		try {
			executer.executeSelect(allClassesHelper);
			executer.executeSelect((new SPARQLHelperCommunity()).new SuperTypeHelper(this.queries, repository, allClassesHelper.getClasses()));
		} catch (SPARQLExecutionException e) {
			throw new RDFTransformException();
		}

		SPARQLHelperCommunity.AllPropertiesHelper allPropertiesHelper = (new SPARQLHelperCommunity()).new AllPropertiesHelper(this.queries, repository, this.labelFetcher);
		try {
			executer.executeSelect(allPropertiesHelper);
			executer.executeSelect((new SPARQLHelperCommunity()).new SuperPropertyHelper(this.queries, repository, allPropertiesHelper.getProperties()));
		} catch (SPARQLExecutionException e) {
			throw new RDFTransformException();
		}

		return serializeJSON(new JSONOntologyElements(allClassesHelper.getClasses(), allPropertiesHelper.getProperties()), callback, null);
	}


//	@GET
//	@Path("testRule")
//	@Produces(MediaTypes.APPLICATION_JSON)
//	public Response testRule(
//			@QueryParam("callback") String callback,
//			@QueryParam("rule") String ruleToTest
//			) {
//		Repository repository = Configuration.getDefault().getInternalRepository().getNativeRepository();			
//
//		SesameSPARQLExecuter executer = new SesameSPARQLExecuter(repository);
//
//		return serializeJSON(null, callback, null);
//	}

	@POST
	@Path("execute")
	public Response executeScript(
			@QueryParam("project") URI projectId,
			@QueryParam("source") URI sourceId,
			@QueryParam("callback") String callback,
			@FormParam("dest_title") String destTitle,
			@FormParam("dest_graph_uri") String targetGraph,
			@FormParam("script") String script)
					throws WebApplicationException {

		System.out.println(
				"RDF-Transform executing script '"+script+"'" +
						" on project '"+projectId+"'," +
								" source '"+sourceId+"'," +
										" to target '"+targetGraph+"'" +
												" with title '"+destTitle+"'");
		
		Repository internal = Configuration.getDefault().getInternalRepository().getNativeRepository();
		try {
			if ((script == null) || (script.equals(""))) {
				RDFTransformException error = new RDFTransformException("ws.missing.param", "script");
				throw new WebApplicationException(
						Response.status(Status.BAD_REQUEST)
						.type(MediaTypes.TEXT_PLAIN_TYPE)
						.entity(error.getMessage()).build()
						);
			}
			// Retrieve project.
			Project p = this.getProject(projectId);

			// clear target graph
			System.out.println("Clearing target graph...");
			internal.getConnection().remove((Resource)null, null, null, internal.getValueFactory().createURI(targetGraph));
			System.out.println("Done");
			
			// copy data into new graph and apply transforms on it ?
			System.out.println("Copy original source...");
			CopyStatementRDFHandler copyHandler = new CopyStatementRDFHandler(internal);
			copyHandler.setTargetGraphs(Collections.singleton(URI.create(targetGraph)));
			internal.getConnection().export(copyHandler, internal.getValueFactory().createURI(sourceId.toString()));
			System.out.println("Done");
			
			// Execute script in target graph
			Script scriptObject = new Script(script);
			
			SesameSPARQLExecuter executer = new SesameSPARQLExecuter(internal);
			executer.setDefaultGraphs(Collections.singleton(URI.create(targetGraph)));
			executer.setDefaultRemoveGraphs(Collections.singleton(URI.create(targetGraph)));
			executer.setDefaultInsertGraph(URI.create(targetGraph));
			System.out.println("Executing");
			scriptObject.execute(executer,null);
			System.out.println("Done");
			
			// Register new transformed RDF source.
			TransformedRdfSource in = (TransformedRdfSource)p.getSource(sourceId);

			System.out.println("Registering source");
			addResultSource(p, in, destTitle, URI.create(targetGraph));
			System.out.println("Done");
		}
		catch (Exception e) {
			e.printStackTrace();
			try {
				internal.getConnection().clear(internal.getValueFactory().createURI(targetGraph.toString()));
			}
			catch (Exception e1) { e1.printStackTrace(); }

			throw new RDFTransformException(e);
		}
		System.out.println("returning");
		return serializeJSON(new JSONExecutionResult(), callback, null);
	}
	
	/**
	 * Notifies the user of successful source creation, redirecting
	 * HTML clients (i.e. browsers) to the source tab of the project
	 * main page.
	 * @param  src   the source the creation of which shall
	 *               be reported.
	 *
	 * @return an HTTP response redirecting to the project main page.
	 * @throws TechnicalException if any error occurred.
	 */
	protected final ResponseBuilder created(Source src) {
		try {
			String targetUrl = src.getProject().getUri() + "#source";
			return Response.created(new URI(src.getUri()))
					.entity(new Viewable("/" + this.getName() + "/redirect.vm", targetUrl))
					.type(TEXT_HTML);
		}
		catch (Exception e) {
			throw new RDFTransformException(e);
		}
	}
	
    /**
     * Creates a new transformed RDF source and attach it to the
     * specified project.
     * @param  p        the owning project.
     * @param  parent   the parent source object.
     * @param  name     the new source name.
     * @param  uri      the new source URI.
     *
     * @return the newly created transformed RDF source.
     * @throws IOException if any error occurred creating the source.
     */
    protected TransformedRdfSource addResultSource(Project p, Source parent,
                                                   String name, URI uri)
                                                            throws IOException {
        TransformedRdfSource newSrc =
                        this.projectManager.newTransformedRdfSource(p, uri,
                                                    name, null, uri, parent);
        this.projectManager.saveProject(p);
        return newSrc;
    }

}
