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
     * @return a Properties objet to be used as logging system
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
