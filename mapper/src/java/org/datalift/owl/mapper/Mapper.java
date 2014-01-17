package org.datalift.owl.mapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.HttpHeaders.*;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlQueries;
import org.datalift.fwk.util.io.FileUtils;
import org.datalift.fwk.util.web.Charsets;
import org.datalift.fwk.util.web.HttpDateFormat;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;
import org.datalift.owl.DatatypeProperty;
import org.datalift.owl.ObjectProperty;
import org.datalift.owl.Ontology;
import org.datalift.owl.OwlClass;
import org.datalift.owl.OwlObject;
import org.datalift.owl.OwlParser;
import org.datalift.owl.OwlProperty;
import org.datalift.owl.TechnicalException;
import org.datalift.owl.toolkit.Argument;
import org.datalift.owl.toolkit.CopyStatementRDFHandler;
import org.datalift.owl.toolkit.Script;
import org.datalift.owl.toolkit.ScriptItem;
import org.datalift.owl.toolkit.SesameSPARQLExecuter;
import org.datalift.owl.toolkit.Template;
import org.datalift.owl.toolkit.TemplateRegistry;
import org.datalift.sparql.query.ConstructQuery;
import org.datalift.sparql.query.UpdateQuery;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.project.Source.SourceType.TransformedRdfSource;
import static org.datalift.fwk.util.StringUtils.*;


