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
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

import javax.ws.rs.Path;

import org.datalift.core.log.LogContext;
import org.datalift.core.project.DefaultProjectManager;
import org.datalift.core.velocity.jersey.VelocityTemplateProcessor;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.Module;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.log.web.LogServletContextListener;

import static org.datalift.core.DefaultConfiguration.DATALIFT_HOME;


/**
 * A Servlet context listener to initialize and shutdown the DataLift
 * application.
 *
 * @author lbihanic
 */
public class ApplicationLoader extends LogServletContextListener
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /**
     * The module sub-directory where to look for classes first. If
     * no present, root-level JAR files will be searched.
     */
    public final static String MODULE_CLASSES_DIR = "classes";
    /**
     * The module sub-directory where to look for third-party JAR files.
     * Regardless the presence of this directory, root-level JARs will
     * be loaded.
     */
    public final static String MODULE_LIB_DIR     = "lib";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** A FileFilter to select directories. */
    private final static FileFilter directoryFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return ((f.isDirectory()) && (f.canRead()));
            }
        };
    /** A FileFilter to select JAR files. */
    private final static FileFilter jarFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return ((f.isFile()) && (f.getName().endsWith(".jar")));
            }
        };

    /**
     * The singleton instance of this class for each deployment of the
     * application, initialized by the application server.
     */
    private static ApplicationLoader defaultLoader = null;

    /**
     * Log initialization shall be delayed until the log configuration
     * has been loaded.
     */
    private static Logger log = null;

    //------------------------------------------------------------------------
    // Instance members
    //------------------------------------------------------------------------

    /** DataLift Core components. */
    private final Set<LifeCycle> components = new HashSet<LifeCycle>();
    /** Application modules. */
    private final Map<String,ModuleDesc> modules =
                                            new TreeMap<String,ModuleDesc>();
    /** The singleton resources managed by this JAX-RS application. */
    private Set<Object> resources = null;

    //------------------------------------------------------------------------
    // LogServletContextListener contract support
    //------------------------------------------------------------------------

    /**
     * Initializes the DataLift application.
     */
    @Override
    public void init(Properties props) {
        // Initialize system properties from env. variables if need be.
        this.setupEnvironment();
        // Initialize log service.
        super.init(props);
        // Initialize DataLift application.
        this.initApplication(props);
        // Install this instance as default loader.
        defaultLoader = this;
    }

    /**
     * Shuts down the DataLift application.
     */
    @Override
    public void shutdown() {
        this.shutdownApplication();
    }

    //------------------------------------------------------------------------
    // Property accessors
    //------------------------------------------------------------------------

    /**
     * Returns the JAX-RS root resource singleton objects to be
     * registered at startup.
     * @return the JAX-RS root resources.
     */
    public Set<Object> getResources() {
        if (this.resources == null) {
            Set<Object> rsc = new HashSet<Object>();
            // Check modules for registration as JAX-RS root resources.
            for (ModuleDesc desc : this.modules.values()) {
                Module m = desc.module;
                if (m.getClass().isAnnotationPresent(Path.class)) {
                    // JAX-RS annotation @Path found on module class.
                    rsc.add(m);
                }
            }
            for (LifeCycle l : this.components) {
                if (l.getClass().isAnnotationPresent(Path.class)) {
                    rsc.add(l);
                }
            }
            this.resources = rsc;
        }
        return this.resources;
    }

    //-------------------------------------------------------------------------
    // Singleton access method
    //-------------------------------------------------------------------------

    /**
     * Returns the current DataLift
     * {@link ApplicationLoader application loader}.
     * @return the current application loader.
     */
    public static ApplicationLoader getDefault() {
        return defaultLoader;
    }

    //------------------------------------------------------------------------
    // Specific implementation
    //------------------------------------------------------------------------

    /**
     * Attempts to set the <code>datalift.home</code> system property
     * from the corresponding environment variable (DATALIFT_HOME) if
     * not set
     */
    protected void setupEnvironment() {
        if (System.getProperty(DATALIFT_HOME) == null) {
            // Try to define datalift.home system property from environment.
            String homePath = System.getenv(
                                DATALIFT_HOME.replace('.', '_').toUpperCase());
            if (homePath != null) {
                System.setProperty(DATALIFT_HOME, homePath);
            }
            // Else: All configuration files are assumed to be present in
            //       JVM classpath or be configured with absolute paths.
        }
    }

    /**
     * Loads the DataLift configuration and initializes the application
     * root resources.
     * @param  props   the application runtime environment.
     *
     * @throws TechnicalException if any error occurred.
     */
    protected void initApplication(Properties props) {
        LogContext.resetContexts("Core", "init");
        log = Logger.getLogger();

        try {
            // Load application configuration.
            Configuration cfg = this.loadConfiguration(props);
            Configuration.setDefault(cfg);
            // Load available application modules.
            this.loadModules(cfg);
            // Initialize resources.
            // First initialization step.
            this.components.add(
                    this.initResource(new RouterResource(this.modules), cfg));
            this.components.add(
                    this.initResource(new DefaultProjectManager(), cfg));
            // Second initialization step.
            for (LifeCycle r : this.components) {
                this.postInitResource(r, cfg);
            }
            this.postInitModules(cfg);
            // So far, so good.
            log.info("DataLift initialization complete");
        }
        catch (Throwable e) {
            TechnicalException error = new TechnicalException(
                                "configuration.load.error", e, e.getMessage());
            log.fatal(error.getMessage(), e);
            throw error;
        }
        finally {
            LogContext.resetContexts();
        }
    }

    /**
     * Locates the DataLift configuration from the specified properties
     * or environment variables and returns the corresponding
     * {@link Configuration} object.
     * <p>
     * This default implementation return an instance of
     * {@link DefaultConfiguration}.</p>
     * @param  props   the properties describing the application runtime
     *                 environment.
     *
     * @return a DataLift {@Link Configuration} populated from the
     *         found configuration file(s).
     * @throws TechnicalException if any error occurred.
     */
    protected Configuration loadConfiguration(Properties props) {
        return new DefaultConfiguration(props);
    }

    /**
     * Shuts the DataLift application down.
     * @throws TechnicalException if any error occurred.
     */
    protected void shutdownApplication() {
        LogContext.resetContexts("Core", "shutdown");
        Logger log = Logger.getLogger();

        try {
            Configuration cfg = Configuration.getDefault();
            if (this.resources != null) {
                for (Object r : this.resources) {
                    if (r instanceof LifeCycle) {
                        try {
                            ((LifeCycle)r).shutdown(cfg);
                            cfg.removeBean(r, null);
                        }
                        catch (Exception e) {
                            TechnicalException error = new TechnicalException(
                                            "resource.shutdown.error", e,
                                            r, e.getMessage());
                            log.error(error.getMessage(), e);
                            throw error;
                        }
                    }
                }
            }
            // Shutdown each module, ignoring errors.
            for (ModuleDesc desc : this.modules.values()) {
                Module m = desc.module;
                Object[] prevCtx = LogContext.pushContexts(desc.name, "shutdown");
                try {
                    m.shutdown(cfg);
                    cfg.removeBean(desc.module, desc.name);                
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
            log.info("DataLift shutdown complete");
        }
        finally {
            LogContext.resetContexts();
            super.shutdown();
        }
    }

    /**
     * Initializes (step #1) a resource object.
     * @param  r     the resource to configure.
     * @param  cfg   the DataLift configuration.
     *
     * @return the resource, configured.
     * @throws TechnicalException if any error occurred.
     */
    private LifeCycle initResource(LifeCycle r, Configuration cfg) {
        try {
            ((LifeCycle)r).init(cfg);
            cfg.registerBean(r);
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                            "resource.init.error", e, r, e.getMessage());
            log.error(error.getMessage(), e);
            throw error;
        }
        return r;
    }

    /**
     * Initializes (step #2) a resource object.
     * @param  r     the resource to configure.
     * @param  cfg   the DataLift configuration.
     *
     * @return the resource, ready for processing requests.
     * @throws TechnicalException if any error occurred.
     */
    private void postInitResource(LifeCycle r, Configuration cfg) {
        try {
            ((LifeCycle)r).postInit(cfg);
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                            "resource.init.error", e, r, e.getMessage());
            log.error(error.getMessage(), e);
            throw error;
        }
    }

    private void postInitModules(Configuration configuration) {
        // Post-init each module, ignoring errors.
        for (Iterator<ModuleDesc> i=this.modules.values().iterator();
                                                                i.hasNext(); ) {
            ModuleDesc desc = i.next();
            Object[] prevCtx = LogContext.pushContexts(desc.name, "postInit");
            try {
                // Complete module initialization.
                desc.module.postInit(configuration);
            }
            catch (Exception e) {
                log.error("Post-init failed for module {}: {}", e,
                          desc.name, e.getMessage());
                // Disable module.
                i.remove();
                configuration.removeBean(desc.module, desc.name);
                // Continue with next module.
            }
            finally {
                LogContext.pushContexts(prevCtx[0], prevCtx[1]);
            }
        }
    }

    private void loadModules(Configuration cfg) {
        this.modules.clear();
        // Load modules embedded in web application first (if any).
        this.loadModules(this.getClass().getClassLoader(), null);
        // Load third-party module bundles.
        if (cfg.getModulesPath() != null) {
            List<File> l = Arrays.asList(cfg.getModulesPath().listFiles(
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
            throw new TechnicalException("module.load.error", e,
                                         f.getName(), e.getMessage());
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
        Configuration cfg = Configuration.getDefault();

        for (Module m : ServiceLoader.load(Module.class, cl)) {
            String name = m.getName();
            // Initialize module.
            Object[] prevCtx = LogContext.pushContexts(name, "init");
            try {
                m.init(cfg);
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
            cfg.registerBean(m);
            cfg.registerBean(name, m);
            // Publish module REST resources.
            ModuleDesc desc = new ModuleDesc(m, f, cl);
            this.modules.put(name, desc);
            // Notify module registration.
            int resourceCount = desc.ressourceClasses.size();
            if (desc.isResource) {
                resourceCount++;
            }
            if (resourceCount > 0) {
                log.info("Registered module \"{}\" as \"{}\" ({} resource(s))",
                                        m.getClass().getSimpleName(),
                                        name, Integer.valueOf(resourceCount));
            }
            else {
                log.info("Registered module \"{}\" as \"{}\"",
                                        m.getClass().getSimpleName(), name);
            }
        }
    }

    /**
     * Analyzes a module structure to match the expected elements and
     * returns the paths to be added to the module classpath.
     * <p>
     * Recognized elements include:</p>
     * <ul>
     *  <li>For JAR files: the JAR file itself.</li>
     *  <li>For directories:
     *   <dl>
     *    <dt><code>/</code></dt>
     *    <dd>The module root directory</dd>
     *    <dt><code>/classes</code></dt>
     *    <dd>The default directory for module classes</dd>
     *    <dt><code>/*.jar</code></dt>
     *    <dd>JAR files containing the module classes</dd>
     *    <dt><code>/lib/**&#47;*.jar</code></dt>
     *    <dd>All the JAR files in the <code>/lib</code> directory tree
     *        (module classes and third-party libraries)</dd>
     *   </dl></li>
     * </ul>
     * @param  path   the directory or JAR file for the module.
     *
     * @return the URLs of the paths to be added to the module
     *         classpath.
     */
    private URL[] getModulePaths(File path) {
        List<URL> urls = new LinkedList<URL>();

        if (path.isDirectory()) {
            // Add module root directory.
            urls.add(this.getFileUrl(path));
            // Look for module classes as a directory tree.
            File classesDir = new File(path, MODULE_CLASSES_DIR);
            if (classesDir.isDirectory()) {
                urls.add(this.getFileUrl(classesDir));
            }
            // Look for root-level JAR files.
            for (File jar : path.listFiles(jarFilter)) {
                urls.add(this.getFileUrl(jar));
            }
            // Look for module dependencies as library JAR files.
            File libDir = new File(path, MODULE_LIB_DIR);
            if (classesDir.isDirectory()) {
                urls.addAll(this.findFiles(libDir, jarFilter));
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
}
