package org.datalift.core.velocity.jersey;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.spi.template.ViewProcessor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.FieldTool;

import static org.apache.velocity.app.VelocityEngine.*;
import static org.apache.velocity.runtime.log.Log4JLogChute.*;

import org.datalift.core.velocity.i18n.I18nDirective;
import org.datalift.core.velocity.i18n.LoadDirective;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.security.SecurityContext;

import static org.datalift.fwk.util.StringUtils.join;


/**
 * A Jersey template processor relying on Apache
 * <a href="http://velocity.apache.org/engine/">Velocity</a> templating
 * engine.
 *
 * @author lbihanic
 */
@Provider
public class VelocityTemplateProcessor implements ViewProcessor<Template>
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The default context key for the application model object. */
    public final static String CTX_MODEL            = "it";
    /** The context key for Velocity Escape tool. */
    public final static String CTX_ESCAPE_TOOL      = "esc";
    /** The context key for Velocity Date tool. */
    public final static String CTX_DATE_TOOL        = "date";
    /** The context key for the HTTP servlet request object. */
    public final static String CTX_HTTP_REQUEST     = "request";
    /** The context key for the HTTP servlet response object. */
    public final static String CTX_HTTP_RESPONSE    = "response";
    /** The context key for JAX-RS {@link UriInfo} object. */
    public final static String CTX_URI_INFO         = "uriInfo";
    /** The context key for the application base URI (a.k.a. context path). */
    public final static String CTX_BASE_URI         = "baseUri";
    /** The context key for DataLift {@link SecurityContext}. */
    public final static String CTX_SECURITY_CONTEXT = "securityCtx";

    /** The init-param defining the Velocity template search path. */
    public final static String TEMPLATES_BASE_PATH =
                                        "velocity.templates.path";
    /** The init-param defining the default encoding for Velocity template. */
    public final static String TEMPLATES_ENCODING =
                                        "velocity.templates.encoding";
    /** The init-param defining the template cache duration. */
    public final static String TEMPLATES_CACHE_DURATION =
                                        "velocity.templates.update.check";
    /** The default file extension for Velocity templates. */
    public final static String TEMPLATES_DEFAULT_EXTENSION = ".vm";

    protected final static String LOADER_CLASS        = "class";
    protected final static String LOADER_DESC         = "description";
    protected final static String LOADER_CACHE        = "cache";
    protected final static String LOADER_PATH         = "path";
    protected final static String LOADER_BOM_CHECK    = "unicode";
    protected final static String LOADER_UPD_INTERVAL =
                                                "modificationCheckInterval";
    protected final static String USER_DIRECTIVES     = "userdirective";

    private final static String FILE_LOADER         = "file";
    private final static String CLASSPATH_LOADER    = "class";
    private final static String MODULE_LOADER       = "module";

    private final static String DEFAULT_VELOCITY_CONFIG = "velocity.properties";
    private final static String VELOCITY_LOG4J_LOGGER   = "org.apache.velocity";

    private final static String LOADER_PROPS_PREFIX = ".resource.loader.";

    private final static String CONFIG_ELTS_SEPARATOR = ", ";
    
    private final static String FIELD_TOOL = "field";
    
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Map<String,File> modulePaths  =
                                                new TreeMap<String,File>();

    /** Velocity template engine instance, one per web application. */
    private static VelocityEngine engine = null;

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /**
     * <i>[Dependency injection]</i> Context of the HTTP request
     * being processed.
     */
    private @Context HttpContext httpContext;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new view processor based on the Velocity template
     * engine.
     * @param  ctx   the servlet context of the web application.
     *
     * @throws RuntimeException if any error occurred while initializing
     *         the Velocity engine.
     */
    public VelocityTemplateProcessor(@Context ServletContext ctx) {
        super();

        if (engine == null) {
            // Initialize Velocity template engine.
            init(ctx);
        }
    }

    //-------------------------------------------------------------------------
    // ViewProcessor contract support
    //-------------------------------------------------------------------------

    /**
     * Resolve a template name to a Velocity <code>Template</code>
     * object.
     *
     * @param  name   the template name.
     *
     * @return the resolved Velocity template object, or
     *         <code>null</code> if the template name can not be
     *         resolved.
     */
    @Override
    public Template resolve(String name) {
        Template template = null;

        try {
            if (! name.endsWith(TEMPLATES_DEFAULT_EXTENSION)) {
                name += TEMPLATES_DEFAULT_EXTENSION;
            }
            template = engine.getTemplate(name);
        }
        catch (ResourceNotFoundException e) {
            // Not found. => Return null.
            log.debug("Failed to resolve template \"{}\"", name);
        }
        catch (Exception e) {
            // A .vm file was found but could not be parsed.
            // => Notify error but return null in case another
            //    ViewProcessor can handle it.
            log.error("Error loading template \"{}\": {}", e,
                                                        name, e.getMessage());
        }
        return template;
    }

    /**
     * Process a template and write the result to an output stream.
     *
     * @param  t          the Velocity template, obtained by calling the
     *                    {@link #resolve(java.lang.String)} method with
     *                    a template name.
     * @param  viewable   the viewable that contains the model to be
     *                    passed to the template.
     * @param  out        the output stream to write the result of
     *                    processing the template.
     *
     * @throws IOException if there was an error processing the template.
     */
    @Override
    public void writeTo(Template t, Viewable viewable,
                                    OutputStream out) throws IOException {
        // Commit the status and headers to the HttpServletResponse
        out.flush();

        try {
            // Populate Velocity context from model data.
            VelocityContext ctx = new VelocityContext();
            Object m = viewable.getModel();
            if (m instanceof Map<?,?>) {
                // Copy all map entries with a string as key.
                Map<?,?> map = (Map<?,?>)m;
                for (Map.Entry<?,?> e : map.entrySet()) {
                    if (e.getKey() instanceof String) {
                        ctx.put((String)(e.getKey()), e.getValue());
                    }
                    // Else: ignore entry.
                }
            }
            else {
                // Single object model (may be null).
                ctx.put(CTX_MODEL, m);
            }
            // Add Velocity string escaping tool.
            if (ctx.get(CTX_ESCAPE_TOOL) == null) {
                ctx.put(CTX_ESCAPE_TOOL, new EscapeTool());
            }
            // Add Velocity date tool.
            if (ctx.get(CTX_DATE_TOOL) == null) {
                Map<String, Object> config = new HashMap<String, Object>();

                List<Locale> l = this.httpContext.getRequest().getAcceptableLanguages();
                if ((l != null) && (! l.isEmpty())) {
                    config.put(ToolContext.LOCALE_KEY, l.get(0));
                }
                DateTool dateTool = new DateTool();
                dateTool.configure(config);
                ctx.put(CTX_DATE_TOOL, dateTool);
            }
            // Add predefined variables, the JSP way.
            if (ctx.get(CTX_HTTP_REQUEST) == null) {
                ctx.put(CTX_HTTP_REQUEST, this.httpContext.getRequest());
            }
            if (ctx.get(CTX_HTTP_RESPONSE) == null) {
                ctx.put(CTX_HTTP_RESPONSE, this.httpContext.getResponse());
            }
            UriInfo uriInfo = this.httpContext.getUriInfo();
            if (ctx.get(CTX_URI_INFO) == null) {
                ctx.put(CTX_URI_INFO, uriInfo);
            }
            if (ctx.get(CTX_BASE_URI) == null) {
                String baseUri = uriInfo.getBaseUri().toString();
                if (baseUri.endsWith("/")) {
                    baseUri = baseUri.substring(0, baseUri.length() - 1);
                }
                ctx.put(CTX_BASE_URI, baseUri);
            }
            if (ctx.get(CTX_SECURITY_CONTEXT) == null) {
                ctx.put(CTX_SECURITY_CONTEXT, SecurityContext.getContext());
            }
            if (ctx.get(FIELD_TOOL) == null) {
                // TODO: TypeSource???
            	ctx.put(FIELD_TOOL, new FieldTool().in(org.datalift.fwk.project.Source.TypeSource.class));
            }
            // Apply Velocity template, using encoding from in HTTP request.
            Writer w = new OutputStreamWriter(out, this.getCharset());
            t.merge(ctx, w);
            w.flush();
        }
        catch (Exception e) {
            log.error("Error merging template \"{}\": {}", e,
                                                t.getName(), e.getMessage());
            throw new IOException(e);
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns the preferred character set for the being processed HTTP
     * request (accessed through Jersey's HttpContext).
     *
     * @return the preferred character set name or <code>UTF-8</code>
     *         if no character set information can be retrieved.
     */
    private String getCharset() {
        MediaType m = this.httpContext.getRequest().getMediaType();
        String name = (m == null)? null: m.getParameters().get("charset");
        return (name == null)? "UTF-8": name;
    }

    /**
     * Add a new template source.
     * @param  path   a JAR file or directory to be scanned when looking
     *                for templates.
     *
     * @throws IllegalArgumentException if <code>path</code> does
     *         not exist, is not a file or cannot be read.
     */
    public static void addModule(String name, File path) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("name");
        }
        if (path == null) {
            throw new IllegalArgumentException("path");
        }
        else if (! path.canRead()) {
            throw new IllegalArgumentException(
                                new FileNotFoundException(path.getPath()));
        }
        modulePaths.put(name, path);
    }

    /**
     * Initializes a new Velocity template engine instance.
     * @param  ctx   the application context to get the configuration
     *               data from.
     *
     * @throws RuntimeException if any error occurred while initializing
     *         the Velocity engine.
     */
    private static void init(ServletContext ctx) {
        try {
            Properties config = null;
            // Load Velocity default configuration (if found in classpath).
            InputStream in = VelocityTemplateProcessor.class
                                .getClassLoader()
                                .getResourceAsStream(DEFAULT_VELOCITY_CONFIG);
            if (in != null) {
                // Load default Velocity configuration.
                Properties defaults = new Properties();
                defaults.load(in);
                config = new Properties(defaults);
            }
            else {
                // No default configuration found.
                config = new Properties();
            }
            // Configure logging to Log4J "org.apache.velocity" logger.
            config.setProperty(RUNTIME_LOG_LOGSYSTEM_CLASS,
                               Log4JLogChute.class.getName());
            config.setProperty(RUNTIME_LOG_LOG4J_LOGGER,
                               VELOCITY_LOG4J_LOGGER);

            // Check for template cache activation.
            long updateInterval = 0L;
            try {
                String s = ctx.getInitParameter(TEMPLATES_CACHE_DURATION);
                if (s != null) {
                    updateInterval = Long.parseLong(s);
                    if (log.isDebugEnabled()) {
                        if (updateInterval < 0L) {
                            log.debug("Disabling template file cache");
                        }
                        else if (updateInterval > 0L) {
                            log.debug("Template update check interval: {} seconds",
                                      Long.valueOf(updateInterval));
                        }
                    }
                }
            }
            catch (Exception e) { /* Ignore... */ }

            // Build the list of template loaders.
            List<String> loaders = new LinkedList<String>();
            // Configure file template loader.
            List<String> fileSources = new LinkedList<String>();
            // Check for configured template path is specified.
            String path = getRealPath(ctx,
                                    ctx.getInitParameter(TEMPLATES_BASE_PATH));
            if (path != null) {
                fileSources.add(path);
            }
            // Add all registered template root paths.
            if (! fileSources.isEmpty()) {
                // File resource loader is needed.
                String paths = join(fileSources, CONFIG_ELTS_SEPARATOR);
                log.debug("Using file template loader for paths: {}", paths);
                loaders.add(FILE_LOADER);
                config.setProperty(getPropName(FILE_LOADER, LOADER_PATH), paths);
                // Force Unicode BOM detection in template files.
                config.setProperty(getPropName(FILE_LOADER, LOADER_BOM_CHECK),
                                   Boolean.TRUE.toString());
                // Configure template cache activation.
                if (updateInterval > 0L) {
                    config.setProperty(
                            getPropName(FILE_LOADER, LOADER_UPD_INTERVAL),
                            String.valueOf(updateInterval));
                }
                config.setProperty(getPropName(FILE_LOADER, LOADER_CACHE),
                            Boolean.valueOf(updateInterval >= 0L).toString());
            }
            // Configure module template loader.
            if (! modulePaths.isEmpty()) {
                // Module resource loader is needed.
                StringBuilder buf = new StringBuilder();
                for (Entry<String,File> e : modulePaths.entrySet()) {
                    buf.append(e.getKey()).append(':')
                       .append(e.getValue().getAbsolutePath())
                       .append(CONFIG_ELTS_SEPARATOR);
                }
                // Remove last separator
                buf.setLength(buf.length() - CONFIG_ELTS_SEPARATOR.length());
                String modules = buf.toString();

                log.debug("Using module template loader for {}", modules);
                loaders.add(MODULE_LOADER);
                config.setProperty(getPropName(MODULE_LOADER, LOADER_CLASS),
                                   ModuleResourceLoader.class.getName());
                config.setProperty(getPropName(MODULE_LOADER, LOADER_DESC),
                                   "DataLift Module Resource Loader");
                config.setProperty(getPropName(MODULE_LOADER, LOADER_PATH),
                                   modules);
                // Configure template cache activation.
                if (updateInterval > 0L) {
                    config.setProperty(
                            getPropName(MODULE_LOADER, LOADER_UPD_INTERVAL),
                            String.valueOf(updateInterval));
                }
                config.setProperty(getPropName(MODULE_LOADER, LOADER_CACHE),
                            Boolean.valueOf(updateInterval >= 0L).toString());
            }
            // Configure classpath template loader.
            config.setProperty(getPropName(CLASSPATH_LOADER, LOADER_CLASS),
                               ClasspathResourceLoader.class.getName());
            config.setProperty(getPropName(CLASSPATH_LOADER, LOADER_DESC),
                               "Velocity Classpath Resource Loader");
            loaders.add(CLASSPATH_LOADER);
            // Registered configured loaders.
            log.debug("Configured template loaders: {}", loaders);
            config.setProperty(RESOURCE_LOADER,
                               join(loaders, CONFIG_ELTS_SEPARATOR));

            // Configure template encoding, if specified.
            String encoding = ctx.getInitParameter(TEMPLATES_ENCODING);
            if (encoding != null) {
                config.setProperty(INPUT_ENCODING, encoding);
            }
            // Configure custom Directives for Velocity
            config.setProperty(USER_DIRECTIVES,
                                    LoadDirective.class.getName() + ',' +
                                    I18nDirective.class.getName());

            // Start a new Velocity engine.
            log.trace("Starting Velocity with configuration: {}", config);
            engine = new VelocityEngine(config);
            engine.init();
        }
        catch (Exception e) {
            throw new RuntimeException(
                                    "Failed to initialize Velocity engine", e);
        }
    }

    /**
     * Returns the real paths of the given template paths within the
     * web application, as provided by the servlet container.
     *
     * @param  ctx     the servlet context of the web application.
     * @param  paths   the specified template paths as a comma-separated
     *                 list of paths.
     * @return the corresponding real template paths or
     *         <code>null</code> if no path could be resolved.

     * Convert the specified template paths into real paths by resolving
     * them through the web application container
     */
    private static String getRealPath(ServletContext ctx, String paths) {
        String resolvedPath = null;

        if (paths != null) {
            StringBuilder buf = new StringBuilder();
            for (String path : paths.split("\\s*,\\s*")) {
                if (path.length() != 0) {
                    // Try resolving relative path through webapp context.
                    String s = ctx.getRealPath(path);
                    if ((s != null) && (new File(s).canRead())) {
                        path = s;
                    }
                }
                else {
                    path = ".";
                }
                if ((path != null) && (new File(path).canRead())) {
                    log.debug("Resolved template file path: {}", path);
                    buf.append(path).append(", ");
                }
                else {
                    log.warn("Failed to resolved template file path: {}", path);
                }
            }
            if (buf.length() != 0) {
                buf.setLength(buf.length() - 2);
                resolvedPath = buf.toString();
            }
            // Else: no path could be resolved.
        }
        return resolvedPath;
    }

    private static String getPropName(String loader, String prop) {
        return loader + LOADER_PROPS_PREFIX + prop;
    }
}