@Path(Mapper.MODULE_NAME)
public class Mapper extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "mapper";
    /** The module position in the project view. */
    private final static int MODULE_POSITION = 1500;
    /** Base name of the resource bundle for converter GUI. */
    private final static String GUI_RESOURCES_BUNDLE = "resources";
    /** The regex to split string concatenation expressions. */
    private final static Pattern CONCAT_PATTERN = Pattern.compile("\\+");

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift project manager. */
    protected ProjectManager projectManager = null;
    
    /** The SPARQL queries used by this module */
	private SparqlQueries queries;
	
    protected LabelFetcher labelFetcher;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public Mapper() {
        super(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);

        this.projectManager = configuration.getBean(ProjectManager.class);
        if (this.projectManager == null) {
            throw new TechnicalException("project.manager.not.available");
        }
        
		// create label fetcher and keep it in session
		Repository internal = Configuration.getDefault().getInternalRepository();
		org.openrdf.repository.Repository repository = internal.getNativeRepository();
		this.labelFetcher = new LabelFetcher(this.queries, repository);
		
		// register templates
		// TemplateRegistry.registerPackage("org.datalift.owl.mapper");
//		Template moveTypeTemplate = new Template();
//		moveTypeTemplate.setName("MOVE_CLASS");
//		moveTypeTemplate.setDisplayName("Translate class to another");
//		moveTypeTemplate.setBody("DELETE { ?s a ?source } INSERT { ?s a ?target } WHERE { ?s a ?source }");
//		
//		Argument sourceParam = new Argument();
//		sourceParam.setVarName("source");
//		sourceParam.setDisplayName("Source type");
//		sourceParam.setMandatory(true);
//		sourceParam.setOrder(0);
//		moveTypeTemplate.addArgument(sourceParam);
//		
//		Argument targetParam = new Argument();
//		targetParam.setVarName("target");
//		targetParam.setDisplayName("Target type");
//		targetParam.setMandatory(true);
//		targetParam.setOrder(1);
//		moveTypeTemplate.addArgument(targetParam);
//		
//		TemplateRegistry.register(moveTypeTemplate);
		
		Template addTypeByPredicateTemplate = new Template();
		addTypeByPredicateTemplate.setName("ADD_CLASS_BY_PRED");
		addTypeByPredicateTemplate.setDisplayName("Add class based on another, selection based on predicate");
		addTypeByPredicateTemplate.setBody("INSERT { ?s a ?target } WHERE { ?s ?predicate ?o. ?s a ?source. }");
		
		Argument predicateParam = new Argument();
		predicateParam.setVarName("predicate");
		predicateParam.setDisplayName("Source predicate");
		predicateParam.setMandatory(true);
		predicateParam.setOrder(0);
		addTypeByPredicateTemplate.addArgument(predicateParam);
		
		Argument sourceParam1 = new Argument();
		sourceParam1.setVarName("source");
		sourceParam1.setDisplayName("Source type");
		sourceParam1.setMandatory(true);
		sourceParam1.setOrder(1);
		addTypeByPredicateTemplate.addArgument(sourceParam1);
		
		Argument targetParam1 = new Argument();
		targetParam1.setVarName("target");
		targetParam1.setDisplayName("Target type");
		targetParam1.setMandatory(true);
		targetParam1.setOrder(2);
		addTypeByPredicateTemplate.addArgument(targetParam1);
		
		Template addTypeTemplate = new Template();
		addTypeTemplate.setName("ADD_CLASS");
		addTypeTemplate.setDisplayName("Add class based on another");
		addTypeTemplate.setBody("INSERT { ?s a ?target } WHERE { ?s a ?source }");
		
		Argument sourceParam = new Argument();
		sourceParam.setVarName("source");
		sourceParam.setDisplayName("Source type");
		sourceParam.setMandatory(true);
		sourceParam.setOrder(0);
		addTypeTemplate.addArgument(sourceParam);
		
		Argument targetParam = new Argument();
		targetParam.setVarName("target");
		targetParam.setDisplayName("Target type");
		targetParam.setMandatory(true);
		targetParam.setOrder(1);
		addTypeTemplate.addArgument(targetParam);
		
		TemplateRegistry.register(addTypeTemplate);
		
		Template deleteTypeTemplate = new Template();
		deleteTypeTemplate.setName("DELETE_CLASS");
		deleteTypeTemplate.setDisplayName("Delete a class");
		deleteTypeTemplate.setBody("DELETE { ?s a ?source } WHERE { ?s a ?source }");
		
		Argument sourceDeleteParam = new Argument();
		sourceDeleteParam.setVarName("source");
		sourceDeleteParam.setDisplayName("Source type");
		sourceDeleteParam.setMandatory(true);
		sourceDeleteParam.setOrder(0);
		deleteTypeTemplate.addArgument(sourceParam);
		
		TemplateRegistry.register(deleteTypeTemplate);
		
		Template movePredicateTemplate = new Template();
		movePredicateTemplate.setName("MOVE_PREDICATE");
		movePredicateTemplate.setDisplayName("Translate predicate to another");
		movePredicateTemplate.setBody("DELETE { ?s ?source ?o } INSERT { ?s ?target ?o } WHERE { ?s ?source ?o }");
		
		Argument sourcePredicateParam = new Argument();
		sourcePredicateParam.setVarName("source");
		sourcePredicateParam.setDisplayName("Source type");
		sourcePredicateParam.setMandatory(true);
		sourcePredicateParam.setOrder(0);
		movePredicateTemplate.addArgument(sourcePredicateParam);
		
		Argument targetPredicateParam = new Argument();
		targetPredicateParam.setVarName("target");
		targetPredicateParam.setDisplayName("Target type");
		targetPredicateParam.setMandatory(true);
		targetPredicateParam.setOrder(1);
		movePredicateTemplate.addArgument(targetPredicateParam);
		
		TemplateRegistry.register(movePredicateTemplate);
    }

    //-------------------------------------------------------------------------
    // ProjectModule contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public UriDesc canHandle(Project p) {
        UriDesc projectPage = null;
        try {//TODO retirer la contrainte du projet sur les ontologies non présentent
            if ((! p.getOntologies().isEmpty()) &&
                (this.findSource(p, false) != null)) {
                try {
                    String label = PreferredLocales.get()
                                .getBundle(GUI_RESOURCES_BUNDLE, this)
                                .getString("ontology.mapper.button");

                    projectPage = new UriDesc(
                                    this.getName() + "?project=" + p.getUri(),
                                    HttpMethod.GET, label);
                    projectPage.setPosition(MODULE_POSITION);
                }
                catch (Exception e) {
                    throw new TechnicalException(e);
                }
            }
        }
        catch (Exception e) {
            log.fatal("Failed to check status of project {}: {}", e,
                                                p.getUri(), e.getMessage());
        }
        return projectPage;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam("project") java.net.URI projectId) {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Display conversion configuration page.
            TemplateModel view = this.newView("mapper.vm", p);
            view.put("srcType", TransformedRdfSource);
            response = Response.ok(view, TEXT_HTML_UTF8).build();
        }
        catch (IllegalArgumentException e) {
            TechnicalException error =
                            new TechnicalException("ws.invalid.param.error",
                                                   "project", projectId);
            this.sendError(BAD_REQUEST, error.getLocalizedMessage());
        }
        return response;
    }

    @GET
    @Path("ontology")
    @Produces(APPLICATION_JSON)
    public Response parseOntology(@QueryParam("src") String src) {
        if (! isSet(src)) {
            this.throwInvalidParamError("src", null);
        }
        Response response = null;
        try {
            URL u = new URL(src);
            String path = u.getPath();
            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }
            File f = new File(Configuration.getDefault().getTempStorage(),
                              MODULE_NAME + '/' + path);
            long now = System.currentTimeMillis();
            if ((! f.exists()) || (f.lastModified() < now)) {
                // Compute HTTP Accept header.
                Map<String,String> headers = new HashMap<String,String>();
                headers.put(ACCEPT, this.getRdfAcceptHeader());
                if (f.exists()) {
                    headers.put(IF_MODIFIED_SINCE,
                                HttpDateFormat.formatDate(f.lastModified()));
                }
                // Make sure parent directories exist.
                f.getParentFile().mkdirs();
                // Retrieve file from source URL.
                log.trace("--- info url ---: {}, file: {}, headers: {}", u, f, headers);
                
                FileUtils.DownloadInfo info = FileUtils.save(u, null, headers, f);
                log.trace("--- info ---: {}", info);
                f.setLastModified((info.expires > 0L)? info.expires: now);
                // Extract RDF format from MIME type to force file suffix.
                RdfFormat fmt = RdfFormat.find(info.mimeType);
                if (fmt == null) {
                	// Got it from lov (but application/octet-stream)
                	if (src.endsWith(".n3")) {
                		fmt = RdfFormat.N3;
                	}
                	else if (src.endsWith(".rdf")) {
                		fmt = RdfFormat.RDF_XML;
                	}
                	else if (src.endsWith(".ttl")) {
                		fmt = RdfFormat.TURTLE;
                	}
                	else {
                		throw new TechnicalException("invalid.remote.mime.type",
                				info.mimeType);
                	}
                }
                // Ensure file extension is present to allow RDF syntax
                // detection in future cache accesses.
                String ext = "." + fmt.getFileExtension();
                if (! f.getName().endsWith(ext)) {
                    File newFile = new File(f.getCanonicalPath() + ext);
                    f.renameTo(newFile);
                    f = newFile;
                }
                // Mark file as to be deleted upon JVM termination.
                f.deleteOnExit();
            }
            // Parse ontology.
            Ontology o = new OwlParser().parse(f, src);
            // Return JSON representation of OWL ontology.
            response = Response.ok(new OntologyJsonStreamingOutput(o),
                                   APPLICATION_JSON_UTF8).build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

//    @GET
//    @Path("sparqlQuery")
//    @Produces(APPLICATION_JSON)
//    public Response getSparqlQuery(@QueryParam("script") String script) {
//    	Script scriptObject = new Script(script);
//    	StringBuilder sb = new StringBuilder();
//    	for(ScriptItem s : scriptObject.getScriptItems()) {
//    		sb.append(s.toString());
//    	}
//    	return Response.ok(sb.toString(), APPLICATION_JSON_UTF8).build();
//    }
    
	@POST
	@Path("execute")
	public Response executeScript(
			@FormParam("project") java.net.URI projectId,
			@FormParam("source") java.net.URI sourceId,
			@FormParam("dest_title") String destTitle,
			@FormParam("dest_graph_uri") String targetGraph,
			@FormParam("script") String script)
					throws WebApplicationException {

		log.debug("RDF-Transform executing script '"+script+"'" +
						" on project '"+projectId+"'," +
								" source '"+sourceId+"'," +
										" to target '"+targetGraph+"'" +
												" with title '"+destTitle+"'");
		
		org.openrdf.repository.Repository internal = Configuration.getDefault().getInternalRepository().getNativeRepository();
		try {
			if ((script == null) || (script.equals(""))) {
				log.error("Missing script parameter.");
				throw new WebApplicationException(
						Response.status(Status.BAD_REQUEST)
						.type(MediaTypes.TEXT_PLAIN_TYPE)
						.entity("Missing script parameter.").build()
						);
			}
			// Retrieve project.
			Project p = this.getProject(projectId);

			// clear target graph
			log.trace("Clearing target graph...");
			internal.getConnection().remove((Resource)null, null, null, internal.getValueFactory().createURI(targetGraph));
			
			// copy data into new graph and apply transforms on it ?
			log.trace("Copy original source...");
			CopyStatementRDFHandler copyHandler = new CopyStatementRDFHandler(internal);
			copyHandler.setTargetGraphs(Collections.singleton(java.net.URI.create(targetGraph)));
			internal.getConnection().export(copyHandler, internal.getValueFactory().createURI(sourceId.toString()));
			
			// Execute script in target graph
			Script scriptObject = new Script(script);

			SesameSPARQLExecuter executer = new SesameSPARQLExecuter(internal);
			executer.setDefaultGraphs(Collections.singleton(java.net.URI.create(targetGraph)));
			executer.setDefaultRemoveGraphs(Collections.singleton(java.net.URI.create(targetGraph)));
			executer.setDefaultInsertGraph(java.net.URI.create(targetGraph));
			log.trace("Executing script...");
			scriptObject.execute(executer,null);
			
			// Register new transformed RDF source.
			TransformedRdfSource in = (TransformedRdfSource)p.getSource(sourceId);

			log.trace("Registering source");
			addResultSource(p, in, destTitle, java.net.URI.create(targetGraph));
		}
		catch (Exception e) {
			e.printStackTrace();
			try {
				internal.getConnection().clear(internal.getValueFactory().createURI(targetGraph.toString()));
			}
			catch (Exception e1) { e1.printStackTrace(); }

			return Response.status(INTERNAL_SERVER_ERROR).build();
		}
		return Response.ok().build();
	}
	
    // Single ontology
    
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getExecuteMapping(
                                @FormParam("project") java.net.URI project,
                                @FormParam("sourceGraph") URIImpl sourceGraph,
                                @FormParam("targetName") String targetName,
                                @FormParam("targetGraph") URIImpl targetGraph,
                                @FormParam("ontology") String ontologyJson,
                                @FormParam("mapping") String mappingJson)
                                                throws WebApplicationException {
        Response response = null;
        try {
            boolean createSource = true;
            if (targetGraph == null) {
                createSource = false;
                targetGraph = sourceGraph;
            }
            Gson gson = new Gson();
            OntologyDesc o = gson.fromJson(ontologyJson, OntologyDesc.class);
            MappingDesc  m = gson.fromJson(mappingJson, MappingDesc.class);
            UpdateQuery query = new ConstructQuery();
            URI ontologyNs = query.uri(m.types.get(0));
            query.prefix(urlify(o.name), ontologyNs.getNamespace());
            Resource node = query.variable(m.name);
            this.mapNode(query, m, node, node, sourceGraph);

            // Retrieve project and input source (to check they exist!).
            String srcId = sourceGraph.toString();
            Project p = null;
            Source in = null;
            try {
                p = this.getProject(project);
                in = (TransformedRdfSource)(p.getSource(srcId));
            }
            catch (Exception e) {
                this.throwInvalidParamError("sourceGraph", srcId);
            }
            // Execute SPARQL Construct queries.
            java.net.URI ctx = java.net.URI.create(targetGraph.toString());
            Repository internal = Configuration.getDefault()
                                               .getInternalRepository();
            String construct = query.toString();
            log.debug("Applying mapping from {} to {}, query:\n{}",
                                                sourceGraph, ctx, construct);
            RdfUtils.convert(internal, Arrays.asList(construct),
                             internal, ctx, createSource);
            Source out = in;
            if (createSource) {
                // Register new transformed RDF source.
                out = this.addResultSource(p, in, targetName, targetGraph);
            }
            // Display project source tab, including the newly created source.
            response = this.displayMappingResult(out, createSource).build();
        }
        catch (Exception e) {
            // ???
        }
        return response;
    }

    @POST
    @Path("preview")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    public String getSparqlPreview(
                                @FormParam("sourceGraph") URIImpl sourceGraph,
                                @FormParam("ontology") String ontologyJson,
                                @FormParam("mapping") String mappingJson)
                                                throws WebApplicationException {
        String preview = "";
        try {
            Gson gson = new Gson();
            OntologyDesc o = gson.fromJson(ontologyJson, OntologyDesc.class);
            MappingDesc  m = gson.fromJson(mappingJson, MappingDesc.class);
            UpdateQuery query = new ConstructQuery();
            URI ontologyNs = query.uri(m.types.get(0));
            query.prefix(urlify(o.name), ontologyNs.getNamespace());
            Resource node = query.variable(m.name);
            preview = this.mapNode(query, m, node, node, sourceGraph)
                          .toString();
        }
        catch (Exception e) {
            log.fatal(e.getMessage(), e);
            // And return empty preview.
        }
        return preview;
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
     * Retrieves a {@link Project} using its URI.
     * @param  projectId   the project URI.
     *
     * @return the project.
     * @throws TechnicalException if the project does not exist.
     */
    private final Project getProject(java.net.URI projectId) {
        Project p = this.projectManager.findProject(projectId);
        if (p == null) {
            throw new IllegalArgumentException(projectId.toString());
        }
        return p;
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
    private Source findSource(Project p, boolean findLast) {
        if (p == null) {
            throw new IllegalArgumentException("p");
        }
        Source src = null;
        for (Source s : p.getSources()) {
            if (s.getType() == TransformedRdfSource) {
                src = s;
                if (! findLast) break;
                // Else: continue to get last source of type in project...
            }
        }
        return src;
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
    private TransformedRdfSource addResultSource(Project p, Source parent,
                                                   String name, URI uri)
                                                            throws IOException {
        java.net.URI id = java.net.URI.create(uri.toString());
        TransformedRdfSource newSrc =
                        this.projectManager.newTransformedRdfSource(p, id,
                                                    name, null, id, parent);
        this.projectManager.saveProject(p);
        return newSrc;
    }
    
    private TransformedRdfSource addResultSource(Project p, Source parent,
									            String name, java.net.URI uri)
									                     throws IOException {
		java.net.URI id = java.net.URI.create(uri.toString());
		TransformedRdfSource newSrc =
		this.projectManager.newTransformedRdfSource(p, id,
		             name, null, id, parent);
		this.projectManager.saveProject(p);
		return newSrc;
	}

    private String getRdfAcceptHeader() {
        StringBuilder buf = new StringBuilder();
        for (RdfFormat fmt : RdfFormat.values()) {
            boolean first = true;
            for (MediaType m : fmt.mimeTypes) {
                if (first) {
                    // Preferred MIME type (q=1.0).
                    if (buf.length() != 0) {
                        buf.append(", ");
                    }
                    buf.append(m);
                    first = false;
                }
                else {
                    // Secondary MIME types.
                    buf.append(", ").append(m).append("; q=0.5");
                }
            }
        }
        return buf.toString();
    }

    private UpdateQuery mapNode(UpdateQuery query, MappingDesc m,
                                Resource from, Resource node, URI srcGraph) {
        Map<URI,String> mapping = new HashMap<URI,String>();
        if (m.types.isEmpty()) {
            // Simple property.
            URI u = query.uri(m.predicate);
            query.prefixFor(u.getNamespace());
            mapping.put(u, this.mapFunctions(m.value, query));
        }
        else {
            // RDF node.
            if (isSet(m.predicate)) {
                Resource o = query.blankNode();
                URI u = query.uri(m.predicate);
                query.prefixFor(u.getNamespace());
                query.triple(node, u, o);
                node = o;
            }
            // Else: No predicate linking node to parent? Assume root node.

            for (String t : m.types) {
                // Add RDF types.
                query.rdfType(node, query.uri(t));
            }
            if (isSet(m.value)) {
                // Add value mapping.
                mapping.put(RDF.VALUE, this.mapFunctions(m.value, query));
            }
        }
        if (! mapping.isEmpty()) {
            // Mappings present (triples & where clauses) => Insert in query.
            query.map(srcGraph, from, node, mapping);
        }
        // Process child mappings.
        for (MappingDesc child : m.children) {
            this.mapNode(query, child, from, node, srcGraph);
        }
        return query;
    }

    private String mapFunctions(String expr, UpdateQuery query) {
        if (isSet(expr)) {
            if (CONCAT_PATTERN.matcher(expr).find()) {
                boolean inQuotes = false;
                StringBuilder buf = new StringBuilder(expr.length() + 8);
                buf.append("CONCAT(");
                for (String s : CONCAT_PATTERN.split(expr.trim())) {
                    for (int i=0,max=s.length(); i<max; i++) {
                        if (s.charAt(i) == '"') inQuotes = (! inQuotes);
                    }
                    if (inQuotes) {
                        buf.append(s).append('+');
                    }
                    else {
                        buf.append(s.trim()).append(',');
                    }
                }
                buf.setLength(buf.length() - 1);
                expr = buf.append(')').toString();
            }
        }
        return expr;
    }

    /**
     * Return a model for the specified template view, populated with
     * the specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated template model.
     */
    private TemplateModel newView(String templateName, Object it) {
        return ViewFactory.newView(
                                "/" + this.getName() + '/' + templateName, it);
    }

    /**
     * Notifies the user of successful processing, redirecting
     * HTML clients (i.e. browsers) to the display of the content
     * of the created or updated source.
     * @param  src       the source the creation or update of which
     *                   shall be reported.
     * @param  created   whether the source was created.
     *
     * @return an partially-built response object redirecting the client
     *         browser to the project source page. The HTTP status code
     *         depends on whether the source was created.
     * @throws TechnicalException if any error occurred.
     */
    private ResponseBuilder displayMappingResult(Source src, boolean created) {
        ResponseBuilder response = null;
        String targetUrl = src.getProject().getUri() + "#source";
        if (created) {
            response = Response.created(java.net.URI.create(src.getUri()))
                               .entity(this.newView("redirect.vm", targetUrl))
                               .type(TEXT_HTML_UTF8);
        }
        else {
            response = Response.seeOther(java.net.URI.create(targetUrl));
        }
        return response;
    }

    /**
     * Throws a {@link WebApplicationException} with a HTTP status set
     * to 400 (Bad request) to signal an invalid or missing web service
     * parameter.
     * @param  name    the parameter name in the web service interface.
     * @param  value   the invalid parameter value or <code>null</code>
     *                 if the parameter was absent.
     *
     * @throws WebApplicationException always.
     */
    private void throwInvalidParamError(String name, Object value)
                                                throws WebApplicationException {
        TechnicalException error = (value != null)?
                new TechnicalException("ws.invalid.param.error", name, value):
                new TechnicalException("ws.missing.param", name);
        this.sendError(BAD_REQUEST, error.getLocalizedMessage());
    }

    /**
     * Logs and map an internal processing error onto HTTP status codes.
     * @param  e   the error to map.
     *
     * @throws WebApplicationException always.
     */
    private void handleInternalError(Exception e)
                                                throws WebApplicationException {
        TechnicalException error = null;
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        else if (e instanceof FileNotFoundException) {
            this.sendError(NOT_FOUND, e.getLocalizedMessage());
        }
        else if (e instanceof TechnicalException) {
            error = (TechnicalException)e;
        }
        else {
            error = new TechnicalException(
                            "ws.internal.error", e, e.getLocalizedMessage());
        }
        log.fatal(e.getMessage(), e);
        this.sendError(INTERNAL_SERVER_ERROR, error.getLocalizedMessage());
    }

    //-------------------------------------------------------------------------
    // Java representations of JSON request data
    //-------------------------------------------------------------------------

    public final static class OntologyDesc
    {
        public String name;
        public String uri;
    }
    
    public final static class MappingDesc
    {
        public String name;
        public String predicate;
        public String value;
        public List<String> types = new LinkedList<String>();
        public Collection<MappingDesc> children = new LinkedList<MappingDesc>();
    }

    //-------------------------------------------------------------------------
    // JSON result serialization handling nested classes
    //-------------------------------------------------------------------------

    /**
     * A {@link StreamingOutput} implementation that serializes an
     * {@link Ontology} object into JSON.
     */
    private final static class OntologyJsonStreamingOutput
                                                    implements StreamingOutput
    {
        private final Ontology ontology;

        public OntologyJsonStreamingOutput(Ontology ontology) {
            this.ontology = ontology;
        }

        /** {@inheritDoc} */
        @Override
        public void write(OutputStream output)
                                throws IOException, WebApplicationException {
            OwlSerializer owlSerializer = new OwlSerializer();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(OwlClass.class, owlSerializer)
                    .registerTypeAdapter(OwlProperty.class, owlSerializer)
                    .registerTypeAdapter(ObjectProperty.class, owlSerializer)
                    .registerTypeAdapter(DatatypeProperty.class, owlSerializer)
                    .registerTypeAdapter(java.net.URI.class, new UriSerializer())
                    .setPrettyPrinting().create();
            Writer w = new OutputStreamWriter(output, Charsets.UTF_8);
            gson.toJson(this.ontology, w);
            w.flush();
        }
    }

    /**
     * A {@link JsonSerializer} to serialize {@link OwlObject}s.
     */
    private final static class OwlSerializer implements JsonSerializer<OwlObject>
    {
        private boolean inOwl = false;

        public OwlSerializer() {
            super();
        }

        public JsonElement serialize(OwlObject src, Type type,
                                     JsonSerializationContext context) {
            JsonElement e = null;
            if (inOwl) {
                e = new JsonPrimitive(src.uri());
            }
            else {
                inOwl = true;
                if (src instanceof OwlClass) {
                    e = this.serialize((OwlClass)src);
                }
                else if (src instanceof OwlProperty) {
                    e = this.serialize((OwlProperty)src);
                }
                else {
                    e = this.serialize(src);
                }
                inOwl = false;
            }
            return e;
        }

        private JsonObject serialize(OwlObject o) {
            JsonObject e = new JsonObject();
            // e.addProperty("uri", src.uri);
            e.addProperty("type", o.getClass().getSimpleName());
            e.addProperty("name", o.name);
            if (isSet(o.desc)) {
                e.addProperty("desc", o.desc);
            }
            return e;
        }

        private JsonObject serialize(OwlClass c) {
            JsonObject e = this.serialize((OwlObject)c);
            this.add(c.parents(), "parents", e);
            this.add(c.subclasses(), "subclasses", e);
            this.add(c.disjoints(), "disjoints", e);
            this.add(c.properties(true), "properties", e);
            if (c.union()) {
                e.addProperty("union", Boolean.TRUE);
            }
            return e;
        }

        private JsonObject serialize(OwlProperty p) {
            JsonObject e = this.serialize((OwlObject)p);
            Collection<OwlClass> ranges = p.ranges();
            if (! ranges.isEmpty()) {
                this.add(p.ranges(),  "ranges", e);
            }
            else {
                if (p.type() != null) {
                    JsonArray x = new JsonArray();
                    x.add(new JsonPrimitive(p.type().stringValue()));
                    e.add("ranges", x);
                }
            }
            this.add(p.domains(), "domains", e);
            return e;
        }

        private void add(Collection<? extends OwlObject> c,
                                                String name, JsonObject e) {
            if ((c != null) && (! c.isEmpty())) {
                JsonArray x = new JsonArray();
                for (OwlObject o : c) {
                    x.add(new JsonPrimitive(o.uri()));
                }
                e.add(name, x);
            }
        }
    }

    /**
     * A {@link JsonSerializer} to serialize {@link URI}s.
     */
    private final static class UriSerializer implements JsonSerializer<URI>
    {
        public UriSerializer() {
            super();
        }

        public JsonElement serialize(URI src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            return new JsonPrimitive(src.stringValue());
        }
    }
}
