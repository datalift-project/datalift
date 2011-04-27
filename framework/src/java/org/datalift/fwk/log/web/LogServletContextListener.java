package org.datalift.fwk.log.web;


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
        LogService.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Shutdown log service.
        LogService.getInstance().shutdown();
    }
}
