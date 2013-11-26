/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
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

package org.datalift.owl.mapper;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;
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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

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
import org.datalift.sparql.query.ConstructQuery;
import org.datalift.sparql.query.UpdateQuery;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.project.Source.SourceType.TransformedRdfSource;
import static org.datalift.fwk.util.StringUtils.*;


@Path(OntologyMapper.MODULE_NAME)
public class OntologyMapper extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "ontology-mapper";
    /** The module position in the project view. */
    private final static int MODULE_POSITION = 1800;
    /** Base name of the resource bundle for converter GUI. */
    private final static String GUI_RESOURCES_BUNDLE = "resources";
    /** The regex to split string concatenation expressions. */
    private final static Pattern CONCAT_PATTERN = Pattern.compile("\\+");
 
    /** One month (30 days actually), in seconds. */
    private final static long MONTH_IN_SECONDS = 30 * 24* 3600L;

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift project manager. */
    protected ProjectManager projectManager = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public OntologyMapper() {
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
    }

    //-------------------------------------------------------------------------
    // ProjectModule contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public UriDesc canHandle(Project p) {
        UriDesc projectPage = null;
        try {
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
            // Build canonical file path.
            URL u = new URL(src);
            String path = u.getPath();
            if (u.getProtocol().equals("file")) {
                // Local file
                // Make path relative by removing leading slash, if any.
                if (path.charAt(0) == '/') {
                    path = path.substring(1);
                }
                // Strip Windows disk name, if any.
                path = path.replaceFirst("^[a-zA-Z]:(/|\\\\)", "");
            }
            else {
                // Remote file
                // Include host name in local cache file path.
                path = u.getHost() + File.separatorChar + u.getPath();
            }
            File f = new File(Configuration.getDefault().getTempStorage(),
                              MODULE_NAME + File.separatorChar + path);
            // Make sure parent directories exist.
            f.getParentFile().mkdirs();
            // Check if ontology data have already been downloaded.
            if (! f.exists()) {
                // Look for prev. downloaded file (with RDF extension added).
                final String prefix = f.getName() + '.';
                File[] l = f.getParentFile().listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(prefix);
                    }
                });
                if ((l != null) && (l.length > 0)) {
                    f = l[0];   // Assume first found file is the expected one!
                }
            }
            long now = System.currentTimeMillis();
            if ((! f.exists()) || (f.lastModified() < now)) {
                // File not in cache or expired. => Download from server.
                try {
                    f = this.download(u, f);
                    log.debug("Downloaded \"{}\" to local cache \"{}\"...", u, f);
                }
                catch (IOException e) {
                    if (! f.exists()) {
                        throw e;
                    }
                    // Else: Continue with local (outdated) copy.
                }
            }
            else {
                // Local cache copy is still valid. => Use it.
                log.debug("Reading ontology data from local cache: {}", f);
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

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getExecuteMapping(
                                @FormParam("project") java.net.URI project,
                                @FormParam("sourceGraph") String srcGraph,
                                @FormParam("targetName") String targetName,
                                @FormParam("targetGraph") String tgtGraph,
                                @FormParam("ontology") String ontologyJson,
                                @FormParam("mapping") String mappingJson)
                                                throws WebApplicationException {
        URI sourceGraph = null;
        try {
            sourceGraph = new URIImpl(srcGraph);
        }
        catch (Exception e) {
            this.throwInvalidParamError("sourceGraph", srcGraph);
        }
        URI targetGraph = sourceGraph;
        boolean createSource = false;
        if (! isBlank(tgtGraph)) {
            if (isBlank(targetName)) {
                this.throwInvalidParamError("targetName", null);
            }
            try {
                targetGraph = new URIImpl(tgtGraph);
                createSource = true;
            }
            catch (Exception e) {
                this.throwInvalidParamError("targetGraph", srcGraph);
            }
        }
        Response response = null;
        try {
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
            log.fatal("Mapping processing failed: {}", e, e.getMessage());
            this.handleInternalError(e);
        }
        return response;
    }

    @POST
    @Path("preview")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    public String getSparqlPreview(
                                @FormParam("sourceGraph") String srcGraph,
                                @FormParam("ontology") String ontologyJson,
                                @FormParam("mapping") String mappingJson)
                                                throws WebApplicationException {
        URI sourceGraph = null;
        try {
            sourceGraph = new URIImpl(srcGraph);
        }
        catch (Exception e) {
            this.throwInvalidParamError("sourceGraph", srcGraph);
        }
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

    /**
     * Downloads the data from the specified URL and store them into
     * the specified file, possibly renaming it to append an extension
     * reflecting the MIME type of the data (as advertised in the HTTP
     * response).
     * @param  u   the URL to download the data from.
     * @param  f   the target file.
     * 
     * @return the actual file in which the data have been stored.
     * @throws IOException if any error occurred retrieving or saving
     *         the data.
     */
    private File download(URL u, File f) throws IOException {
        long now = System.currentTimeMillis();
        // Compute HTTP Accept header.
        Map<String,String> headers = new HashMap<String,String>();
        headers.put(ACCEPT, this.getRdfAcceptHeader());
        if (f.exists() && (f.lastModified() < now)) {
            headers.put(IF_MODIFIED_SINCE,
                        HttpDateFormat.formatDate(f.lastModified()));
        }
        // Retrieve file from source URL.
        FileUtils.DownloadInfo info = FileUtils.save(u, null, headers, f);
        // Cache data as long as allowed, 1 month otherwise.
        f.setLastModified((info.expires > now)? info.expires:
                                            now + (MONTH_IN_SECONDS * 1000L));
        if (info.httpStatus == 0) {
            // New file has been downloaded.
            // => Retrieve data MIME type.
            MediaType mimeType = info.mimeType;
            if ((mimeType == null) ||
                (APPLICATION_OCTET_STREAM_TYPE.equals(mimeType))) {
                // No server provided MIME type.
                // => Try to guess MIME type from file name and content.
                mimeType = MediaType.valueOf(
                                new MimetypesFileTypeMap().getContentType(f));
            }
            if ((mimeType == null) ||
                (APPLICATION_OCTET_STREAM_TYPE.equals(mimeType))) {
                // Not recognized. => Try to match well-known RDF file suffixes.
                mimeType = RdfUtils.guessRdfTypeFromExtension(f.getName());
            }
            // Extract RDF format from MIME type to set file suffix.
            RdfFormat fmt = null;
            if (mimeType != null) {
                fmt = RdfFormat.find(mimeType);
            }
            if (fmt == null) {
                throw new TechnicalException("invalid.remote.mime.type",
                                             info.mimeType);
            }
            // Ensure file extension is present to allow RDF syntax
            // detection in future cache accesses.
            String ext = "." + fmt.getFileExtension();
            if (! f.getName().endsWith(ext)) {
                File newFile = new File(f.getCanonicalPath() + ext);
                if (f.renameTo(newFile)) {
                    f = newFile;
                }
            }
            // Mark file as to be deleted upon JVM termination.
            f.deleteOnExit();
        }
        // Else: Not modified...

        return f;
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
