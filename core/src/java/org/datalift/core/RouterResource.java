package org.datalift.core;


import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

import static java.util.GregorianCalendar.*;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.openrdf.model.Literal;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerBase;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.core.log.LogContext;
import org.datalift.core.velocity.jersey.VelocityTemplateProcessor;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.Module;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.sparql.SparqlQueries;

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

    /** The default cache duration for static & RDF resources. */
    public final static String CACHE_DURATION_PROPERTY =
                                                "datalift.cache.duration";
    /** The business day opening hours. */
    public final static String BUSINESS_DAY_PROPERTY =
                                                "datalift.cache.businessDay";

    private final static String MODULE_NAME = "RouterResource";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static FileFilter directoryFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return ((f.isDirectory()) && (f.canRead()));
            }
        };
    private final static FileFilter jarFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return ((f.isFile()) && (f.getName().endsWith(".jar")));
            }
        };

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Application modules. */
    private final Map<String,ModuleDesc> modules =
                                            new TreeMap<String,ModuleDesc>();
    /** Predefined SPARQL queries for DataLift core module. */
    private final SparqlQueries queries =
                                new SparqlQueries(DataliftApplication.class);

    /** The DataLift configuration. */
    private Configuration configuration = null;
    /** Cache management informations. */
    private int cacheDuration = 2 * 3600;           // 2 hours in seconds
    private int[] businessDay = { 8, 20 };          // 8 A.M. to 8 P.M.
    /** The DataLift SPARQL endpoint module. */
    private SparqlEndpoint sparqlEndpoint = null;

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
    public void init(Configuration config) {
        if (config == null) {
            throw new IllegalArgumentException("configuration");
        }
        this.configuration = config;

        // Step #1: Load configuration.
        // Cache: duration
        String s = config.getProperty(CACHE_DURATION_PROPERTY);
        if (! isBlank(s)) {
            try {
                this.cacheDuration = Integer.parseInt(s);
            }
            catch (Exception e) {
                log.warn("Invalid cache duration: {}. Using default: {}",
                                        s, Integer.valueOf(this.cacheDuration));
            }
        }
        // Cache: business day hours
        s = config.getProperty(BUSINESS_DAY_PROPERTY);
        if (! isBlank(s)) {
            if ("-".equals(s.trim())) {
                // Not business day hours specified.
                this.businessDay[0] = 0;
                this.businessDay[1] = 23;
            }
            else {
                String[] v = s.split("\\s*-\\s*", -1);
                try {
                    int openingHour = Integer.parseInt(v[0]);
                    int closingHour = Integer.parseInt(v[1]);
                    if ((openingHour < 0) || (openingHour > 23) ||
                        (closingHour < 0) || (closingHour > 23)) {
                        throw new IllegalArgumentException(BUSINESS_DAY_PROPERTY);
                    }
                    this.businessDay[0] = Math.min(openingHour, closingHour);
                    this.businessDay[1] = Math.max(openingHour, closingHour);
                }
                catch (Exception e) {
                    log.warn("Invalid business day hours: {}. Using default: {}", s,
                             "" + this.businessDay[0] + '-' + this.businessDay[1]);
                }
            }
        }

        // Step #2: Register this object in configuration.
        this.configuration.registerBean(this);

        // Step #3: Load available modules.
        this.modules.clear();
        // Load modules embedded in web application first (if any).
        this.loadModules(this.getClass().getClassLoader(), null);
        // Load third-party module bundles.
        if (config.getModulesPath() != null) {
            List<File> l = Arrays.asList(config.getModulesPath().listFiles(
                    new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return (jarFilter.accept(f) ||
                                    directoryFilter.accept(f));
                        }
                    }));
            Collections.sort(l);
            for (File m : l) {
                try {
                    this.loadModules(m);
                }
                catch (Exception e) {
                    log.fatal("Failed to load modules from {}: {}. Skipping...",
                              e, m.getName(), e.getMessage());
                    // Continue with next module.
                }
            }
        }
        // Step #4: Check whether SPARQL endpoint module is available.
        try {
            this.sparqlEndpoint = config.getBean(SparqlEndpoint.class);
        }
        catch (Exception e) {
            log.warn("No SPARQL endpoint module available");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown(Configuration configuration) {
        // Shutdown each module, ignoring errors.
        for (ModuleDesc desc : this.modules.values()) {
            Module m = desc.module;
            Object[] prevCtx = LogContext.pushContexts(m.getName(), "shutdown");
            try {
                m.shutdown(configuration);
            }
            catch (Exception e) {
                log.error("Failed to properly shutdown module {}: {}", e,
                          m.getName(), e.getMessage());
                // Continue with next module.
            }
            finally {
                LogContext.pushContexts(prevCtx[0], prevCtx[1]);
            }
        }
        LogContext.pushContexts(null, null);
    }

    //-------------------------------------------------------------------------
    // ResourceResolver contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Response resolveStaticResource(String path, Request request)
                                                throws WebApplicationException {
        return this.resolveStaticResource(
                                    this.configuration.getPublicStorage(),
                                    path, request);
    }

    /** {@inheritDoc} */
    @Override
    public Response resolveRdfResource(UriInfo uriInfo, Request request,
                                       String acceptHdr)
                                                throws WebApplicationException {
        if (uriInfo == null) {
            throw new IllegalArgumentException("uriInfo");
        }
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        Response response = null;

        URI uri = uriInfo.getAbsolutePath();
        // Check that target subject exists in published data store.
        Repository data = this.configuration.getDataRepository();
        String query = this.queries.get("checkExists");
        ExistsQueryResultHandler result = new ExistsQueryResultHandler();
        try {
            Map<String,Object> bindings = new HashMap<String,Object>();
            bindings.put("s", uri);
            data.select(query, bindings, result);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to execute query \""
                                       + query + "\" for \"" + uri + '"', e);
        }

        if (result.subject != null) {
            // Subject found in RDF store.
            // => Check whether data shall be returned.
            ResponseBuilder b = null;
            if (result.lastModified != null) {
                b = request.evaluatePreconditions(result.lastModified);
            }
            if (b == null) {
                // Forward request to SPARQL endpoint.
                log.trace("Resolved request as RDF resource {}", uri);
                b = this.sparqlEndpoint.executeQuery(
                                                "DESCRIBE <" + uri + '>',
                                                uriInfo, request, acceptHdr);
            }
            // Else: Client already has an up-to-date copy of the data.
            response = this.addCacheDirectives(b, result.lastModified).build();
        }
        // Else: No matching RDF resource.

        return response;
    }

    //-------------------------------------------------------------------------
    // Resource web services
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Forwards a web service call to the
     * module specified in the request path.
     * <p>
     * If the module is unknown or is not itself a
     * {@link Module#isResource() JAX-RS resource}, the methods
     * tries to resolve the call against a public local file or a RDF
     * resource in the public RDF store.</p>
     * @param  module      the target module, from the request path.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return the module as a JAX-RS sub-resource to which forward the
     *         request or a {@link Response service response} if the
     *         request was resolved as a local file or RDF resource.
     * @throws WebApplicationException complete with status code and
     *         plain-text error message if any error occurred while
     *         processing the service call or the request path can not
     *         be resolved.
     */
    @Path("{module}")
    public Object moduleForwarding(@PathParam("module") String module,
                                   @Context UriInfo uriInfo,
                                   @Context Request request,
                                   @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Object target = null;

        ModuleDesc m = this.modules.get(module);
        if ((m != null) && (m.isResource)) {
            // Matching module found.
            LogContext.setContexts(module, null);
            target = m.module;
            log.debug("Forwarding request on \"{}\" to module \"{}\"",
                                                    uriInfo.getPath(), m.name);
        }
        else {
            // Unknown module or direct module query not supported.
            // => Try resolving URL as a file or an RDF resource.
            target = this.resolveUnmappedResource(m, uriInfo,
                                                     request, acceptHdr);
        }
        return target;
    }

    /**
     * <i>[Resource method]</i> Forwards a web service call to the
     * module and resource specified in the request path, allocating
     * the resource instance.
     * <p>
     * If either the module or the resource name is unknown, the methods
     * tries to resolve the call against a public local file or a RDF
     * resource in the public RDF store.</p>
     * @param  module      the target module, from the request path.
     * @param  resource    the target resource, from the request path.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a JAX-RS sub-resources to which forward the request or
     *         a {@link Response service response} if the request was
     *         resolved as a local file or RDF resource.
     * @throws WebApplicationException complete with status code and
     *         plain-text error message if any error occurred while
     *         processing the service call or the request path can not
     *         be resolved.
     */
    @Path("{module}/{resource}")
    public Object resourceForwarding(@PathParam("module") String module,
                                     @PathParam("resource") String resource,
                                     @Context UriInfo uriInfo,
                                     @Context Request request,
                                     @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Object target = null;

        ModuleDesc m = this.modules.get(module);
        if (m != null) {
            Class<?> clazz = m.get(resource);
            if (clazz != null) {
                try {
                    // Look for a constructor with the module as argument.
                    try {
                        Constructor<?> c = clazz.getConstructor(
                                                        m.module.getClass());
                        target = c.newInstance(m.module);
                    }
                    catch (NoSuchMethodException e) {
                        // Constructor not found. => Use default constructor.
                        target = clazz.newInstance();
                    }
                    // Matching resource successfully created.
                    LogContext.setContexts(module, resource);
                }
                catch (Exception e) {
                    log.error("Failed to create resource of type {}", e, clazz);
                    throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .build());
                }
            }
            // Else: unknown resource for module
        }
        // Else: unknown module.

        if (target == null) {
            // No matching module or resource found.
            // => Try resolving URL as a file or an RDF resource.
            target = this.resolveUnmappedResource(m, uriInfo,
                                                     request, acceptHdr);
        }
        return target;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Resolved an unmapped web service request against the application
     * {@link Configuration#getLocalStorage() public local file storage}
     * or an RDF resource in the
     * {@link Configuration#getDataRepository() public data RDF store}.
     * @param  module      the descriptor of the resolved module if the
     *                     beginning of the request path matched the
     *                     name of one of the configured modules,
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
    private ResponseWrapper resolveUnmappedResource(ModuleDesc module,
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
        LogContext.setContexts(MODULE_NAME, path);
        log.trace("Resolving unmapped resource: {}", path);

        try {
            if (module != null) {
                // Path prefix was resolved as a module name.
                // => Try to resolve resource as a module static resource.
                String rsc = MODULE_PUBLIC_DIR
                                        + path.substring(module.name.length());
                URL src = module.classLoader.getResource(rsc);
                if (src != null) {
                    // Module static resource found.
                    // => Check whether data shall be returned.
                    Date lastModified = new Date((module.isJarPackage())?
                                    module.root.lastModified():
                                    new File(src.getFile()).lastModified());
                    ResponseBuilder b = request.evaluatePreconditions(lastModified);
                    if (b == null) {
                        // Get MIME type from file extension.
                        String mt = new MimetypesFileTypeMap().getContentType(rsc);
                        log.debug("Serving module public resource: {}/{} ({})",
                                        module, rsc, mt);
                        b = Response.ok(src.openStream(), mt);
                    }
                    response = this.addCacheDirectives(b, lastModified)
                                   .build();
                }
            }
            if (response == null) {
                // Not a module static resource.
                // => Try to match a file on local storage.
                response = this.resolveStaticResource(
                                        this.configuration.getPublicStorage(),
                                        path, request);
            }
            if ((response == null) && (this.sparqlEndpoint != null)) {
                // Not a public file. => Check triple store for RDF resource.
                response = resolveRdfResource(uriInfo, request, acceptHdr);
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
            throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
        }
        return (response != null)? new ResponseWrapper(response): null;
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
    private Response resolveStaticResource(File root, String path,
                                           Request request)
                                                throws WebApplicationException {
        if (root == null) {
            throw new IllegalArgumentException("root");
        }
        if (! isSet(path)) {
            throw new IllegalArgumentException("path");
        }
        Response response = null;

        File f = new File(root, path);
        if ((f != null) && (f.isFile()) && (f.canRead())) {
            // Path resolved as an existing file.
            // => Check path validity.
            if (! f.getAbsolutePath().startsWith(root.getAbsolutePath())) {
                // Oops! Forged path that point outside public file store.
                log.warn("Attempt to access file {} outside storage: {}",
                         f, root);
                throw new WebApplicationException(Status.FORBIDDEN);
            }
            // Check whether data shall be returned.
            Date lastModified = new Date(f.lastModified());
            ResponseBuilder b = request.evaluatePreconditions(lastModified);
            if (b == null) {
                // Get MIME type from file extension.
                String mt = new MimetypesFileTypeMap().getContentType(f);
                log.debug("Serving static resource: {} ({})", f, mt);
                b = Response.ok(f, mt);
            }
            response = this.addCacheDirectives(b, lastModified).build();
        }
        return response;
    }

    private ResponseBuilder addCacheDirectives(ResponseBuilder response,
                                               Date lastModified) {
        if (this.cacheDuration > 0) {
            // Compute cache expiry date/time.
            GregorianCalendar cal = new GregorianCalendar();
            int h = cal.get(HOUR_OF_DAY);
            if ((h <= this.businessDay[0]) || (h >= this.businessDay[1])) {
                // No data updates occur between close and opening of business. 
                // => Set expiry date to opening of business hour, ignoring
                //    minutes & seconds.
                if (h >= this.businessDay[1]) {
                    cal.add(DAY_OF_YEAR, 1);
                }
                cal.set(HOUR_OF_DAY, this.businessDay[0]);
                response = response.expires(cal.getTime());
            }
            else {
                // Else: cache entries for 2 hours.
                CacheControl cc = new CacheControl();
                cc.setMaxAge(this.cacheDuration);
                cc.setPrivate(false);
                cc.setNoTransform(false);
                response = response.cacheControl(cc);
            }
        }
        // Else: caching disabled.

        // Set last modified date, if available.
        if (lastModified != null) {
            response = response.lastModified(lastModified);
        }
        return response;
    }

    /**
     * Loads a DataLift module from the specified path and registers it.
     * @param  f   the directory or JAR file for the module.
     *
     * @throws TechnicalException if any error occurred while loading
     *         or configuring the module.
     */
    private void loadModules(File f) {
        log.info("Loading module(s) from: {}", f);
        try {
            this.loadModules(new URLClassLoader(this.getModulePaths(f),
                                        this.getClass().getClassLoader()), f);
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                                "module.load.error", e,
                                                f.getName(), e.getMessage());
            log.fatal(error.getMessage(), e);
            throw error;
        }
    }

    /**
     * Loads all {@link Module} implementation classes from the
     * specified JAR file or directory, initializing and registering
     * them as well as their Velocity templates.
     * @param  cl   the classloader to load the module classes.
     * @param  f    the module JAR file or directory.
     */
    private void loadModules(ClassLoader cl, File f) {
        for (Module m : ServiceLoader.load(Module.class, cl)) {
            String name = m.getName();
            // Initialize module.
            Object[] prevCtx = LogContext.pushContexts(name, "init");
            try {
                m.init(this.configuration);
            }
            finally {
                LogContext.pushContexts(prevCtx[0], prevCtx[1]);
            }
            // Register module root (directory or JAR file) as
            // a Velocity template source, if available.
            if (f != null) {
                VelocityTemplateProcessor.addModule(name, f);
            }
            // Make module available thru the Configuration object.
            this.configuration.registerBean(m);
            this.configuration.registerBean(name, m);
            // Publish module REST resources.
            ModuleDesc desc = new ModuleDesc(m, f, cl);
            modules.put(name, desc);
            log.info("Registered module {} ({} resource(s))", name,
                            Integer.valueOf(desc.ressourceClasses.size()));
        }
    }

    /**
     * Analyzes a module structure to match the expected elements and
     * returns the paths to be added to the module classpath.
     * <p>
     * Recognized elements include:</p>
     * <ul>
     *  <li>For directories:
     *   <dl>
     *    <dt><code>/classes</code></dt>
     *    <dd>The default directory for module classes</dd>
     *    <dt><code>/*.jar</code></dt>
     *    <dd>JAR files containing the module classes, if no
     *        <code>/classes</code> directory is present</dd>
     *    <dt><code>/lib/**&#47;*.jar</code></dt>
     *    <dd>All the JAR files in the <code>/lib</code> directory tree
     *        (module classes and third-party libraries)</dd>
     *    <dt><code>/</code></dt>
     *    <dd>The module root directory, if no classes directory nor
     *        any JAR file were found</dd>
     *   </dl></li>
     *  <li>For JAR files: the JAR file itself.</li>
     * </ul>
     * @param  path   the directory or JAR file for the module.
     *
     * @return the URLs of the paths to be added to the module
     *         classpath.
     */
    private URL[] getModulePaths(File path) {
        List<URL> urls = new LinkedList<URL>();

        if (path.isDirectory()) {
            // Look for module classes as a directory tree.
            File classesDir = new File(path, MODULE_CLASSES_DIR);
            if (classesDir.isDirectory()) {
                urls.add(this.getFileUrl(classesDir));
            }
            else {
                // No classes directory. => Look for root-level JAR files.
                for (File jar : path.listFiles(jarFilter)) {
                    urls.add(this.getFileUrl(jar));
                }
            }
            // Look for module dependencies as library JAR files.
            File libDir = new File(path, MODULE_LIB_DIR);
            if (classesDir.isDirectory()) {
                urls.addAll(this.findFiles(libDir, jarFilter));
            }
            if (urls.isEmpty()) {
                // No path matched.
                // => Add module root directory.
                urls.add(this.getFileUrl(path));
            }
        }
        else {
            // JAR file. => Add the JAR file itself to the classpath.
            urls.add(this.getFileUrl(path));
        }
        return urls.toArray(new URL[urls.size()]);
    }

    /**
     * Scans a directory tree and returns the files matching the
     * specified filter.
     * @param  root     the root of directory tree.
     * @param  filter   the file filer.
     *
     * @return the URLs of the matched files.
     */
    private Collection<URL> findFiles(File root, FileFilter filter) {
        return this.findFiles(root, filter, new LinkedList<URL>());
    }

    /**
     * Recursively scans a directory tree and returns the files
     * matching the specified filter.
     * @param  root      the root of directory tree.
     * @param  filter    the file filer.
     * @param  results   the collection to append the matched files to.
     *
     * @return the <code>results</code> collection updated with the
     *         matched files.
     */
    private Collection<URL> findFiles(File root, FileFilter filter,
                                                 Collection<URL> results) {
        List<File> dirs = new LinkedList<File>();
        // Scan first-level directory content.
        for (File f : root.listFiles()) {
            if (filter.accept(f)) {
                results.add(this.getFileUrl(f));
            }
            else if ((f.isDirectory()) && (f.canRead())) {
                // Child directory not handled by filter.
                // => Mark for recursive scan.
                dirs.add(f);
            }
            // Else: ignore...
        }
        // Recursively scan child directories.
        for (File child : dirs) {
            this.findFiles(child, filter, results);
        }
        return results;
    }

    /**
     * Returns the URL of the specified file, compliant with the
     * requirements of {@link URLClassLoader#URLClassLoader(URL[])}.
     * @param  f   the file or directory to convert.
     *
     * @return the URL of the file.
     * @throws TechnicalException if <code>f</code> if neither a
     *         regular file nor a directory.
     */
    private URL getFileUrl(File f) {
        URL u = null;
        try {
            if (f.isFile()) {
                u = f.toURI().toURL();
            }
            else if (f.isDirectory()) {
                String uri = f.toURI().toString();
                if (! uri.endsWith("/")) {
                    uri += "/";
                }
                u = new URL(uri);
            }
            else {
                throw new TechnicalException("invalid.file.type", f.getPath());
            }
        }
        catch (MalformedURLException e) {
            // Should never happen...
            throw new UnsupportedOperationException(e.getMessage(), e);
        }
        log.debug("Added resource \"{}\" to module classpath", u);
        return u;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                        + " (" + this.modules.size()
                        + " modules: " + this.modules.keySet() + ')';
    }

    //-------------------------------------------------------------------------
    // ModuleDesc nested class
    //-------------------------------------------------------------------------

    /**
     * The descriptor for a registered module that acts as a cache for
     * module-provided data.
     */
    private final static class ModuleDesc
    {
        public final String name;
        public final Module module;
        public final File root;
        public final ClassLoader classLoader;
        public final Map<String,Class<?>> ressourceClasses;
        public final boolean isResource;

        public ModuleDesc(Module module, File root, ClassLoader classLoader) {
            if (module == null) {
                throw new IllegalArgumentException("module");
            }
            if ((root == null) || (! root.exists())) {
                throw new IllegalArgumentException("root");
            }
            if (classLoader == null) {
                throw new IllegalArgumentException("classLoader");
            }
            this.module      = module;
            this.name        = module.getName();
            this.root        = root;
            this.classLoader = classLoader;
            this.ressourceClasses = new TreeMap<String,Class<?>>();
            Map<String,Class<?>> resources = module.getResources();
            if (resources != null) {
                this.ressourceClasses.putAll(resources);
            }
            this.isResource = module.isResource();
        }

        public Class<?> get(String key) {
            return this.ressourceClasses.get(key);
        }

        public boolean isJarPackage() {
            return this.root.isFile();
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    //-------------------------------------------------------------------------
    // ResponseWrapper nested class
    //-------------------------------------------------------------------------

    /**
     * A catch-all JAX-RS sub-resource that accepts all HTTP GET
     * requests and returns a prepared response.
     */
    public final static class ResponseWrapper
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

    private static class ExistsQueryResultHandler
                                        extends TupleQueryResultHandlerBase {
        public String subject = null;
        public Date   lastModified = null;

        public ExistsQueryResultHandler() {
            super();
        }

        @Override
        public void handleSolution(BindingSet b) {
            if (this.subject == null) {
                // Store information for the first matched entry.
                this.subject = this.getString(b, "s");
                this.lastModified = this.getDate(b, "lastModified");
            }
            // Else: Ignore subsequent entries.
        }

        private String getString(BindingSet b, String name) {
            Binding v = b.getBinding(name);
            return (v != null)? v.getValue().stringValue(): null;
        }

        private Date getDate(BindingSet b, String name) {
            Binding v = b.getBinding(name);
            try {
                // Try to extract date.
                return ((Literal)(v.getValue())).calendarValue()
                                                .toGregorianCalendar()
                                                .getTime();
            }
            catch (Exception e) {
                return null;            // No date here...
            }
        }
    }
}
