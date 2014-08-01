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

package org.datalift.core.velocity.jersey;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.Set;
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
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.FieldTool;
import org.apache.velocity.tools.generic.LinkTool;
import org.apache.velocity.tools.view.WebappResourceLoader;

import static org.apache.velocity.app.VelocityEngine.*;
import static org.apache.velocity.runtime.log.Log4JLogChute.*;

import org.datalift.core.velocity.i18n.I18nDirective;
import org.datalift.core.velocity.i18n.I18nTool;
import org.datalift.core.velocity.i18n.LoadDirective;
import org.datalift.core.velocity.sparql.SparqlTool;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.view.TemplateModel;

import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.util.web.Charsets.UTF8_CHARSET;


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
    public final static String CTX_MODEL            = TemplateModel.MODEL_KEY;
    /** The context key for internationalization tool. */
    public final static String CTX_I18N_TOOL        = I18nTool.KEY;
    /** The context key for Velocity Escape tool. */
    public final static String CTX_ESCAPE_TOOL      = EscapeTool.DEFAULT_KEY;
    /** The context key for Velocity Link tool. */
    public final static String CTX_LINK_TOOL        = "link";
    /** The context key for Velocity Date tool. */
    public final static String CTX_DATE_TOOL        = "date";
    /** The context key for Velocity Field tool. */
    public final static String CTX_FIELD_TOOL       = "field";
    /** The context key for Datalift SPARQL tool. */
    public final static String CTX_SPARQL_TOOL      = "sparql";

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
    /** The default path for Velocity templates in webapp. */
    public final static String TEMPLATES_WEBAPP_DEFAULT_PATH =
                                                        "/WEB-INF/templates";

    protected final static String LOADER_CLASS        = "class";
    protected final static String LOADER_DESC         = "description";
    protected final static String LOADER_CACHE        = "cache";
    protected final static String LOADER_PATH         = "path";
    protected final static String LOADER_BOM_CHECK    = "unicode";
    protected final static String LOADER_UPD_INTERVAL =
                                                "modificationCheckInterval";
    protected final static String USER_DIRECTIVES     = "userdirective";

    private final static String WEBAPP_LOADER       = "webapp";
    private final static String CLASSPATH_LOADER    = "class";
    private final static String MODULE_LOADER       = "module";

    private final static String DEFAULT_VELOCITY_CONFIG = "velocity.properties";
    private final static String VELOCITY_LOG4J_LOGGER   = "org.apache.velocity";

    private final static String LOADER_PROPS_PREFIX = ".resource.loader.";

    private final static String CONFIG_ELTS_SEPARATOR = ", ";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** User provided directive classes. */
    private final static Set<Class<? extends Directive>> directives  =
                                    new HashSet<Class<? extends Directive>>();
    /** Locations of Datalift module templates (directories or JAR file). */
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
            log.error("Error loading template \"{}\": {}", e,
                                                        name, e.getMessage());
            throw new RuntimeException(e);
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
            FieldTool fieldTool = new FieldTool();
            Map<String,Object> ctx = this.buildContext(viewable.getModel(),
                                                       fieldTool);
            log.trace("Merging template {} with context {}", t.getName(), ctx);
            // Add predefined variable for base URI.
            UriInfo uriInfo = this.httpContext.getUriInfo();
            if (uriInfo != null) {
                if (! ctx.containsKey(CTX_BASE_URI)) {
                    String baseUri = uriInfo.getBaseUri().toString();
                    if (baseUri.endsWith("/")) {
                        baseUri = baseUri.substring(0, baseUri.length() - 1);
                    }
                    ctx.put(CTX_BASE_URI, baseUri);
                }
                if (! ctx.containsKey(CTX_URI_INFO)) {
                    ctx.put(CTX_URI_INFO, uriInfo);
                }
            }
            // Add predefined variables, the JSP way.
            if (this.httpContext != null) {
                if (! ctx.containsKey(CTX_HTTP_REQUEST)) {
                    ctx.put(CTX_HTTP_REQUEST, this.httpContext.getRequest());
                }
                if (! ctx.containsKey(CTX_HTTP_RESPONSE)) {
                    ctx.put(CTX_HTTP_RESPONSE, this.httpContext.getResponse());
                }
            }
            if (! ctx.containsKey(CTX_SECURITY_CONTEXT)) {
                ctx.put(CTX_SECURITY_CONTEXT, SecurityContext.getContext());
            }
            // Add internationalization tool.
            ctx.put(CTX_I18N_TOOL, new I18nTool());
            // Add Velocity tools: escaping, date, link, field, sparql...
            ctx.put(CTX_ESCAPE_TOOL, new EscapeTool());
            ctx.put(CTX_LINK_TOOL, new LinkTool());
            if (! ctx.containsKey(CTX_DATE_TOOL)) {
                DateTool dateTool = new DateTool();
                // Format dates according to the user's preferred locale.
                Map<String, Object> config = new HashMap<String, Object>();
                config.put(ToolContext.LOCALE_KEY,
                                                PreferredLocales.get().get(0));
                dateTool.configure(config);
                ctx.put(CTX_DATE_TOOL, dateTool);
            }
            if (! ctx.containsKey(CTX_FIELD_TOOL)) {
                ctx.put(CTX_FIELD_TOOL, fieldTool);
            }
            if (! ctx.containsKey(CTX_SPARQL_TOOL)) {
                ctx.put(CTX_SPARQL_TOOL, new SparqlTool());
            }
            VelocityContext context = new VelocityContext(ctx);
            // Prepare writing into HTTP response.
            out.flush();
            Writer w = new OutputStreamWriter(out, this.getCharset());
            // Execute a two-pass rendering to support layouts.
            // 1st pass: render page template.
            StringWriter buf = new StringWriter(4096);
            t.merge(context, buf);
            String screen = buf.toString();
            // Check for layout, after merging the screen template so the
            // template can overrule any layout set in the model.
            final Object layout = context.get(TemplateModel.LAYOUT_KEY);
            if (layout != null) {
                String srcTemplateName = t.getName();
                t = resolve(layout.toString());
                context.put(TemplateModel.SCREEN_CONTENT_KEY, screen);
                t.merge(context, w);
                log.trace("Completed rendering template {} with layout {}",
                          srcTemplateName, t.getName());
            }
            else {
                w.write(screen);
                log.trace("Completed rendering template {}", t.getName());
            }
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
        MediaType m = this.httpContext.getResponse().getMediaType();
        String cs = (m == null)? null: m.getParameters().get("charset");
        return (cs == null)? UTF8_CHARSET: cs;
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
     * Register a Velocity language supplementary directive.
     * @param  directive   the directive to register.
     */
    public static void addDirective(Class<? extends Directive> directive) {
        if (directive == null) {
            throw new IllegalArgumentException("directive");
        }
        directives.add(directive);
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
            // Add default template path for web application.
            fileSources.add(getAbsolutePath(TEMPLATES_WEBAPP_DEFAULT_PATH));
            // Check for configured template path is specified.
            String path = ctx.getInitParameter(TEMPLATES_BASE_PATH);
            if (! isBlank(path)) {
                fileSources.add(getAbsolutePath(path));
            }
            // Add all registered template root paths.
            if (! fileSources.isEmpty()) {
                // File resource loader is needed.
                String paths = join(fileSources, CONFIG_ELTS_SEPARATOR);
                log.debug("Using webapp template loader for paths: {}", paths);
                loaders.add(WEBAPP_LOADER);
                config.setProperty(getPropName(WEBAPP_LOADER, LOADER_CLASS),
                                   WebappResourceLoader.class.getName());
                config.setProperty(getPropName(WEBAPP_LOADER, LOADER_PATH),
                                   paths);
                // Force Unicode BOM detection in template files.
                config.setProperty(getPropName(WEBAPP_LOADER, LOADER_BOM_CHECK),
                                   Boolean.TRUE.toString());
                // Configure template cache activation.
                if (updateInterval > 0L) {
                    config.setProperty(
                            getPropName(WEBAPP_LOADER, LOADER_UPD_INTERVAL),
                            String.valueOf(updateInterval));
                }
                config.setProperty(getPropName(WEBAPP_LOADER, LOADER_CACHE),
                                        String.valueOf(updateInterval >= 0L));
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
                                        String.valueOf(updateInterval >= 0L));
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
            config.setProperty(INPUT_ENCODING,
                                    isBlank(encoding)? UTF8_CHARSET: encoding);
            // Configure custom Directives for Velocity.
            registerCoreDirectives();
            if (! directives.isEmpty()) {
                config.setProperty(USER_DIRECTIVES,
                                   join(directives, ",").replace("class ", ""));
            }
            // Start a new Velocity engine.
            log.trace("Starting Velocity with configuration: {}", config);
            engine = new VelocityEngine(config);
            if (ctx != null) {
                // Set web app. context, required to resolve embedded templates.
                engine.setApplicationAttribute(
                                        ServletContext.class.getName(), ctx);
            }
            engine.init();
        }
        catch (Exception e) {
            throw new RuntimeException(
                                    "Failed to initialize Velocity engine", e);
        }
    }

    /**
     * Builds the Velocity context.
     * @param  model    the application-provided model.
     * @param  fields   the Velocity Field tool for accessing class
     *                  members from the templates.
     * @return a populated Velocity context.
     */
    private Map<String,Object> buildContext(Object model, FieldTool fields) {
        Map<String,Object> ctx = new HashMap<String,Object>();
        if (model instanceof Map<?,?>) {
            // Copy all map entries with a string as key.
            Map<?,?> map = (Map<?,?>)model;
            for (Map.Entry<?,?> e : map.entrySet()) {
                if (e.getKey() instanceof String) {
                    String key = (String)(e.getKey());

                    // Check for field classes.
                    if ((TemplateModel.FIELD_CLASSES_KEY.equals(key)) &&
                        (e.getValue() instanceof Collection<?>)) {
                        // Register each class to Velocity Field tool.
                        for (Object o : (Collection<?>)(e.getValue())) {
                            if (o instanceof Class<?>) {
                                fields.in((Class<?>)o);
                            }
                        }
                        // Else: ignore entry.
                    }
                    else {
                        // Regular entry. => Add to the model.
                        ctx.put((String)(e.getKey()), e.getValue());
                    }
                }
                // Else: ignore entry.
            }
        }
        else {
            // Single object model (may be null).
            ctx.put(CTX_MODEL, model);
        }
        return ctx;
    }

    /**
     * Returns the absolute path corresponding to the specified path.
     * This method simply ensures that the provided path starts with
     * the '/' character.
     * @param  path   the path to make absolute.
     *
     * @return the absolute path.
     */
    private static String getAbsolutePath(String path) {
        return isSet(path)? ((path.charAt(0) == '/')? path: "/" + path): "/";
    }

    /**
     * Returns the fully-qualified property name for the specified
     * Velocity {@link ResourceLoader}.
     * @param  loader   the name of the resource loader in Velocity
     *                  configuration.
     * @param  prop     the (relative) property name.
     *
     * @return the fully-qualified property name, prefixed with the
     *         resource loader name followed by the
     *         {@link #LOADER_PROPS_PREFIX resource loader marker}.
     */
    private static String getPropName(String loader, String prop) {
        return loader + LOADER_PROPS_PREFIX + prop;
    }

    /**
     * Registers the Velocity directives provided by Datalift Core.
     */
    private static void registerCoreDirectives() {
        addDirective(LoadDirective.class);
        addDirective(I18nDirective.class);
    }
}
