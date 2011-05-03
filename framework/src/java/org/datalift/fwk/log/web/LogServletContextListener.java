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
        ServletContext ctx = event.getServletContext();
        // Make application init parameters available for log configuration.
        Properties props = new Properties(System.getProperties());
        for (Enumeration<?> e = ctx.getInitParameterNames();
             e.hasMoreElements(); ) {
            String name = (String)(e.nextElement());
            props.setProperty(name, ctx.getInitParameter(name));
        }
        // Initialize log service.
        LogService.selectAndConfigure(props);
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Shutdown log service.
        LogService.getInstance().shutdown();
    }
}
