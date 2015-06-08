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
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;

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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_TIMEOUT;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.Response.Status.*;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.TupleQueryResultHandlerBase;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.BaseLocalizedItem;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.BaseTupleQueryResultMapper;
import org.datalift.fwk.rdf.ElementType;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.TupleQueryResultMapper;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.web.MainMenu;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.rdf.RdfNamespace.*;
import static org.datalift.fwk.rdf.ElementType.*;


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
     * The configuration property defining the maximum number of entries
     * (statements, binding sets...) to be displayed in HTML pages.
     */
    public final static String MAX_HTML_RESULTS_PROPERTY =
                                            "sparql.max.html.results";
    /** Default value for {@link #MAX_HTML_RESULTS_PROPERTY}. */
    public final static int DEFAULT_MAX_HTML_RESULTS = 1000;
    /**
     * The (optional) configuration property holding the path of
     * the RDF file to load predefined queries from.
     */
    public final static String QUERIES_FILE_PROPERTY =
                                            "sparql.predefined.queries.file";
    /** The default queries file, embedded in module JAR. */
    private final static String DEFAULT_QUERIES_FILE = "predefined-queries.ttl";

    /** The name of the default template for the endpoint welcome page. */
    protected final static String DEFAULT_WELCOME_TEMPLATE =
                                                        "sparqlEndpoint.vm";
    /**
     * The configuration property defining the (client-side) cache
     * duration of SPARQL query results (in minutes).
     */
    public final static String RESULTS_CACHE_DURATION_PROPERTY =
                                            "sparql.results.cache.duration";
    /** Default value for {@link #RESULTS_CACHE_DURATION_PROPERTY}. */
    public final static int DEFAULT_RESULTS_CACHE_DURATION = 3;

    /**
     * The object description query to use when the RDF store provides
     * <a href="http://www.w3.org/Submission/CBD/">Concise Bounded Descriptions</a>
     * of resources. OpenRDF Sesame supports CBD starting with
     * version 2.7.
     */
    private final static MessageFormat CBD_DESCRIBE_OBJECT_QUERY =
            new MessageFormat("DESCRIBE <{0}>");
    /** The default query for object description. */ 
    private final static MessageFormat DEFAULT_DESCRIBE_OBJECT_QUERY =
            // SPARQL 1.0 compliant query (i.e. for Sesame 2.6):
            /* new MessageFormat("CONSTRUCT '{' ?s1 ?p1 ?o1 ."
                              +            " ?o1 ?p2 ?o2 . '}'\n"
                              + "WHERE '{'\n"
                              + "  ?s1 ?p1 ?o1 .\n"
                              + "  OPTIONAL '{'\n"
                              + "    ?o1 ?p2 ?o2 .\n"
                              + "    FILTER isBlank(?o1)\n  '}'\n"
                              + "  FILTER ( ?s1 = <{0}> || ?o1 = <{0}> )\n'}'");
             */
            // SPARQL 1.1 optimized query (for Sesame 2.7+):
            new MessageFormat("CONSTRUCT '{' ?s1 ?p1 ?o1 ."
                              +            " ?o1 ?p2 ?o2 . '}'\n"
                              + "WHERE '{'\n  '{'\n"
                              + "    ?s1 ?p1 ?o1 .\n"
                              + "    OPTIONAL '{'\n"
                              + "      ?o1 ?p2 ?o2 .\n"
                              + "      FILTER isBlank(?o1)\n    '}'\n"
                              + "    VALUES ?s1 '{' <{0}> '}'\n"
                              + "  '}'\n  UNION '{'\n"
                              + "    ?s1 ?p1 ?o1 .\n"
                              + "    VALUES ?o1 '{' <{0}> '}'\n"
                              + "  '}'\n'}'");
    private final static MessageFormat DESCRIBE_PREDICATE_QUERY =
            new MessageFormat("CONSTRUCT '{' ?s <{0}> ?o . '}' WHERE '{'"
                              + " ?s <{0}> ?o . '}'");
    private final static MessageFormat DESCRIBE_TYPE_QUERY =
            new MessageFormat("CONSTRUCT '{' ?s a <{0}> . '}' WHERE '{'"
                              + " ?s a <{0}> . '}'");
    private final static MessageFormat DESCRIBE_GRAPH_QUERY =
            new MessageFormat("CONSTRUCT '{' ?s ?p ?o . '}' WHERE '{'\n"
                              + "  GRAPH <{0}> '{' ?s ?p ?o . '}'\n'}'");

    private final static String DETERMINE_TYPE_QUERY =
            "SELECT DISTINCT ?kind WHERE {\n" +
                "{ ?u ?p1 ?o1 . BIND(\"" + Resource + "\" AS ?kind) }\n" +
                "UNION\n" +
                "{ GRAPH ?u { ?s4 ?p4 ?o4 . BIND(\"" + Graph + "\" AS ?kind) } }\n" +
                "UNION\n" +
                "{ ?s2 ?u ?o2 . BIND(\"" + Predicate + "\" AS ?kind) }\n" +
                "UNION\n" +
                "{ ?s3 a ?u .   BIND(\"" + RdfType + "\" AS ?kind) }\n" +
                "UNION\n" +
                "{ ?s5 ?p5 ?u . BIND(\"" + Value + "\" AS ?kind) }\n}";

    /** The SPARQL query to extract predefined query data. */
    private final static String LOAD_PREDEFINED_QUERIES_QUERY =
                    "PREFIX rdf: <" + RDF.uri + ">\n" +
                    "PREFIX rdfs: <" + RDFS.uri + ">\n" +
                    "PREFIX dcterms: <" + DC_Terms.uri + ">\n" +
                    "PREFIX datalift: <" + DataLift.uri + ">\n" +
                    "SELECT ?s ?label ?query ?position ?description WHERE { " +
                        "?s a datalift:SparqlQuery ; " +
                        "   rdfs:label ?label ; rdf:value ?query . " +
                        "OPTIONAL { ?s datalift:position ?position . " +
                        "           FILTER(isNumeric(?position)) } " +
                        "OPTIONAL { ?s dcterms:description ?description . } }";

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
    /** Whether Concise Bounded Description queries should be used. */
    private boolean useCdb;
    /**
     * Whether to serve graphs instead of resources when the same URI
     * is used for both.
     */
    private boolean serveGraphsFirst;
    /** The client-side cache duration for SPARQL query results. */
    private int resultsCacheInSeconds;

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
     * @param  welcomeTemplate   the name of the template, relative to
     *                           the module path, to display as
     *                           welcome page.
     */
    protected AbstractSparqlEndpoint(String name, String welcomeTemplate) {
        super(name);
        this.welcomeTemplate = (isSet(welcomeTemplate))?
                                    welcomeTemplate: DEFAULT_WELCOME_TEMPLATE;
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        // Check whether Concise Bounded Description is to be used.
        this.useCdb = this.getBoolean(configuration,
                                                CBD_SUPPORT_PROPERTY, true);
        // Check whether graphs shall be served first.
        this.serveGraphsFirst = this.getBoolean(configuration,
                                                PREFER_GRAPHS_PROPERTY, false);
        // Load predefined SPARQL queries.
        this.predefinedQueries = Collections.unmodifiableList(
                                    this.loadPredefinedQueries(configuration));
        // Compute client-side result cache duration.
        String s = configuration.getProperty(RESULTS_CACHE_DURATION_PROPERTY,
                                        "" + DEFAULT_RESULTS_CACHE_DURATION);
        try {
            this.resultsCacheInSeconds = Integer.parseInt(s) * 60;
        }
        catch (Exception e) {
            log.error("Failed to parse value for configuration property {}: " +
                      "{}. Disabling client-side cache",
                      RESULTS_CACHE_DURATION_PROPERTY, s);
            this.resultsCacheInSeconds = -1;
        }
        if (this.resultsCacheInSeconds <= 0) {
            this.resultsCacheInSeconds = 0;
        }
        // Register main menu entry(ies).
        this.registerToMainMenu();
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
        return this.executeQuery(null, null, query, -1, -1, null, null,
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
                            -1, -1, null, null, uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException {
        return this.executeQuery(defaultGraphUris, namedGraphUris, query,
                                startOffset, endOffset, null, null,
                                uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset,
                            String format, String jsonCallback,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            if ((! isBlank(jsonCallback)) && (isBlank(format))) {
                format = APPLICATION_JSON;
            }
            if (startOffset <= 0) {
                startOffset = -1;
            }
            if (endOffset <= 0) {
                endOffset = -1;
            }
            response = this.doExecute(defaultGraphUris, namedGraphUris, query,
                                      startOffset, endOffset,
                                      format, jsonCallback,
                                      uriInfo, request, acceptHdr, null, null);
        }
        catch (Exception e) {
            this.handleError(query, e);
        }
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder describe(String uri, ElementType type,
                                UriInfo uriInfo, Request request,
                                String acceptHdr, List<Variant> allowedTypes)
                                                throws WebApplicationException {
        return this.describe(uri, type, null, null, null, -1, null, null,
                                  uriInfo, request, acceptHdr, allowedTypes);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder describe(
                    String uri, ElementType type, Repository repository,
                    UriInfo uriInfo, Request request,
                    String acceptHdr, List<Variant> allowedTypes)
                                                throws WebApplicationException {
        return this.describe(uri, type, repository, null, null, -1, null, null,
                             uriInfo, request, acceptHdr, allowedTypes);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder describe(
                    String uri, ElementType type, Repository repository,
                    List<String> defaultGraphUris, List<String> namedGraphUris,
                    UriInfo uriInfo, Request request,
                    String acceptHdr, List<Variant> allowedTypes)
                                                throws WebApplicationException {
        return this.describe(uri, type, repository,
                             defaultGraphUris, namedGraphUris, -1, null, null,
                             uriInfo, request, acceptHdr, allowedTypes);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder describe(
                    String uri, ElementType type, Repository repository,
                    List<String> defaultGraphUris, List<String> namedGraphUris,
                    int max, String format, String jsonCallback,
                    UriInfo uriInfo, Request request,
                    String acceptHdr, List<Variant> allowedTypes)
                                                throws WebApplicationException {
        if (isBlank(uri)) {
            this.throwInvalidParamError("uri", uri);
        }
        // Build the list of target graphs, including the target repository.
        List<String> defGraphs = new LinkedList<String>();
        if (repository != null) {
            defGraphs.add(repository.name);
        }
        if (defaultGraphUris != null) {
            defGraphs.addAll(defaultGraphUris);
        }
        if ((repository == null) && (! defGraphs.isEmpty())) {
            try {
                repository = Configuration.getDefault()
                                          .getRepository(defGraphs.get(0));
            }
            catch (MissingResourceException e) { /* Ignore... */ }
        }

        ResponseBuilder response = null;
        try {
            URI u = URI.create(uri).normalize();
            if (! u.isAbsolute()) {
                u = uriInfo.getBaseUri().resolve(u);
            }
            if ((type == null) || (type == Value)) {
                // Force revalidation of values to prevent redirecting user
                // to a malevolent site in case of forged URI in request.
                type = this.getDescribeTypeFromUri(u, repository);
            }
            if (type == Value) {
                // Requested URI found in RDF store but only as a value.
                String scheme = u.getScheme();
                if (("http".equals(scheme)) || ("https".equals(scheme))) {
                    // Redirect to target URI.
                    response = Response.seeOther(u);
                }
                // Else: No handling of non HTTP URIs. => Send a 404 status.
            }
            else if (type != null) {
                // URI found in RDF store as a subject, predicate or graph.
                String query = null;
                MessageFormat fmt =
                            (type == Predicate)? DESCRIBE_PREDICATE_QUERY:
                            (type == Graph)?     DESCRIBE_GRAPH_QUERY:
                            (type == RdfType)?   DESCRIBE_TYPE_QUERY:
                            (this.useCdb)?       CBD_DESCRIBE_OBJECT_QUERY:
                                                 DEFAULT_DESCRIBE_OBJECT_QUERY;
                synchronized (fmt) {
                    query = fmt.format(new Object[] { u });
                }
                Map<String,Object> viewData = new HashMap<String,Object>();
                viewData.put("describe-type", type);
                viewData.put("describe-uri",  u);
                response = this.doExecute(defGraphs, namedGraphUris, query,
                                          -1, max, format, jsonCallback,
                                          uriInfo, request, acceptHdr,
                                          allowedTypes, viewData);
            }
            if (response == null) {
                this.sendError(NOT_FOUND, null);
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
                @QueryParam("format") String format,
                @QueryParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, format,
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
                @FormParam("format") String format,
                @FormParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        return this.getQuery(defaultGraphUris, namedGraphUris, query,
                             startOffset, endOffset, format,
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
                @QueryParam("format") String format,
                @QueryParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        List<String> defGraphUris = new LinkedList<String>();
        defGraphUris.add(repository);
        if (defaultGraphUris != null) {
            defGraphUris.addAll(defaultGraphUris);
        }
        return this.dispatchQuery(defGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, format,
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
                @FormParam("format") String format,
                @FormParam("callback") String jsonCallback,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        return this.getStoreQuery(repository, defaultGraphUris, namedGraphUris,
                                  query, startOffset, endOffset, format,
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
     * @param  uri                the URI of the object to describe.
     * @param  type               the type of the object or
     *                            <code>null</code> if unknown.
     * @param  defaultGraphUris   the <code>default-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  namedGraphUris     the <code>named-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  max                the max. number of results to return.
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
    @Path("describe")
    public final Response getDescribe(
                @QueryParam("uri") String uri,
                @QueryParam("type") String type,
                @QueryParam("default-graph-uri") List<String> defaultGraphUris,
                @QueryParam("named-graph-uri") List<String> namedGraphUris,
                @QueryParam("max") @DefaultValue("-1") int max,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        return this.describe(uri, ElementType.fromString(type),
                             null, defaultGraphUris, namedGraphUris,
                             max, null, null,
                             uriInfo, request, acceptHdr, null)
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
     * @param  uri                the URI of the object to describe.
     * @param  type               the type of the object or
     *                            <code>null</code> if unknown.
     * @param  defaultGraphUris   the <code>default-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  namedGraphUris     the <code>named-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  max                the max. number of results to return.
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
    @Path("describe")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public final Response postDescribe(
                @FormParam("uri") String uri,
                @FormParam("type") String type,
                @FormParam("default-graph-uri") List<String> defaultGraphUris,
                @FormParam("named-graph-uri") List<String> namedGraphUris,
                @FormParam("max") @DefaultValue("-1") int max,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        return this.getDescribe(uri, type, defaultGraphUris, namedGraphUris,
                                max, uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private final Response dispatchQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset,
                            String format, String jsonCallback,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;

        // Check for empty query.
        if (StringUtils.isSet(query)) {
            try {
                response = this.executeQuery(defaultGraphUris, namedGraphUris,
                                query, startOffset, endOffset, format,
                                jsonCallback, uriInfo, request, acceptHdr);
                if (this.resultsCacheInSeconds != 0) {
                    // Add cache directives.
                    CacheControl cc = new CacheControl();
                    cc.setMaxAge(this.resultsCacheInSeconds);
                    cc.setPrivate(true);
                    cc.setNoTransform(false);
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.add(Calendar.SECOND, this.resultsCacheInSeconds);
                    response = response.header(VARY, AUTHORIZATION)
                                       .cacheControl(cc)
                                       .expires(cal.getTime());
                }
            }
            catch (Exception e) {
                this.handleError(query, e);
            }
        }
        else {
            // No query. => Render HTML query input form.
            response = this.displayWelcomePage();
        }
        return response.build();
    }

    abstract protected ResponseBuilder doExecute(
                                  List<String> defaultGraphUris,
                                  List<String> namedGraphUris, String query,
                                  int startOffset, int endOffset,
                                  String format, String jsonCallback,
                                  UriInfo uriInfo, Request request,
                                  String acceptHdr, List<Variant> allowedTypes,
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
        Repository repo = null;
        try {
            repo = cfg.getRepository(targetRepo);
            if ((! repo.isPublic()) &&
                (SecurityContext.getUserPrincipal() == null)) {
                // Repository is not public and user is not authenticated.
                throw new java.lang.SecurityException();
            }
        }
        catch (MissingResourceException e) {
            // No repository found for first default graph.
            // => Use default DataLift repository.
            defaultGraphUris.add(0, targetRepo);
            repo = cfg.getDefaultRepository();
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
            log.trace("Negotiated content type: {}", responseType);
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
        return responseType;
    }

    protected final ResponseBuilder displayWelcomePage() {
        return this.displayWelcomePage(this.welcomeTemplate);
    }

    // Render HTML query input form.
    protected final ResponseBuilder displayWelcomePage(String template) {
        if (! isSet(template)) {
            throw new IllegalArgumentException("template");
        }
        // Get a list of available repositories for user.
        boolean userAuthenticated = (SecurityContext.getUserPrincipal() != null);
        Collection<Repository> c = Configuration.getDefault()
                                        .getRepositories(! userAuthenticated);
        // Sort predefined queries by position first and label second.
        List<PredefinedQuery> queries =
                            new ArrayList<PredefinedQuery>(predefinedQueries);
        Collections.sort(queries, new PredefinedQueryComparator());
        // Create and populate view template.
        TemplateModel view = this.newView(template, null);
        view.put("repositories", c);
        view.put("queries", queries);
        view.put("namespaces", RdfNamespace.values());
        view.put("max", wrap(this.getDefaultMaxResults()));
        ResponseBuilder response = Response.ok(view, TEXT_HTML_UTF8);
        // Add cache directives.
        CacheControl cc = new CacheControl();
        cc.setMaxAge(7 * 24 * 3600);            // One week in seconds.
        cc.setPrivate(userAuthenticated);       // Only public for anon.
        cc.setNoTransform(false);
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_YEAR, 7);       // One week in days.
        response = response.header(VARY, AUTHORIZATION)
                           .cacheControl(cc)
                           .expires(cal.getTime());
        return response;
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

    /**
     * Returns a new template model for web service response rendering.
     * @param  templateName   the template name, relative to the module.
     * @param  it             the (optional) model object.
     *
     * @return a new template view.
     */
    protected final TemplateModel newView(String templateName, Object it) {
        return ViewFactory.newView(
                                "/" + this.getName() + '/' + templateName, it);
    }

    /**
     * Returns the value of the specified property in the Datalift
     * configuration as a boolean value.
     * <p>
     * The following mapping applies:</p>
     * <dl>
     *  <dt><code>true</code></dt>
     *  <dd>if the property value (case insensitive) is
     *      "<code>true</code>", "<code>yes</code>" or
     *      "<code>1</code>"</dd>
     *  <dt><code>false</code></dt>
     *  <dd>if the property value (case insensitive) is
     *      "<code>false</code>", "<code>no</code>" or
     *      "<code>0</code>"</dd>
     *  <dt><code>def</code>, the default value</dt>
     *  <dd>otherwise</dd>
     * </dl>
     * @param  cfg   the Datalift configuration.
     * @param  key   the property name.
     * @param  def   the value to return if the property is absent from
     *               the configuration.
     *
     * @return the property value as a boolean.
     */
    protected boolean getBoolean(Configuration cfg, String key, boolean def) {
        boolean value = def;

        String s = trimToNull(cfg.getProperty(key));
        if (s != null) {
            s = s.toLowerCase();
            if ((s.equals(Boolean.TRUE.toString())) || (s.equals("yes"))
                                                    || (s.equals("1"))) {
                value = true;
            }
            else if ((s.equals(Boolean.FALSE.toString())) || (s.equals("no"))
                                                          || (s.equals("0"))) {
                value = false;
            }
            // Else: use default value.
        }
        return value;
    }

    /**
     * Register one (or several) main menu entry(ies) to access this
     * SPARQL endpoint.
     */
    protected void registerToMainMenu() {
        MainMenu.get().add(new MainMenu.EntryDesc(
                        MODULE_NAME, "sparql.endpoint.title",
                        MainMenu.DEFAULT_BUNDLE_NAME, this, 1, null));
    }

    /**
     * Returns the configured maximum number of HTML results for
     * SPARQL queries.
     * @return the configured maximum number of HTML results.
     */
    protected int getDefaultMaxResults() {
        return this.getDefaultMaxResults(-1, -1);
    }

    /**
     * Computes the maximum number of HTML results for the
     * being-processed SPARQL query.
     * @param  startOffset   the first requested result.
     * @param  endOffset     the last requested result.
     *
     * @return the maximum number of HTML results for a query.
     */
    protected int getDefaultMaxResults(int startOffset, int endOffset) {
        if (endOffset <= 0) {
            // Compute max number of results from configuration.
            String v = Configuration.getDefault()
                                    .getProperty(MAX_HTML_RESULTS_PROPERTY);
            if (v != null) {
                try {
                    endOffset = Integer.parseInt(v);
                }
                catch (Exception e) {
                    log.warn("Invalid value for configuration parameter \"{}\": " +
                             "\"{}\". Integer value expected.",
                             MAX_HTML_RESULTS_PROPERTY, v);
                }
            }
            if (endOffset <= 0) {
                endOffset = DEFAULT_MAX_HTML_RESULTS;
            }
            if (startOffset >= 0) {
                endOffset += startOffset;
            }
        }
        // Else: honor contract.
        return endOffset;
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
            // => Return HTTP status 408 (Request Timeout).
            TechnicalException error = new TechnicalException(
                                        "query.max.duration.exceeded", query);
            // No constant for 408 provided by Jersey. => Using Servlet API.
            this.sendError(SC_REQUEST_TIMEOUT, error.getLocalizedMessage());
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

    private ElementType getDescribeTypeFromUri(URI uri,
                                                Repository repository) {
        ElementType type = null;
        try {
            // Try to determine the URI type by performing a SPARQL query.
            Map<String,Object> bindings = new HashMap<String,Object>();
            bindings.put("u", uri);
            TupleQueryResultMapper<ElementType> m =
                                new BaseTupleQueryResultMapper<ElementType>() {
                    private Set<ElementType> nodeKinds =
                        new TreeSet<ElementType>(comparator(serveGraphsFirst));

                    @Override
                    public void handleSolution(BindingSet b) {
                        if (b.hasBinding("kind")) {
                            ElementType kind = ElementType.fromString(
                                            b.getValue("kind").stringValue());
                            if (kind != null) {
                                this.nodeKinds.add(kind);
                            }
                        }
                    }
                    @Override
                    public ElementType getResult() {
                        return (! this.nodeKinds.isEmpty())?
                                        this.nodeKinds.iterator().next(): null;
                    }
                };
            if (repository == null) {
                // Use default repository if none is specified.
                repository = Configuration.getDefault().getDefaultRepository();
            }
            // Execute filter query.
            repository.select(DETERMINE_TYPE_QUERY, bindings, m,
                                                    null, null, false);
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
                log.info("No predefined queries file specified, using default");
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
                    private int position = Integer.MAX_VALUE;
                    private Map<String,String> labels =
                                                new HashMap<String,String>();
                    private Map<String,String> descriptions =
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
                        v  = (Literal)(b.getValue("position"));
                        if (v != null) {
                            this.position = v.intValue();
                        }
                        v  = (Literal)(b.getValue("description"));
                        if (v != null) {
                            this.descriptions.put(v.getLanguage(), v.getLabel());
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
                                                this.query,  this.position,
                                                this.labels, this.descriptions);
                            queries.add(q);
                            log.trace("Registered predefined SPARQL query \"{}\": {}",
                                      q.getLabel(), q.query);
                            this.query = null;
                            this.position = Integer.MAX_VALUE;
                            this.labels.clear();
                            this.descriptions.clear();
                        }
                        // Else: Ignore...
                    }
                });
        }
        catch (Exception e) {
            TechnicalException error =
                        new TechnicalException("queries.load.failed", e, path);
            log.error(error.getLocalizedMessage(), e);
        }
        finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
        return queries;
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
        private final static String DESCRIPTION_LABEL = "description";
        public final int position;
        public final String query;

        public PredefinedQuery(String query, int position,
                                             Map<String,String> labels,
                                             Map<String,String> descriptions) {
            super(labels);
            if (isBlank(query)) {
                throw new IllegalArgumentException("query");
            }
            this.query = query;
            this.position = position;
            this.setLabels(DESCRIPTION_LABEL, descriptions);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return this.query;
        }

        /**
         * Returns the description of this query in the user's preferred
         * locale.
         * @return the description of this query.
         */
        public String getDescription() {
            return this.getTypeLabel(DESCRIPTION_LABEL);
        }
    }

    //-------------------------------------------------------------------------
    // PredefinedQueryComparator nested class
    //-------------------------------------------------------------------------

    private final static class PredefinedQueryComparator
                                        implements Comparator<PredefinedQuery>
    {
        private final Collator collator = Collator.getInstance(
                                                PreferredLocales.get().get(0));

        public PredefinedQueryComparator() {
            super();
        }

        /** {@inheritDoc} */
        @Override
        public int compare(PredefinedQuery o1, PredefinedQuery o2) {
            int d = o1.position - o2.position;
            if (d == 0) {
                d = this.collator.compare(o1.getLabel(), o2.getLabel());
            }
            return d;
        }
    }
}
