package org.datalift.core.log.log4j;


import java.net.URL;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.MDC;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator2;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.log.LogService;


/**
 * An implementation of {@link LogService} that relies on Apache's
 * <a href="http://logging.apache.org/log4j/">Log4J</a> framework for
 * the actual logging.
 * <p>
 * To use this implementation, specify the following provider
 * definition for the <code>org.datalift.fwk.log.LogService</code>
 * service in the corresponding service provider configuration
 * file:</p>
 * <blockquote><pre>
 *   org.datalift.fwk.log.log4j.Log4JLogService
 * </pre></blockquote>
 *
 * @author  lbihanic
 */
public final class Log4JLogService extends LogService
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    public final static PromotedLevel PROMOTED_DEBUG =
                                new PromotedLevel(Level.DEBUG_INT, "DEBUG");
    public final static PromotedLevel PROMOTED_TRACE =
                                new PromotedLevel(Level.TRACE_INT, "TRACE");

    private static final String LOG4J_INIT_OVERRIDE_KEY = 
                                                 "log4j.defaultInitOverride";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The Log4J logger repository. */
    private Hierarchy loggerRepository = null;

    //-------------------------------------------------------------------------
    // LogService contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    public Logger getLogger(String name) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("name");
        }
        return new LoggerImpl(this.loggerRepository.getLogger(name));
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation registers the per-thread diagnostic
     * contexts using Log4J's MDC (<em>Mapped Diagnostic Context</em>)
     * feature.</p>
     */
    @Override
    public Object setDiagnosticContext(String name, Object context) {
        Object oldCtx = MDC.get(name);
        if (context != null) {
            MDC.put(name, context);
        }
        else {
            MDC.remove(name);
        }
        return oldCtx;
    }

    /** {@inheritDoc} */
    @Override
    public Object removeDiagnosticContext(String name) {
        Object oldCtx = MDC.get(name);
        MDC.remove(name);
        return oldCtx;
    }

    /** {@inheritDoc} */
    @Override
    public void clearDiagnosticContexts() {
        MDC.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void init(Properties props) {
        // Try to install a specific logger factory.
        try {
            Hierarchy h = new Hierarchy(new RootLogger(Level.DEBUG));
            // Configure Log4J, forcing usage of an XML configurator
            // supporting XML schemas and includes.
            URL u = this.getClass().getClassLoader().getResource("log4j.xml");
            DOMConfigurator2 cfg = new DOMConfigurator2();
            if (props != null) {
                cfg.setProperties(props);
            }
            cfg.doConfigure(u, h);

            // Install configured Logger factory as default.
            // 1. Prevent loading of default Log4J configuration.
            String oldOverride = System.setProperty(LOG4J_INIT_OVERRIDE_KEY,
                                                    String.valueOf(true));
            // 2. Install configured Logger factory.
            LogManager.setRepositorySelector(
                                    new DefaultRepositorySelector(h), this);
            // 3. Restore previous Log4J system configuration.
            if (oldOverride != null) {
                System.setProperty(LOG4J_INIT_OVERRIDE_KEY, oldOverride);
            }
            else {
                System.getProperties().remove(LOG4J_INIT_OVERRIDE_KEY);
            }
        }
        catch (IllegalArgumentException e) {
            // Oops! Another non default Logger factory has already been
            // installed => Use it...
        }
        this.loggerRepository = (Hierarchy)(LogManager.getLoggerRepository());
        
        // Capture all java.util.logging requests and redirect them to Log4J.
        JulToLog4jHandler.install(this.loggerRepository);
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        // Remove redirection of java.util.logging requests to Log4J.
        JulToLog4jHandler.uninstall();
        // Shutdown log service.
        this.loggerRepository.shutdown();
    }

    //-------------------------------------------------------------------------
    // Logger implementation nested class
    //-------------------------------------------------------------------------

    /**
     * A Logger implementation wrapping a Log4J Logger object.
     */
    private final class LoggerImpl extends Logger
    {
        /**
         * The fully qualified class name of this class, passed on to
         * Log4J to allow it detect the actual log user class.
         */
        private final String FQCN = LoggerImpl.class.getName();
        /** The wrapped Log4J Logger object. */
        private final org.apache.log4j.Logger proxied;
        /** The resurce bundle associated to the wrapped logger. */
        private final ResourceBundle msgBundle;

        /**
         * Default constructor.
         * @param  proxied   the wrapped Log4J Logger.
         */
        public LoggerImpl(org.apache.log4j.Logger proxied) {
            this.proxied = proxied;
            this.msgBundle = proxied.getResourceBundle();
        }

        //---------------------------------------------------------------------
        // Logger provider contract support
        //---------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        protected Level getActualLevel(LogLevel level,
                                          boolean promoteDebugTraces) {
            Level p = null;
            switch (level) {
                case FATAL:
                    p = Level.FATAL;
                    break;
                case ERROR:
                    p = Level.ERROR;
                    break;
                case WARN:
                    p = Level.WARN;
                    break;
                case INFO:
                    p = Level.INFO;
                    break;
                case DEBUG:
                    p = (promoteDebugTraces)? PROMOTED_DEBUG: Level.DEBUG;
                    break;
                case TRACE:
                    p = (promoteDebugTraces)? PROMOTED_TRACE: Level.TRACE;
                    break;
                default:
                    p = Level.OFF;
                    break;
            }
            return (this.proxied.isEnabledFor(p))? p: null;
        }

        /** {@inheritDoc} */
        @Override
        protected void doLog(Object level, Object message, boolean formatted,
                                           Throwable t, Object... args) {
            if ((! formatted) && (this.msgBundle != null)
                              && (message instanceof String)) {
                String fmt = null;
                String key = (String)message;
                if (this.msgBundle.containsKey(key)) {
                    try {
                        fmt = this.msgBundle.getString(key);
                    }
                    catch (MissingResourceException e) { /* Ignore... */ }
                }
                if (fmt != null) {
                    message = MessageFormat.format(fmt, args);
                }
            }
            this.proxied.log(FQCN, (Priority)level, message, t);
        }

        /** {@inheritDoc} */
        @Override
        protected String render(Object o) {
            return (o instanceof String)? (String)o:
                   (o != null)?
                        loggerRepository.getRendererMap().get(o).doRender(o):
                        "";
        }

        //---------------------------------------------------------------------
        // Specific implementation
        //---------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "[" + this.getClass().getName()
                       + "] proxied=" + this.proxied.getName();
        }
    }

    //-------------------------------------------------------------------------
    // PromotedLevel implementation nested class
    //-------------------------------------------------------------------------

    /**
     * A specialization of Log4J Level to support promoted debug traces
     * log levels.
     */
    private static class PromotedLevel extends Level
    {
        protected PromotedLevel(int level, String levelStr) {
            super(Level.INFO_INT + (level / 100), levelStr, 7);
        }
    }
}
