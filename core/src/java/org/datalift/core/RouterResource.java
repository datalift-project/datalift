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

package org.datalift.core;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.Response.Status.*;

import org.openrdf.model.Literal;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerBase;

import org.datalift.core.util.web.DefaultCacheConfiguration;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.FileStore;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.Module;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.ElementType;
import org.datalift.fwk.rdf.QueryDescription;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.sparql.SparqlQueries;
import org.datalift.fwk.util.UriPolicy;
import org.datalift.fwk.util.UriPolicy.ResourceHandler;
import org.datalift.fwk.util.web.CacheConfiguration;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * A JAX-RS resource that routes web service calls to registered
 * modules/resources and resolves unmapped URLs to local files or
 * published RDF resources.
 * <p>
 * For file and RDF resources, cache control directives can be
 * inserted. Their presence and values are controlled by two
 * configuration properties:</p>
 * <ul>
 *  <li><code>datalift.cache.duration</code> defines the cache duration,
 *      in seconds. A negative value disables caching. Default is 2
 *      hours (7200 seconds).</li>
 *  <li><code>datalift.cache.businessDay</code> defines the business day
 *      hours, i.e. hours during which data updates are expected.
 *      Outside these hours, cache expiry is set to the beginning of the
 *      next business day. Format is
 *      <code>&lt;opening hour&gt;-&lt;closing hour&gt;</code>. Use
 *      <code>-</code> or <code>0-23</code> to disable usage of business
 *      day in cache management. Default is <code>8-20</code>.</li>
 * </ul>
 *
 * @author hdevos
 */
