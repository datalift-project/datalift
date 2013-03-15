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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import org.datalift.core.log.TimerContext;
import org.datalift.core.project.DefaultProjectManager;
import org.datalift.core.velocity.jersey.VelocityTemplateProcessor;
import org.datalift.core.velocity.jersey.VelocityViewFactory;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.Module;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.log.web.LogServletContextListener;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.util.DefaultUriBuilder;
import org.datalift.fwk.view.ViewFactory;

import static org.datalift.core.DefaultConfiguration.DATALIFT_HOME;
import static org.datalift.core.log.LogContext.*;


/**
 * A Servlet context listener to initialize and shutdown the DataLift
 * application.
 *
 * @author lbihanic
 */
public class ApplicationLoader extends LogServletContextListener
{
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
            for (Module m : Configuration.getDefault().getBeans(Module.class)) {
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
        Logger.setContext(Timer, new TimerContext());
        Logger.setContext(Path, "Init");
        log = Logger.getLogger();

        try {
            // Load application configuration.
            DefaultConfiguration cfg = this.loadConfiguration(props);
            Configuration.setDefault(cfg);
            // Find available third-party modules.
            this.registerBundles(cfg);
            // Initialize file stores and RDF store connections
            // (connectors may be provided as part of third-party modules).
            cfg.init();
            // Load and initialize modules form third-party packages.
            this.loadModules(cfg);
            // Initialize and register default resources if no custom
            // implementations are provided by third-party packages.
            if (cfg.getBeans(ViewFactory.class).isEmpty()) {
                // Add default view factory.
                cfg.registerBean(new VelocityViewFactory());
            }
            if (cfg.getBeans(UriBuilder.class).isEmpty()) {
                // Add default URI building policy.
                cfg.registerBean(new DefaultUriBuilder());
            }
            if (cfg.getBeans(ResourceResolver.class).isEmpty()) {
                // Add default resource resolver.
                this.components.add(
                    this.initResource(new RouterResource(), cfg));
            }
            if ((cfg.getRepositoryUris().contains(
                                        Configuration.INTERNAL_REPOSITORY)) &&
                (cfg.getBeans(ProjectManager.class).isEmpty())) {
                // Add default project manager (if an internal repository is
                // available to store projects).
                this.components.add(
                    this.initResource(new DefaultProjectManager(), cfg));
            }
            // Execute modules second initialization step.
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
            Logger.clearContexts();
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
     * @return a DataLift {@link Configuration} populated from the
     *         found configuration file(s).
     * @throws TechnicalException if any error occurred.
     */
    protected DefaultConfiguration loadConfiguration(Properties props) {
        return new DefaultConfiguration(props);
    }

    /**
     * Shuts the DataLift application down.
     * @throws TechnicalException if any error occurred.
     */
    protected void shutdownApplication() {
        Logger.setContext(Timer, new TimerContext());
        Logger.setContext(Path, "Shutdown");
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
            for (Module m : cfg.getBeans(Module.class)) {
                String name = m.getName();
                Object prevCtx = Logger.setContext(Path, "Shutdown - " + name);
                try {
                    m.shutdown(cfg);
                    cfg.removeBean(m, name);
                }
                catch (Exception e) {
                    log.error("Failed to properly shutdown module {}: {}", e,
                              m.getName(), e.getMessage());
                    // Continue with next module.
                }
                finally {
                    Logger.setContext(Path, prevCtx);
                }
            }
            if (cfg instanceof DefaultConfiguration) {
                ((DefaultConfiguration)cfg).shutdown();
            }
            log.info("DataLift shutdown complete");
        }
        finally {
            Logger.clearContexts();
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
        ClassLoader ctxClassLoader = Thread.currentThread()
                                           .getContextClassLoader();
        // Post-init each module, ignoring errors.
        for (Module m : configuration.getBeans(Module.class)) {
            String name = m.getName();
            Object prevCtx = Logger.setContext(Path, "PostInit - " + name);
            try {
                // Set context classloader to module classloader.
                Thread.currentThread().setContextClassLoader(
                                                m.getClass().getClassLoader());
                // Complete module initialization.
                m.postInit(configuration);
            }
            catch (Exception e) {
                log.error("Post-init failed for module {}: {}", e,
                                                        name, e.getMessage());
                // Disable module.
                configuration.removeBean(m, name);
                // Continue with next module.
            }
            finally {
                // Restore context classloader.
                Thread.currentThread().setContextClassLoader(ctxClassLoader);
                Logger.setContext(Path, prevCtx);
            }
        }
    }

    private void registerBundles(Configuration cfg) {
        ClassLoader cl = this.getClass().getClassLoader();
        // Load third-party bundles.
        for (File modulesDir : cfg.getModulePaths()) {
            log.debug("Searching \"{}\" for modules...", modulesDir);
            List<File> l = Arrays.asList(modulesDir.listFiles(
                    new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return (jarFilter.accept(f) ||
                                    directoryFilter.accept(f));
                        }
                    }));
            Collections.sort(l);
            for (File f : l) {
                Bundle b = new Bundle(f, cl, cfg);
                cfg.registerBean(b);
            }
        }
    }

    private void loadModules(Configuration cfg) {
        // Load modules embedded in web application first (if any).
        this.loadModules(new Bundle(this.getClass().getClassLoader()), cfg);
        // Load third-party packages.
        for (Bundle b : cfg.getBeans(Bundle.class)) {
            File f = b.getBundleFile();
            Object prevCtx = Logger.setContext(Path, "Init - " + f.getName());
            try {
                log.debug("Searching \"{}\" for modules...", f.getName());
                this.loadModules(b, cfg);
            }
            catch (Exception e) {
                log.fatal("Failed to load modules from {}. Skipping...",
                          e, f.getName());
                // Continue with next module.
            }
            finally {
                Logger.setContext(Path, prevCtx);
            }
        }
    }

    /**
     * Loads all {@link Module} implementation classes from the
     * specified bundle, initializing and registering them
     * as well as their Velocity templates.
     * @param  b   the bundle to load the module classes from.
     */
    private void loadModules(Bundle b, Configuration cfg) {
        ClassLoader ctxClassLoader = Thread.currentThread()
                                           .getContextClassLoader();
        File f = b.getBundleFile();
        for (Module m : b.loadServices(Module.class, false)) {
            String name = m.getName();
            log.debug("Initializing module \"{}\"...", name);
            try {
                // Set context classloader to module classloader.
                Thread.currentThread().setContextClassLoader(b.getClassLoader());
                // Initialize module.
                m.init(cfg);
                // Register module root (directory or JAR file) as
                // a Velocity template source, if available.
                if (f != null) {
                    VelocityTemplateProcessor.addModule(name, f);
                }
                // Make module available thru the Configuration object.
                cfg.registerBean(m);
                cfg.registerBean(name, m);
                // Notify module registration.
                log.info("Registered module \"{}\" ({})",
                                                name, m.getClass().getName());
            }
            catch (Exception e) {
                throw new TechnicalException("module.load.error", e,
                                                            b, e.getMessage());
            }
            finally {
                // Restore context classloader.
                Thread.currentThread().setContextClassLoader(ctxClassLoader);
            }
        }
    }
}
