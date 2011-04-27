package org.datalift.core;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.datalift.core.log.LogContext;
import org.datalift.core.project.WorkspaceResource;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.log.web.LogServletContextListener;


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
        // Initialize log service.
        super.contextInitialized(event);
        // Initialize DataLift application.
        this.init(event.getServletContext());
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
            Set<Object> rsc = new HashSet<Object>();
            rsc.add(this.initResource(new RouterResource()));
            rsc.add(this.initResource(new WorkspaceResource()));
            // So far, so good. => Install singletons
            resources = Collections.unmodifiableSet(rsc);
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