@Path("/")
public class RouterResource implements LifeCycle, ResourceResolver
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The resource serving order: static (file) or RDF resources first. */
    public final static String SERVE_RDF_RESOURCES_FIRST_PROPERTY =
                                                "datalift.resources.rdf.first";
    /**
     * The module sub-directory where to look for classes first. If
     * no present, root-level JAR files will be searched.
     */
    public final static String MODULE_CLASSES_DIR = "classes";
    /** The module sub-directory where to look for third-party JAR files. */
    public final static String MODULE_LIB_DIR     = "lib";
    /**
     * The module sub-directory exposing remotely accessible static
     * resources.
     */
    public final static String MODULE_PUBLIC_DIR  = "public";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Cache management configuration. */
    private CacheConfiguration cacheConfig = new DefaultCacheConfiguration();
    /** Whether to serve static resources (files) before RDF resources. */
    private boolean serveStaticResourcesFirst = true;
    /** Application modules. */
    private final Map<String,Bundle> modules = new TreeMap<String,Bundle>();
    /** Resource resolvers. */
    private final List<UriPolicy> policies = new LinkedList<UriPolicy>();
    /** Predefined SPARQL queries for DataLift core module. */
    private final SparqlQueries queries =
                                new SparqlQueries(RouterResource.class);
    /** Default URI policy. */
    private final UriPolicy defaultPolicy = new UriPolicy()
        {
            @Override public void init(Configuration cfg)     { /* NOP */ }
            @Override public void postInit(Configuration cfg) { /* NOP */ }
            @Override public void shutdown(Configuration cfg) { /* NOP */ }
            @Override public ResourceHandler canHandle(URI uri, UriInfo uriInfo,
                                            Request request, String acceptHdr) {
                return new DefaultUriHandler(uri, uriInfo, request, acceptHdr);
            }
        };

    //-------------------------------------------------------------------------
    // LifeCycle contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation looks for modules (packaged as directories
     * or JAR files) in the DataLift module storage directories to
     * load them and register the resources they declare.</p>
     */
    @Override
    public void init(Configuration configuration) {
        // Read and install cache configuration.
        this.cacheConfig = new DefaultCacheConfiguration(configuration);
        configuration.registerBean(CacheConfiguration.DEFAULT_CONFIG_NAME,
                                   this.cacheConfig);
        // Read resource serving order.
        this.serveStaticResourcesFirst = (! Boolean.parseBoolean(
                configuration.getProperty(SERVE_RDF_RESOURCES_FIRST_PROPERTY,
                                          "true")));
    }

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        // Get the list of published Datalift modules and their bundle.
        this.modules.clear();
        Map<ClassLoader,Bundle> bundles = new HashMap<ClassLoader,Bundle>();
        for (Bundle b : configuration.getBeans(Bundle.class)) {
            bundles.put(b.getClassLoader(), b);
        }
        for (Module m : configuration.getBeans(Module.class)) {
            this.modules.put(m.getName(),
                             bundles.get(m.getClass().getClassLoader()));
        }
        // Load available URI policies, ignoring errors.
        this.policies.clear();
        for (Bundle b : configuration.getBeans(Bundle.class)) {
            int count = 0;
            // Load & install bundle-provided URI policies, if any.
            for (UriPolicy p : b.loadServices(UriPolicy.class)) {
                try {
                    p.init(configuration);
                    p.postInit(configuration);
                    // Make policy available thru the Configuration object.
                    configuration.registerBean(p);
                    // Register policy.
                    this.policies.add(p);
                    count++;
                }
                catch (Exception e) {
                    log.error("Failed to load URI policy {} for bundle {}", e,
                                                    p.getClass().getName(), b);
                    // Skip policy...
                }
            }
            if (count != 0) {
                // Notify whether URI policies were installed.
                log.info("Registered {} URI policy(ies) for bundle \"{}\"",
                         Integer.valueOf(count), b);
            }
        }

        // Check whether SPARQL endpoint module is available.
        try {
            configuration.getBean(SparqlEndpoint.class);
        }
        catch (Exception e) {
            log.warn("No SPARQL endpoint module available");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown(Configuration configuration) {
        // Shutdown each URI policy, ignoring errors.
        for (UriPolicy p : this.policies) {
            try {
                p.shutdown(configuration);
                configuration.removeBean(p, null);
            }
            catch (Exception e) {
                log.error("Failed to properly shutdown URI policy {}: {}", e,
                          p.getClass(), e.getMessage());
                // Continue with next policy.
            }
        }
    }

    //-------------------------------------------------------------------------
    // ResourceResolver contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Response resolveStaticResource(String path, Request request)
                                                throws WebApplicationException {
        FileStore fs = Configuration.getDefault().getPublicStorage();
        if (fs == null) {
            this.sendError(NOT_FOUND, null);
        }
        return this.resolveStaticResource(fs, path, request);
    }

    /** {@inheritDoc} */
    @Override
    public Response resolveRdfResource(UriInfo uriInfo, Request request,
                                       String acceptHdr)
                                                throws WebApplicationException {
        return this.resolveRdfResource(null, uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public Response resolveRdfResource(URI uri, UriInfo uriInfo,
                                       Request request, String acceptHdr)
                                                throws WebApplicationException {
        if (uriInfo == null) {
            throw new IllegalArgumentException("uriInfo");
        }
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        if (uri == null) {
            uri = uriInfo.getAbsolutePath().normalize();
        }

        // Find a resource handler supporting the requested URI.
        ResourceHandler handler = null;
        for (Iterator<UriPolicy> i=this.policies.iterator(); i.hasNext(); ) {
            handler = i.next().canHandle(uri, uriInfo, request, acceptHdr);
            if (handler != null) break;
        }
        if (handler == null) {
            // URI not supported by any configured handler. => Use default.
            handler = this.defaultPolicy.canHandle(uri, uriInfo,
                                                        request, acceptHdr);
        }
        Response response = null;
        // Check whether a 303 redirection is required for accessing resource.
        URI target = handler.resolve();
        if ((target == null) || (target.equals(uri))) {
            response = handler.getRepresentation();
        }
        else {
            response = Response.seeOther(target)
                            .header(HttpHeaders.VARY, "Accept, Accept-Encoding")
                            .build();
        }
        return response;
    }

    public Response resolveModuleResource(String module,
                                          UriInfo uriInfo, Request request,
                                          String acceptHdr)
                                                throws WebApplicationException {
        return this.resolveUnmappedResource(module, this.modules.get(module),
                                            uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Resource web services
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Attempts to handle an unmatched request
     * by resolving the request path against files in public storage
     * and the requested URI against subjects and named graphs present
     * in the public RDF store.
     * @param  path        the request path.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a JAX-RS response if the request was resolved to a local
     *         file or RDF resource, <code>null</code> otherwise.
     * @throws WebApplicationException if any error occurred while
     *         accessing the resolved resource.
     */
    @Path("{path: .*$}")
    public ResponseWrapper resourceForwarding(
                                     @PathParam("path") String path,
                                     @Context UriInfo uriInfo,
                                     @Context Request request,
                                     @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = this.resolveUnmappedResource(null, null, uriInfo,
                                                         request, acceptHdr);
        return (response != null)? new ResponseWrapper(response): null;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Resolved an unmapped web service request against the application
     * {@link Configuration#getLocalStorage() public local file storage}
     * or an RDF resource in the
     * {@link Configuration#getDataRepository() public data RDF store}.
     * @param  module      the first element of the request path.
     * @param  bundle      the target bundle if the first element of the
     *                     request path matches the name of one of the
     *                     registered Datalift modules,
     *                     <code>null</code> otherwise.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value (injected).
     *
     * @return a {@link Response service response} with the file data
     *         or the result of the SPARQL DESCRIBE query on the RDF
     *         resource.
     * @throws WebApplicationException if the request path can not be
     *         resolved.
     */
    private Response resolveUnmappedResource(String module,
                                             Bundle bundle,
                                             UriInfo uriInfo,
                                             Request request,
                                             String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;

        // Get relative resource path
        String path = uriInfo.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        log.trace("Resolving unmapped resource: {}", path);
        // Normalize path, e.g. to resolve relative directories ("x/../y").
        path = URI.create(path).normalize().toString();

        try {
            if (bundle != null) {
                // Path prefix was resolved as a module name.
                // => Try to resolve resource as a module static resource.
                URL src = null;
                String rsc = path.substring(module.length());
                if ((rsc.length() != 0) && (! "/".equals(rsc))) {
                    rsc = MODULE_PUBLIC_DIR + rsc;
                    src = bundle.getResource(rsc);
                }
                // Else: Empty path after module name. => Ignore.

                if (src != null) {
                    // Module static resource found.
                    // => Check whether up-to-date data shall be returned.
                    File f = (bundle.isJar())? bundle.getBundleFile():
                                               new File(src.getFile());
                    Date lastModified = new Date(f.lastModified());
                    ResponseBuilder b = request.evaluatePreconditions(
                                                                lastModified);
                    if (b == null) {
                        // Get MIME type from file extension.
                        String mt = new MimetypesFileTypeMap().getContentType(rsc);
                        log.trace("Serving module public resource: {}/{} ({})",
                                  module, rsc, mt);
                        b = Response.ok(src.openStream(), mt);
                    }
                    response = this.cacheConfig.addCacheDirectives(b,
                                                        lastModified).build();
                }
            }
            if ((response == null) && (this.serveStaticResourcesFirst)) {
                // Not a module static resource.
                // => Try to match a file on public storage.
                response = this.resolveStaticResource(path, request);
            }
            if (response == null) {
                // Not a public file. => Check triple store for RDF resource.
                response = resolveRdfResource(uriInfo, request, acceptHdr);
            }
            if ((response == null) && (! this.serveStaticResourcesFirst)) {
                // Not an RDF resource.
                // => Try to match a file on public storage.
                response = this.resolveStaticResource(path, request);
            }
            // Else: Return null to notify that no match was found.

            if (response == null) {
                log.warn("Failed to resolve resource: {}", uriInfo.getPath());
            }
        }
        catch (WebApplicationException e) {
            throw e;
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
            log.error(error.getMessage(), e);
            this.sendError(INTERNAL_SERVER_ERROR, error.getMessage());
        }
        return response;
    }

    /**
     * Attempts to resolve a request as a local static resource.
     * @param  root      the document root directory.
     * @param  path      the request resource.
     * @param  request   the HTTP request being processed.
     *
     * @return a JAX-RS response pointing to the resolved static
     *         resource of <code>null</code> if no matching resource
     *         was found.
     * @throws WebApplicationException if the resolved request path
     *         points outside of the specified document root directory.
     */
    private Response resolveStaticResource(FileStore store, String path,
                                           Request request)
                                                throws WebApplicationException {
        if (store == null) {
            throw new IllegalArgumentException("store");
        }
        if (! isSet(path)) {
            throw new IllegalArgumentException("path");
        }
        Response response = null;

        File f = store.getFile(path);
        if ((f != null) && (f.isFile()) && (f.canRead())) {
            // Path resolved as an existing file.
            // Check whether data shall be returned.
            Date lastModified = new Date(f.lastModified());
            ResponseBuilder b = request.evaluatePreconditions(lastModified);
            if (b == null) {
                // Get MIME type from file extension.
                String mt = new MimetypesFileTypeMap().getContentType(f);
                log.trace("Serving static resource: {} ({})", f, mt);
                try {
                    b = Response.ok(store.getInputStream(f), mt);
                }
                catch (FileNotFoundException e) {
                    this.sendError(NOT_FOUND, null);
                }
                catch (IOException e) {
                    TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
                    log.error(error.getMessage(), e);
                    this.sendError(INTERNAL_SERVER_ERROR, error.getMessage());
                }
            }
            response = this.cacheConfig.addCacheDirectives(b, lastModified)
                           .build();
        }
        return response;
    }

    /**
     * Helper method to build a JAX-RS web service error response
     * @param  status    the {@link Status HTTP status code}.
     * @param  message   an optional error message to return
     *                   to the service user.
     *
     * @throws WebApplicationException always.
     */
   private void sendError(Status status, String message)
                                            throws WebApplicationException {
        ResponseBuilder r = Response.status(status);
        if (isSet(message)) {
            r.entity(message).type(MediaTypes.TEXT_PLAIN);
        }
        throw new WebApplicationException(r.build());
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
    // ResponseWrapper nested class
    //-------------------------------------------------------------------------

    /**
     * A catch-all JAX-RS sub-resource that accepts all HTTP GET
     * requests and returns a prepared response.
     */
    public static final class ResponseWrapper
    {
        private final Response response;

        /**
         * Creates a new response wrapper.
         * @param  response   the JAX-RS response to return.
         */
        public ResponseWrapper(Response response) {
            this.response = response;
        }

        /**
         * Catch-all GET request processing method.
         * @return the prepared response.
         */
        @GET
        public Response doGet() {
            return this.response;
        }

        /**
         * Catch-all POST request processing method.
         * @return the prepared response.
         */
        @POST
        public Response doPost() {
            return this.response;
        }
    }

    //-------------------------------------------------------------------------
    // DefaultHandler inner class
    //-------------------------------------------------------------------------

    /**
     * The default {@link ResourceHandler} implementation.
     * <p>
     * This implementation serves the RDF resources and named graphs
     * from Datalift public RDF store. It relies on the installed
     * {@link SparqlEndpoint} implementation for providing the
     * response content, as the result of a SPARQL DESCRIBE query, as
     * well as negotiating the best matching representation.</p>
     */
    private final class DefaultUriHandler implements ResourceHandler
    {
        private final URI     uri;
        private final UriInfo uriInfo;
        private final Request request;
        private final String acceptHdr;

        /**
         * Creates a new URI handler.
         * @param  uri         the URI of the RDF resource being accessed.
         * @param  uriInfo     the request URI data.
         * @param  request     the JAX-RS Request object, for content
         *                     negotiation.
         * @param  acceptHdr   the HTTP Accept header, for content
         *                     negotiation.
         */
        public DefaultUriHandler(URI uri, UriInfo uriInfo,
                                 Request request, String acceptHdr) {
            this.uri       = uri;
            this.uriInfo   = uriInfo;
            this.request   = request;
            this.acceptHdr = acceptHdr;
        }

        /**
         * {@inheritDoc}
         * @return <code>null</code> always.
         */
        @Override
        public URI resolve() throws WebApplicationException {
            return null;
        }

        /**
         * {@inheritDoc}
         * <p>
         * This implementation checks that the requested URI matches
         * a RDF resource of named graph in the Datalift public
         * RDF store and forwards the request as a DESCRIBE query to
         * the SPARQL endpoint.</p>
         */
        @Override
        public Response getRepresentation() throws WebApplicationException {
            // Retrieve platform SPARQL endpoint.
            SparqlEndpoint sparqlEndpoint =
                    Configuration.getDefault().getBean(SparqlEndpoint.class);
            if (sparqlEndpoint == null) {
                // No endpoint, no RDF resource description!
                return null;
            }
            Response response = null;

            // Check that the requested URI exists as subject in the
            // public data RDF store.
            String query = queries.get("checkExists");
            ExistsQueryResultHandler result = new ExistsQueryResultHandler();
            try {
                Map<String,Object> bindings = new HashMap<String,Object>();
                bindings.put("u", this.uri);
                Configuration.getDefault().getDataRepository()
                                          .select(query, bindings, result,
                                                         null, null, false);
            }
            catch (Exception e) {
                log.warn("Failed to execute URI type determination query " +
                         "\"{}\" for \"{}\"", e,
                         new QueryDescription(query), this.uri);
                // Assume the URI identifies a resource.
                result.type = ElementType.Resource;
            }
            if (result.type != null) {
                // URI found as subject in RDF store.
                // => Check whether data shall be returned.
                ResponseBuilder b = null;
                if (result.lastModified != null) {
                    b = this.request.evaluatePreconditions(result.lastModified);
                }
                if (b == null) {
                    // Data recently updated or not cached by client.
                    // => Get subject description from SPARQL endpoint.
                    log.trace("Resolved requested URI {} as RDF resource", uri);
                    b = sparqlEndpoint.describe(
                                    this.uri.toString(), result.type,
                                    this.uriInfo, this.request, this.acceptHdr,
                                    null);
                }
                // Else: Client already up-to-date.

                response = cacheConfig.addCacheDirectives(b,
                                                result.lastModified).build();
            }
            return response;
        }
    }

    //-------------------------------------------------------------------------
    // ExistsQueryResultHandler nested class
    //-------------------------------------------------------------------------

    private static final class ExistsQueryResultHandler
                                        extends TupleQueryResultHandlerBase {
        public ElementType type = null;
        public Date        lastModified = null;

        public ExistsQueryResultHandler() {
            super();
        }

        /** {@inheritDoc} */
        @Override
        public void handleSolution(BindingSet b) {
            if (this.type == null) {
                // Store information for the first matched entry.
                this.type = (b.hasBinding("s"))? ElementType.Resource:
                            (b.hasBinding("g"))? ElementType.Graph: null;
                if (this.type != null) {
                    this.lastModified = this.extractDate(b, "lastModified");
                }
            }
            // Else: Ignore subsequent entries.
        }

        private Date extractDate(BindingSet b, String name) {
            Date d = null;
            if (b.hasBinding(name)) {
                try {
                    // Try to extract date.
                    d = ((Literal)(b.getValue(name))).calendarValue()
                                                     .toGregorianCalendar()
                                                     .getTime();
                }
                catch (Exception e) { /* No date here... */ }
            }
            return d;
        }
    }
}
