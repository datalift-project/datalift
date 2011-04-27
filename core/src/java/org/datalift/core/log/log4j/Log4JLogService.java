package org.datalift.core.log.log4j;


import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.MDC;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator2;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.log.LogService;


/**
 * An implementation of the {@link LogService} that relies on Apache's
 * <a href="http://logging.apache.org/log4j/">Log4J</a> framework for
 * the actual logging.
 * <p>
 * The Log4J configuration may be specified using the
 * {@link #setConfiguration configuration} property that supports both
 * XML and property type Log4J configurations.</p>
 * <p>
 * To use this implementation, specify the following provider
 * definition for the <code>org.datalift.fwk.log.LogService</code>
 * service in the corresponding service provider configuration
 * file:</p>
 * <blockquote><pre>
 *   org.datalift.fwk.log.log4j.Log4JLogService
 * </pre></blockquote>
 *
 * @author  Laurent Bihanic
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

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The Log4J logger repository. */
    private Hierarchy loggerRepository = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public Log4JLogService() {
        super();
        // Force usage of new XML configurator supporting XML includes.
        System.setProperty("log4j.configuratorClass",
                           DOMConfigurator2.class.getName());
        // Initialize Log4J
        this.loggerRepository = (Hierarchy)(LogManager.getLoggerRepository());
        // Capture all java.util.logging requests and redirect them to Log4J.
        JulToLog4jHandler.install(this.loggerRepository);
    }

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
    public void shutdown() {
        // Remove redirection of java.util.logging requests to Log4J.
        JulToLog4jHandler.uninstall();
        // Shutdown log service.
        this.loggerRepository.shutdown();
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Sets the XML or properties file to use for Log4J configuration.
     *
     * @param  fileName   the configuration file path.
     *
     * @throws IOException   if any error occurred while reading the
     *                       specified configuration.
     */
    public void setConfiguration(String fileName) throws IOException {
        if ((fileName == null) || (fileName.length() == 0)) {
            throw new IllegalArgumentException("fileName");
        }
        Hierarchy hierarchy = new Hierarchy(
                                new RootLogger(org.apache.log4j.Level.DEBUG));
        if (fileName.endsWith(".xml")) {
            new DOMConfigurator2().doConfigure(fileName, hierarchy);
        }
        else if (fileName.endsWith(".properties")) {
            new PropertyConfigurator().doConfigure(
                                this.loadProperties(fileName), hierarchy);
        }
        else {
            throw new IllegalArgumentException(
                                "fileName: Please specify an \"xml\" or " +
                                "\"properties\" file for configuring Log4J.");
        }
        this.loggerRepository = hierarchy;
    }

    /**
     * Loads a properties file.
     *
     * @param  fileName   the properties file path.
     */
    private Properties loadProperties(String fileName) throws IOException {
        InputStream is = null;

        try {
            Properties props = new Properties()
                {
                    @Override
                    public String getProperty(String key) {
                        String value = System.getProperty(key);
                        if (value == null) {
                            value = super.getProperty(key);
                        }
                        return value;
                    }

                    @Override
                    public Object get(Object key) {
                        Object value = null;

                        if (key instanceof String) {
                            value = System.getProperty((String)key);
                        }
                        if (value == null) {
                            value = super.get(key);
                        }
                        return value;
                    }
                };

            if ((fileName != null) && (fileName.length() != 0)) {
                is = this.getClass().getClassLoader().
                                     getResourceAsStream(fileName);
                if (is == null) {
                    throw new FileNotFoundException(fileName);
                }
                props.load(is);
            }
            return props;
        }
        finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { /* Ignore... */ }
            }
        }
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

    private static class PromotedLevel extends Level
    {
        protected PromotedLevel(int level, String levelStr) {
            super(Level.INFO_INT + (level / 100), levelStr, 7);
        }
    }
}
