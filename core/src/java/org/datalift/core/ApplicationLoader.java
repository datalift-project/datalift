package org.datalift.core;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.datalift.core.log.LogContext;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
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
    // Class members
    //-------------------------------------------------------------------------

    /** The DataLift configuration. */
    private static Configuration configuration = null;
    /** The singleton resources managed by this JAX-RS application. */
    private static Set<Object> resources = null;

    private static Logger log = null;

    //------------------------------------------------------------------------
    // ServletContextListener contract support
    //------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        // Initialize system properties from env. variables if need be.
        this.setupEnvironment(ctx);
        // Initialize log service.
        super.contextInitialized(event);
        // Initialize DataLift application.
        this.init(ctx);
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Shutdown application.
        this.shutdown();
        // Shutdown log service.
        super.contextDestroyed(event);
    }

    //------------------------------------------------------------------------
    // Property accessors
    //------------------------------------------------------------------------

    /**
     * Returns the DataLift configuration object.
     * @return the DataLift configuration.
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the JAX-RS root resource singleton objects to be
     * registered at startup.
     * @return the JAX-RS root resources.
     */
    public static Set<Object> getResources() {
        return resources;
    }

    //------------------------------------------------------------------------
    // Specific implementation
    //------------------------------------------------------------------------

    /**
     * Attempts to set the <code>datalift.home</code> system property
     * from the corresponding environment variable (DATALIFT_HOME) if
     * not set
     */
    private void setupEnvironment(ServletContext ctx) {
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
     * @param  ctx   the web application context.
     *
     * @throws TechnicalException if any error occurred.
     */
    private void init(ServletContext ctx) {
        LogContext.resetContexts("Core", "init");
        log = Logger.getLogger();

        try {
            // Load application configuration.
            configuration = new DefaultConfiguration(ctx);
            // Initialize resources.
            // First initialization step.
            Set<Object> rsc = new HashSet<Object>();
            rsc.add(this.initResource(new RouterResource()));
            // So far, so good. => Install singletons
            resources = Collections.unmodifiableSet(rsc);
            // Second initialization step.
            this.postInit();
            log.info("DataLift application initialized");
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

    private void postInit() {
    	for (Object r : resources) {
            if (r instanceof LifeCycle) {
                try {
                    ((LifeCycle)r).postInit(configuration);
                }
                catch (Exception e) {
                    TechnicalException error = new TechnicalException(
                                "resource.init.error", e, r, e.getMessage());
                    log.error(error.getMessage(), e);
                    throw error;
                }
            }
    	}
    }
    
    /**
     * Initializes a resource object.
     * @param  r   the resource to configure.
     * @return the resource, ready for processing requests.
     *
     * @throws TechnicalException if any error occurred.
     */
    private Object initResource(Object r) {
        if (r instanceof LifeCycle) {
            try {
                ((LifeCycle)r).init(configuration);
            }
            catch (Exception e) {
                TechnicalException error = new TechnicalException(
                                "resource.init.error", e, r, e.getMessage());
                log.error(error.getMessage(), e);
                throw error;
            }
        }
        return r;
    }

    /**
     * Shuts down the DataLift application resources.
     */
    private void shutdown() {
        LogContext.resetContexts("Core", "shutdown");
        Logger log = Logger.getLogger();

        try {
            if (resources != null) {
                for (Object r : resources) {
                    if (r instanceof LifeCycle) {
                        try {
                            ((LifeCycle)r).shutdown(configuration);
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
            log.info("DataLift application shutdown complete");
        }
        finally {
            LogContext.resetContexts();
        }
    }
}
