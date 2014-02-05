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

package org.datalift.fwk.log.web;


import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.datalift.fwk.log.LogService;


/**
 * An implementation of {@link ServletContextListener} to properly
 * configure and shut down the logging system.
 *
 * @author lbihanic
 */
public class LogServletContextListener implements ServletContextListener
{
    //------------------------------------------------------------------------
    // ServletContextListener contract support
    //------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        // Initialize log service.
        this.init(this.getConfiguration(event.getServletContext()));
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Shutdown log service.
        this.shutdown();
    }

    //------------------------------------------------------------------------
    // Specific implementation
    //------------------------------------------------------------------------

    /**
     * Initializes the logging system using the specified configuration.
     * @param  props   the runtime environment configuration (e.g.
     *                 web application parameters).
     */
    public void init(Properties props) {
        // Initialize log service.
        LogService.selectAndConfigure(props);
    }

    /**
     * Shuts down the logging system.
     */
    public void shutdown() {
        // Shutdown log service.
        LogService.getInstance().shutdown();
    }

    /**
     * Returns a logging system configuration created from the web
     * application initialization parameters.
     * @param  ctx   the web application context.
     *
     * @return a Properties object to be used as logging system
     *         configuration.
     */
    protected final Properties getConfiguration(ServletContext ctx) {
        // Make application init parameters available for log configuration.
        Properties props = new Properties(System.getProperties());
        for (Enumeration<?> e = ctx.getInitParameterNames();
                                                     e.hasMoreElements(); ) {
            String name = (String)(e.nextElement());
            props.setProperty(name, ctx.getInitParameter(name));
        }
        return props;
    }
}
