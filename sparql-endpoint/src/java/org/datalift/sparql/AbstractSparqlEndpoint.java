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

package org.datalift.sparql;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.Response.Status.*;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.TupleQueryResultHandlerBase;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.BaseLocalizedItem;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.BaseTupleQueryResultMapper;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.TupleQueryResultMapper;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.sparql.SparqlEndpoint.DescribeType.*;


/**
 * A skeleton for implementations of the {@link SparqlEndpoint}
 * Java interface and SPARQL endpoint HTTP bindings.
 *
 * @author lbihanic
 */
abstract public class AbstractSparqlEndpoint extends BaseModule
                                             implements SparqlEndpoint
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The SPARQL endpoint module name. */
    public final static String MODULE_NAME = "sparql";

    /**
     * The (optional) configuration property holding the path of
     * the RDF file to load predefined queries from.
     */
    public final static String QUERIES_FILE_PROPERTY =
                                            "sparql.predefined.queries.file";
    /** The default queries file, embedded in module JAR. */
    private final static String DEFAULT_QUERIES_FILE = "predefined-queries.ttl";

    private final static Pattern QUERY_START_PATTERN = Pattern.compile(
                    "SELECT|CONSTRUCT|ASK|DESCRIBE", Pattern.CASE_INSENSITIVE);
    private final static int MAX_QUERY_DESC = 128;

    private final static MessageFormat DESCRIBE_OBJECT_QUERY =
            new MessageFormat("CONSTRUCT '{' ?s1 ?p1 ?o1 ."
                              +            " ?o1 ?p2 ?o2 . '}'\n"
                              + "WHERE '{'\n"
                              + "  ?s1 ?p1 ?o1 .\n"
                              + "  OPTIONAL '{'\n"
                              + "    ?o1 ?p2 ?o2 .\n"
                              + "    FILTER isBlank(?o1)\n  '}'\n"
                              + "  FILTER ( ?s1 = <{0}> || ?o1 = <{0}> )\n'}'");
    private final static MessageFormat DESCRIBE_PREDICATE_QUERY =
            new MessageFormat("CONSTRUCT '{' ?s ?p ?o . '}' WHERE '{'\n"
                              + "  ?s ?p ?o .\n"
                              + "  FILTER ( ?p = <{0}> )\n'}'");
    private final static MessageFormat DESCRIBE_GRAPH_QUERY =
            new MessageFormat("CONSTRUCT '{' ?s ?p ?o . '}' WHERE '{'\n"
                              + "  GRAPH <{0}> '{' ?s ?p ?o . '}'\n'}'");

    private final static String DETERMINE_TYPE_QUERY =
            "SELECT DISTINCT ?s ?p ?g WHERE {\n" +
            "  OPTIONAL { ?s ?p1 ?o1 . FILTER( ?s = ?u ) }\n" +
            "  OPTIONAL { ?s1 ?p ?o2 . FILTER( ?p = ?u ) }\n" +
            "  OPTIONAL { GRAPH ?g { ?s2 ?p2 ?o3 . FILTER( ?g = ?u ) } }\n" +
            "} LIMIT 1";

    /** The SPARQL query to extract predefined query data. */
    private final static String LOAD_PREDEFINED_QUERIES_QUERY =
                    "PREFIX rdf: <" + RDF.NAMESPACE + ">\n" +
                    "PREFIX rdfs: <" + RDFS.NAMESPACE + ">\n" +
                    "PREFIX datalift: <" + RdfNamespace.DataLift.uri + ">\n" +
                    "SELECT ?s ?label ?query WHERE { " +
                        "?s a datalift:SparqlQuery ; " +
                        "   rdfs:label ?label ; rdf:value ?query . " +
                    "} ORDER BY ?s";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    protected final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The predefined SPARQL queries. */
    private List<PredefinedQuery> predefinedQueries =
                                            new LinkedList<PredefinedQuery>();
    /** The welcome page. */
    private final String welcomeTemplate;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    protected AbstractSparqlEndpoint() {
        this(null);
    }

    /**
     * Creates a new SPARQL endpoint resource.
     * @param  welcomeTemplate   the Velocity template to display as
     *                           welcome page.
     */
    protected AbstractSparqlEndpoint(String welcomeTemplate) {
        this(MODULE_NAME, welcomeTemplate);
    }

    /**
     * Creates a new SPARQL endpoint resource.
     * @param  name              the module name.
     * @param  welcomeTemplate   the Velocity template to display as
     *                           welcome page.
     */
    protected AbstractSparqlEndpoint(String name, String welcomeTemplate) {
        super(name);
        this.welcomeTemplate = (isSet(welcomeTemplate))? welcomeTemplate:
                                       "/" + name + "/sparqlEndpoint.vm";
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        // Load predefined SPARQL queries.
        this.predefinedQueries = Collections.unmodifiableList(
                                    this.loadPredefinedQueries(configuration));
    }

    /**
     * {@inheritDoc}
     * <p>This implementation returns <code>true</code> (no
     * authentication required).</p>
     */
    @Override
    public boolean allowsAnonymousAccess() {
        return true;
    }

    //-------------------------------------------------------------------------
    // SparqlEndpoint contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(String query, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                                throws WebApplicationException {
        return this.executeQuery(null, null, query, -1, -1, false, null, null,
                                 uriInfo, request, acceptHdr);
    }
    
    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                                        List<String> namedGraphUris,
                                        String query, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                                throws WebApplicationException {
        return this.executeQuery(defaultGraphUris, namedGraphUris, query,
                        -1, -1, false, null, null, uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset, boolean gridJson,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException {
        return this.executeQuery(defaultGraphUris, namedGraphUris, query,
                                startOffset, endOffset, gridJson, null, null,
                                uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset,
                            boolean gridJson, String format, String jsonCallback,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            if ((! isBlank(jsonCallback)) && (isBlank(format))) {
                format = APPLICATION_JSON;
            }
            response = this.doExecute(defaultGraphUris, namedGraphUris, query,
                                      startOffset, endOffset, gridJson,
                                      format, jsonCallback,
                                      uriInfo, request, acceptHdr, null);
        }
        catch (Exception e) {
            this.handleError(query, e);
        }
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder describe(String uri, DescribeType type,
                                    UriInfo uriInfo, Request request,
                                    String acceptHdr)
                                                throws WebApplicationException {
        return this.describe(uri, type, null, uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder describe(String uri, DescribeType type,
                                    Repository repository, UriInfo uriInfo,
                                    Request request, String acceptHdr)
                                                throws WebApplicationException {
        return this.describe(uri, type, repository, -1, null, null,
                                                uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder describe(String uri, DescribeType type,
                                    Repository repository, int max, 
                                    String format, String jsonCallback,
                                    UriInfo uriInfo, Request request,
                                    String acceptHdr)
                                                throws WebApplicationException {
        if (isBlank(uri)) {
            this.throwInvalidParamError("uri", uri);
        }
        List<String> defGraphs = null;
        if (repository != null) {
            defGraphs = new ArrayList<String>();
            defGraphs.add(repository.name);
        }
        ResponseBuilder response = null;
        try {
            if (type == null) {
                type = this.getDescribeTypeFromUri(uri, repository);
            }
            if (type != null) {
                // URI found in RDF store.
                String query = null;
                MessageFormat fmt = (type == Object)? DESCRIBE_OBJECT_QUERY:
                                    (type == Graph)?  DESCRIBE_GRAPH_QUERY:
                                                      DESCRIBE_PREDICATE_QUERY;
                synchronized (fmt) {
                    query = fmt.format(new Object[] { uri });
                }
                Map<String,Object> viewData = new HashMap<String,Object>();
                viewData.put("describe-type", type);
                viewData.put("describe-uri",  uri);
                response = this.doExecute(defGraphs, null, query, -1, max,
                                          false, format, jsonCallback, uriInfo,
                                          request, acceptHdr, viewData);
            }
            else {
                try {
                    URL u = new URL(uri);
                    response = Response.seeOther(u.toURI());
                }
                catch (Exception e) {
                    this.sendError(NOT_FOUND, null);
                }
            }
        }
        catch (Exception e) {
            this.handleError(uri, e);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Process a SPARQL query sent as an HTTP
     * GET request.
     * @param  defaultGraphUris   the <code>default-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  namedGraphUris     the <code>named-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  query              the <code>query</code>
     *                            parameter of the SPARQL query.
     * @param  startOffset        the offset of the first expected
     *                            result.
     * @param  endOffset          the offset of the last expected
     *                            result.
     * @param  gridJson           whether to return HTML table-ready
     *                            JSON data.
     * @param  format             the expected response format,
     *                            overrides the HTTP Accept header.
     * @param  jsonCallback       the name of the JSONP callback to
     *                            wrap the JSON response.
     * @param  uriInfo            the request URI data.
     * @param  request            the JAX-RS Request object, for content
     *                            negotiation.
     * @param  acceptHdr          the HTTP Accept header, for content
     *                            negotiation.
     *
     * @return the SPARQL query result, formatted according to the
     *         negotiated content type.
     * @throws WebApplicationException if the SPARQL request is invalid
     *         or a processing error occurred or the user is not allowed
     *         to execute the specified query.
     */
    @GET
    public Response getQuery(
                @QueryParam("default-graph-uri") List<String> defaultGraphUris,
                @QueryParam("named-graph-uri") List<String> namedGraphUris,
                @QueryParam("query") String query,
                @QueryParam("min") @DefaultValue("-1") int startOffset,
                @QueryParam("max") @DefaultValue("-1") int endOffset,
                @QueryParam("grid") @DefaultValue("false") boolean gridJson,
                @QueryParam("format") String format,
                @QueryParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, gridJson, format,
                                  jsonCallback, uriInfo, request, acceptHdr);
    }
    
    /**
     * <i>[Resource method]</i> Process a SPARQL query sent as an HTTP
     * POST request.
     * @param  defaultGraphUris   the <code>default-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  namedGraphUris     the <code>named-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  query              the <code>query</code>
     *                            parameter of the SPARQL query.
     * @param  startOffset        the offset of the first expected
     *                            result.
     * @param  endOffset          the offset of the last expected
     *                            result.
     * @param  gridJson           whether to return HTML table-ready
     *                            JSON data.
     * @param  format             the expected response format,
     *                            overrides the HTTP Accept header.
     * @param  jsonCallback       the name of the JSONP callback to
     *                            wrap the JSON response.
     * @param  uriInfo            the request URI data.
     * @param  request            the JAX-RS Request object, for content
     *                            negotiation.
     * @param  acceptHdr          the HTTP Accept header, for content
     *                            negotiation.
     *
     * @return the SPARQL query result, formatted according to the
     *         negotiated content type.
     * @throws WebApplicationException if the SPARQL request is invalid
     *         or a processing error occurred or the user is not allowed
     *         to execute the specified query.
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public final Response postQuery(
                @FormParam("default-graph-uri") List<String> defaultGraphUris,
                @FormParam("named-graph-uri") List<String> namedGraphUris,
                @FormParam("query") String query,
                @FormParam("min") @DefaultValue("-1") int startOffset,
                @FormParam("max") @DefaultValue("-1") int endOffset,
                @FormParam("grid") @DefaultValue("false") boolean gridJson,
                @FormParam("format") String format,
                @FormParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, gridJson, format,
                                  jsonCallback, uriInfo, request, acceptHdr);
    }
    
    @GET
    @Path("{store}")
    public Response getStoreQuery(
                @PathParam("store") String repository,
                @QueryParam("default-graph-uri") List<String> defaultGraphUris,
                @QueryParam("named-graph-uri") List<String> namedGraphUris,
                @QueryParam("query") String query,
                @QueryParam("min") @DefaultValue("-1") int startOffset,
                @QueryParam("max") @DefaultValue("-1") int endOffset,
                @QueryParam("grid") @DefaultValue("false") boolean gridJson,
                @QueryParam("format") String format,
                @QueryParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        List<String> defGraphUris = Arrays.asList(repository);
        if (defaultGraphUris != null) {
            defGraphUris.addAll(defGraphUris);
        }
        return this.dispatchQuery(defGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, gridJson, format,
                                  jsonCallback, uriInfo, request, acceptHdr);
    }
    
    @POST
    @Path("{store}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public final Response postStoreQuery(
    			@PathParam("store") String repository,
                @FormParam("default-graph-uri") List<String> defaultGraphUris,
                @FormParam("named-graph-uri") List<String> namedGraphUris,
                @FormParam("query") String query,
                @FormParam("min") @DefaultValue("-1") int startOffset,
                @FormParam("max") @DefaultValue("-1") int endOffset,
                @FormParam("grid") @DefaultValue("false") boolean gridJson,
                @FormParam("format") String format,
                @FormParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, gridJson, format,
                                  jsonCallback, uriInfo, request, acceptHdr);
    }


    /**
     * <i>[Resource method]</i> Returns the description of the specified
     * object (node, predicate or named graph) from the specified RDF
     * store, using the HTTP GET method.
     * <p>
     * Whenever known, the type of description shall be provided to
     * avoid the overhead of querying the RDF store to try to detect
     * the possible applicable description types.</p>
     * @param  uri            the URI of the object to describe.
     * @param  type           the type of the object or
     *                        <code>null</code> if unknown.
     * @param  defaultGraph   the RDF store to read the object
     *                        description from.
     * @param  uriInfo        the request URI data.
     * @param  request        the JAX-RS Request object, for content
     *                        negotiation.
     * @param  acceptHdr      the HTTP Accept header, for content
     *                        negotiation.
     *
     * @return the SPARQL query result, formatted according to the
     *         negotiated content type.
     * @throws WebApplicationException if the SPARQL request is invalid
     *         or a processing error occurred or the user is not allowed
     *         to execute the specified query.
     */
    @GET
    @Path("describe")
    public final Response getDescribe(
                            @QueryParam("uri") String uri,
                            @QueryParam("type") String type,
                            @QueryParam("default-graph") String defaultGraph,
                            @QueryParam("max") @DefaultValue("-1") int max,
                            @Context UriInfo uriInfo,
                            @Context Request request,
                            @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        Repository repository = null;
        if (! isBlank(defaultGraph)) {
            // Resolve target repository (a mutable list is required).
            List<String> l = new LinkedList<String>();
            l.add(defaultGraph);
            repository = this.getTargetRepository(l);
        }
        return this.describe(uri, DescribeType.fromString(type),
                             repository, max, null, null,
                             uriInfo, request, acceptHdr)
                   .build();
    }

    /**
     * <i>[Resource method]</i> Returns the description of the specified
     * object (node, predicate or named graph) from the specified RDF
     * store, using the HTTP POST method.
     * <p>
     * Whenever known, the type of description shall be provided to
     * avoid the overhead of querying the RDF store to try to detect
     * the possible applicable description types.</p>
     * @param  uri            the URI of the object to describe.
     * @param  type           the type of the object or
     *                        <code>null</code> if unknown.
     * @param  defaultGraph   the RDF store to read the object
     *                        description from.
     * @param  uriInfo        the request URI data.
     * @param  request        the JAX-RS Request object, for content
     *                        negotiation.
     * @param  acceptHdr      the HTTP Accept header, for content
     *                        negotiation.
     *
     * @return the SPARQL query result, formatted according to the
     *         negotiated content type.
     * @throws WebApplicationException if the SPARQL request is invalid
     *         or a processing error occurred or the user is not allowed
     *         to execute the specified query.
     */
    @POST
    @Path("describe")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public final Response postDescribe(
                            @FormParam("uri") String uri,
                            @FormParam("type") String type,
                            @FormParam("default-graph") String defaultGraph,
                            @FormParam("max") @DefaultValue("-1") int max,
                            @Context UriInfo uriInfo,
                            @Context Request request,
                            @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.getDescribe(uri, type, defaultGraph, max,
                                uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private final Response dispatchQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset, 
                            boolean gridJson, String format, String jsonCallback,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;

        // Check for empty query.
        if (StringUtils.isSet(query)) {
            try {
                response = this.executeQuery(defaultGraphUris, namedGraphUris,
                                query, startOffset, endOffset, gridJson, format,
                                jsonCallback, uriInfo, request, acceptHdr);
            }
            catch (Exception e) {
                this.handleError(query, e);
            }
        }
        else {
            // No query. => Render HTML query input form.
            // Get a list of available repositories for user.
            boolean userAuthenticated = SecurityContext.isUserAuthenticated();
            Collection<Repository> c = Configuration.getDefault()
                                        .getRepositories(! userAuthenticated);
            TemplateModel view = ViewFactory.newView(this.welcomeTemplate);
            view.put("repositories", c);
            view.put("queries", predefinedQueries);
            view.put("namespaces", RdfNamespace.values());
            response = Response.ok(view, TEXT_HTML_UTF8);
        }
        return response.build();
    }

    abstract protected ResponseBuilder doExecute(
                                          List<String> defaultGraphUris,
                                          List<String> namedGraphUris,
                                          String query, int startOffset,
                                          int endOffset, boolean gridJson,
                                          String format, String jsonCallback,
                                          UriInfo uriInfo, Request request,
                                          String acceptHdr,
                                          Map<String,Object> viewData)
                                                            throws Exception;

    protected final Repository getTargetRepository(
                                            List<String> defaultGraphUris)
                                                    throws SecurityException {
        String targetRepo = null;
        if ((defaultGraphUris != null) && (! defaultGraphUris.isEmpty())) {
            targetRepo = defaultGraphUris.remove(0);
        }
        Configuration cfg = Configuration.getDefault();
        Repository repo = cfg.getRepository(targetRepo);
        if (repo == null) {
            // No repository found for first default graph.
            // => Use default DataLift repository.
            defaultGraphUris.add(0, targetRepo);
            repo = cfg.getDefaultRepository();
        }
        else {
            if (! ((repo.isPublic()) ||
                   (SecurityContext.isUserAuthenticated()))) {
                // Repository is not public and user is not authenticated.
                throw new java.lang.SecurityException();
            }
        }
        return repo;
    }

    protected final Variant getResponseType(Request request, String expected,
                                            List<Variant> supportedTypes)
                                                throws WebApplicationException {
        Variant responseType = null;
        log.trace("Negotiating content type for {}", supportedTypes);
        if (! isBlank(expected)) {
            // Specific output format requested thru request parameter.
            // => Try to get a valid response type form it.
            Collection<AcceptType> types = new TreeSet<AcceptType>();
            int i = 0;
            for (String t : expected.split("\\s*,\\s*")) {
                types.add(new AcceptType(t, i++, supportedTypes));
            }
            Iterator<AcceptType> it = types.iterator();
            while ((it.hasNext()) && (responseType == null)) {
                AcceptType t = it.next();
                for (Variant v : supportedTypes) {
                    if (t.mimeType.isCompatible(v.getMediaType())) {
                        responseType = v;
                        break;
                    }
                }
            }
            log.trace("Negotiated content type: [{}] -> {}",
                                                    expected, responseType);
        }
        else {
            // No specific output format requested.
            // => Get matching format from request Accept HTTP header.
            responseType = request.selectVariant(supportedTypes);
        }
        if (responseType == null) {
            // Oops! No matching MIME type found.
            List<MediaType> l = new LinkedList<MediaType>();
            for (Variant v : supportedTypes) {
                l.add(v.getMediaType());
            }
            TechnicalException error = new TechnicalException(
                    (! isBlank(expected))? "explicit.negotiation.failed":
                                           "default.negotiation.failed",
                    l, expected);
            log.error(error.getMessage());
            this.sendError(NOT_ACCEPTABLE, error.getLocalizedMessage());
        }
        log.debug("Negotiated content type: {}", responseType);
        return responseType;
    }

    /**
     * Returns the list of predefined queries to offer the user on this
     * SPARQL endpoint.
     * @return the unmodifiable list of predefined queries, read from
     *         the Datalift configuration.
     */
    protected final List<PredefinedQuery> getPredefinedQueries() {
        return this.predefinedQueries;
    }

    protected final TemplateModel newView(String templateName, Object it) {
        return ViewFactory.newView(
                                "/" + this.getName() + '/' + templateName, it);
    }

    protected final void handleError(String query,
                                     String message, Status status)
                                                throws WebApplicationException {
        if (status == null) {
            status = INTERNAL_SERVER_ERROR;
        }
        log.error("Query processing failed: {}, for \"{}\"", message, query);
        this.sendError(status, message);
    }

    protected final void handleError(URI uri, Exception e)
                                                throws WebApplicationException {
        this.handleError("<" + uri + ">", e);
    }

    protected final void handleError(String query, Exception e)
                                                throws WebApplicationException {
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        else if (e instanceof SecurityException) {
            this.sendError(FORBIDDEN, null);
        }
        else if (e.getCause() instanceof QueryInterruptedException) {
            // Query processing was interrupted as it was taking too much time.
            // => Return HTTP status 413 (Request Entity Too Large).
            TechnicalException error = new TechnicalException(
                                        "query.max.duration.exceeded", query);
            this.sendError(413, error.getLocalizedMessage());
        }
        else if (e.getCause() instanceof QueryDoneException) {
            // End of requested range (start/end offset) successfully reached.
        }
        else {
            log.error("Query processing failed: \"{}\" for: \"{}\"",
                                            e, e.getLocalizedMessage(), query);
            this.sendError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    protected void throwInvalidParamError(String name, Object value) {
        TechnicalException error = (value != null)?
                new TechnicalException("ws.invalid.param.error", name, value):
                new TechnicalException("ws.missing.param", name);
        this.sendError(BAD_REQUEST, error.getLocalizedMessage());
    }

    protected final static String getQueryDesc(String query) {
        String desc = query;
        if (query != null) {
            // Strip prefix declarations.
            Matcher m = QUERY_START_PATTERN.matcher(query);
            if (m.find()) {
                int i = m.start();
                // Get the 100 first chars of the query string, minus prefixes.
                desc = (query.length() - i > MAX_QUERY_DESC)?
                                query.substring(i, MAX_QUERY_DESC + i) + "...":
                                query.substring(i);
            }
        }
        return desc;
    }

    private DescribeType getDescribeTypeFromUri(final String uri,
                                                Repository repository) {
        DescribeType type = null;
        try {
            // Try to determine the URI type by performing a SPARQL query.
            Map<String,Object> bindings = new HashMap<String,Object>();
            bindings.put("u", new URIImpl(uri));
            TupleQueryResultMapper<DescribeType> m =
                                new BaseTupleQueryResultMapper<DescribeType>() {
                    private DescribeType nodeType = null;
                    @Override
                    public void handleSolution(BindingSet b) {
                        if (nodeType == null) {
                            if (b.hasBinding("s")) {
                                nodeType = Object;
                            }
                            else if (b.hasBinding("p")) {
                                nodeType = Predicate;
                            }
                            else if (b.hasBinding("g")) {
                                nodeType = Graph;
                            }
                        }
                        // Else: Already set. => Ignore...
                    }
                    @Override
                    public DescribeType getResult() {
                        return nodeType;
                    }
                };
            if (repository == null) {
                // Use default repository if none is specified.
                repository = Configuration.getDefault().getDefaultRepository();
            }
            // Execute filter query.
            repository.select(DETERMINE_TYPE_QUERY, bindings, m);
            type = m.getResult();
        }
        catch (Exception e) {
            log.warn("Failed to determine object type of <{}>", e, uri);
        }
        return type;
    }
    /**
     * Loads the available predefined SPARQL queries from the RDF
     * file {@link #QUERIES_FILE_PROPERTY} specified in the Datalift
     * configuration or from the default query definition file present
     * in the module JAR.
     * @param  cfg   the Datalift configuration.
     *
     * @return the loaded queries, as a list.
     * @throws TechnicalException if any error occurred while loading
     *         the query definitions.
     */
    private List<PredefinedQuery> loadPredefinedQueries(Configuration cfg) {
        final List<PredefinedQuery> queries = new LinkedList<PredefinedQuery>();

        String path = cfg.getProperty(QUERIES_FILE_PROPERTY);
        InputStream in = null;
        try {
            if (isSet(path)) {
                // Query definition file specified. => Check presence.
                log.info("Loading predefined SPARQL queries from {}", path);
                File f = new File(path);
                if (! (f.isFile() && f.canRead())) {
                    throw new FileNotFoundException(path);
                }
                in = new FileInputStream(f);
            }
            else {
                // No query definition file specified. => Use default.
                log.debug("No queries file specified, using default");
                path = DEFAULT_QUERIES_FILE;
                in = this.getClass().getClassLoader().getResourceAsStream(path);
            }
            // Extract predefined queries from RDF file.
            RdfUtils.queryFile(cfg, in,
                RdfUtils.guessRdfFormatFromExtension(path),
                LOAD_PREDEFINED_QUERIES_QUERY,
                new TupleQueryResultHandlerBase() {
                    private Value currentId = null;
                    private String query;
                    private Map<String,String> labels =
                                                new HashMap<String,String>();
                    @Override
                    public void handleSolution(BindingSet b) {
                        Value id = b.getValue("s");
                        if (! id.equals(this.currentId)) {
                            // Register current query.
                            this.registerQuery();
                            // Switch to next query.
                            this.currentId = id;
                        }
                        this.query = b.getValue("query").stringValue();
                        Literal v  = (Literal)(b.getValue("label"));
                        if (v != null) {
                            this.labels.put(v.getLanguage(), v.getLabel());
                        }
                    }

                    @Override
                    public void endQueryResult() {
                        this.registerQuery();
                    }

                    private void registerQuery() {
                        if ((isSet(this.query)) && (! this.labels.isEmpty())) {
                            // Create and register new query.
                            PredefinedQuery q = new PredefinedQuery(
                                                    this.query, this.labels);
                            queries.add(q);
                            log.trace("Registered predefined SPARQL query \"{}\": {}",
                                      q.getLabel(), q.query);
                            this.labels.clear();
                            this.query = null;
                        }
                        // Else: Ignore...
                    }
                });
        }
        catch (Exception e) {
            throw new TechnicalException("queries.load.failed", e, path);
        }
        finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
        return queries;
    }

    //-------------------------------------------------------------------------
    // QueryDescription
    //-------------------------------------------------------------------------

    /**
     * Helper class to ease logging of SPARQL queries by providing a
     * {@link #toString() shortened description} of the query text,
     * without namespace prefixes declarations and a possibly truncated
     * query body.
     */
    protected final static class QueryDescription
    {
        /** The full text of the SPARQL query. */
        public final String query;
        /** The shortened description of the SPARQL query. */
        private String desc = null;

        /**
         * Default constructor.
         * @param  query   the SPARQL query to wrap.
         */
        public QueryDescription(String query) {
            this.query = query;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            if (this.desc == null) {
                this.desc = getQueryDesc(this.query);
            }
            return this.desc;
        }
    }

    //-------------------------------------------------------------------------
    // AcceptType nested class
    //-------------------------------------------------------------------------

    private final static class AcceptType implements Comparable<AcceptType>
    {
        public final MediaType mimeType;
        public final double priority;
        private final int order;

        public AcceptType(String type, int order, List<Variant> knownTypes) {
            String mimeType = type;
            double priority = 1.0;
            // Extract MIME type and type priority ("quality factor").
            int i = type.indexOf(";");
            if (i != -1) {
                mimeType = type.substring(0, i);
                i = type.indexOf("q=", i);
                if (i != -1) {
                    try {
                        priority = Double.parseDouble(type.substring(i+2));
                    }
                    catch (Exception e) { /* Ignore... */ }
                }
            }
            this.mimeType = this.mapType(mimeType, knownTypes);
            this.priority = priority;
            this.order = order;
        }

        /**
         * Determines the media type from a MIME type string
         * representation. This method accepts shorthand MIME types with
         * only the subtype set (e.g. "json" instead of
         * "application/json").
         * @param  type         the MIME type to map.
         * @param  knownTypes   the well-known MIME types to check
         *                      shorthand types against.
         *
         * @return the matched media type.
         */
        private MediaType mapType(String type, List<Variant> knownTypes) {
            MediaType mimeType = null;
            if ((knownTypes != null) && (type.indexOf('/') == -1)) {
                for (Variant v : knownTypes) {
                    MediaType t = v.getMediaType();
                    if (t.getSubtype().equals(type)) {
                        mimeType = t;
                        break;
                    }
                }
            }
            if (mimeType == null) {
                mimeType = MediaType.valueOf(type);
            }
            return mimeType;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(AcceptType o) {
            // Higher priority comes first in list (i.e. lesser).
            int diff = (int)((o.priority - this.priority) * 100);
            // Same priority? first in list comes first!
            return (diff != 0)? diff: this.order - o.order; 
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object o) {
            boolean equals = (o instanceof AcceptType);
            if (equals) {
                AcceptType t = (AcceptType)o;
                equals = ((this.mimeType.equals(t.mimeType)) &&
                          (((int)((this.priority - t.priority) * 100)) == 0));
            }
            return equals;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.mimeType.hashCode()
                                    + (int)(this.priority * 100) + this.order;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return this.mimeType + "; q=" + this.priority;
        }
    }

    //-------------------------------------------------------------------------
    // PredefinedQuery nested class
    //-------------------------------------------------------------------------

    public final static class PredefinedQuery extends BaseLocalizedItem
    {
        public final String query;

        public PredefinedQuery(String query, Map<String,String> labels) {
            super(labels);
            if (isBlank(query)) {
                throw new IllegalArgumentException("query");
            }
            this.query = query;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return this.query;
        }
    }
}
