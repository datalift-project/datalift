/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.core;


import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.datalift.core.log.LogContext;
import org.datalift.core.project.DefaultProjectManager;
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
    }

    /**
     * Shuts down the DataLift application.
     */
    @Override
    public void shutdown() {
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
            super.shutdown();
        }
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
    private void setupEnvironment() {
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
    private void initApplication(Properties props) {
        LogContext.resetContexts("Core", "init");
        log = Logger.getLogger();

        try {
            // Load application configuration.
            configuration = new DefaultConfiguration(props);
            // Initialize resources.
            // First initialization step.
            Set<Object> rsc = new HashSet<Object>();
            rsc.add(this.initResource(new RouterResource()));
            rsc.add(this.initResource(new DefaultProjectManager()));
            // Second initialization step.
            for (Object r : rsc) {
                this.postInitResource(r);
            }
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
     * Initializes (step #1) a resource object.
     * @param  r   the resource to configure.
     *
     * @return the resource, configured.
     * @throws TechnicalException if any error occurred.
     */
    private Object initResource(Object r) {
        if (r instanceof LifeCycle) {
            try {
                ((LifeCycle)r).init(configuration);
                configuration.registerBean(r);
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
     * Initializes (step #2) a resource object.
     * @param  r   the resource to configure.
     *
     * @return the resource, ready for processing requests.
     * @throws TechnicalException if any error occurred.
     */
    private void postInitResource(Object r) {
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
