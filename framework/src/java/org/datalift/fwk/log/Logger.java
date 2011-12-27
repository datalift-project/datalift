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

package org.datalift.fwk.log;


/**
 * A abstract class that defines the Logger contract and acts as
 * a factory to access concrete implementations.
 * <p>
 * The <code>Logger</code> class is the sole class of the log
 * framework that should be referenced in the client code.</p>
 * <p>
 * Clients should use one of the <code>getLogger()</code> methods to
 * retrieve <code>Logger</code> instances and then access the logging
 * methods (<code>fatal()</code>, <code>error()</code>,
 * <code>warning()</code>, <code>info()</code>, <code>trace()</code>
 * and <code>debug()</code>) on the returned object.</p>
 * <p>
 * The Logger interface has been designed to be compatible with
 * Log4J's Logger class to ease migration from or to Log4J. It only
 * differs from the Log4J's Logger on the following points:</p>
 * <ul>
 *  <li>The Log4J MDC (<em>Mapped Diagnostic Context</em>) class
 *      features (<code>put()</code> and <code>remove()</code>
 *      methods) are part of the Logger interface (see the
 *      {@link #setContext(Object, Object)} and
 *      {@link #removeContext(Object)} methods)</li>
 *  <li>The log methods (<code>fatal()</code>, <code>error()</code>,
 *      <code>warning()</code>, <code>info()</code>,
 *      <code>trace()</code> and <code>debug()</code> support parameter
 *      replacement a la SLF4J (i.e. using the "<code>{}</code>"
 *      argument substitution pattern)</li>
 *  <li>Support for debug and trace message promotion (to the warning
 *      and info levels) on a per thread basis, thanks to
 *      {@link #promoteDebugTraces(boolean)}. This method should be
 *      called from (web request) filters or aspects to force logging
 *      of debug message for specific executions</li>
 * </ul>
 * <p>
 * As in Log4J, six levels of logging are available:</p>
 * <ul>
 *  <li><code>fatal</code>: the application aborts immediately
 *  <li><code>error</code>: the application can continue to run
 *  <li><code>warning</code>: not a blocking error
 *  <li><code>info</code>: normal follow-up information
 *  <li><code>debug</code>: verbose mode
 *  <li><code>trace</code>: debugging traces, for developers only
 * </ul>
 *
 * @author lbihanic
 */
public abstract class Logger
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Log levels. */
    public enum LogLevel {
        FATAL, ERROR, WARN, INFO, DEBUG, TRACE
    }

    private final static String FORMAT_ELT_PATTERN = "{}";
    // private final static Pattern FORMAT_ELT_PATTERN = Pattern.compile("\\{}");

    //-------------------------------------------------------------------------
    // Logger contract definition
    //-------------------------------------------------------------------------

    /**
     * Checks whether this log is enabled for the info level.
     * <p>
     * This function is intended to lessen the computational cost of
     * disabled log info statements.</p>
     * <p>
     * For some <code>log</code> Log object, when you write,
     * <pre>
     *   log.info("This is entry number: " + i );
     * </pre>
     * You incur the cost constructing the message, concatenation in
     * this case, regardless of whether the message is logged or not.</p>
     * <p>
     * If you are worried about speed, then you should write
     * <pre>
     *   if (log.isInfoEnabled()) {
     *     log.info("This is entry number: " + i);
     *   }
     * </pre></p>
     * <p>
     * This way you will not incur the cost of parameter construction
     * if logging is disabled for <code>log</code>.</p>
     *
     * @return <code>true</code> if this log is info enabled,
     *         <code>false</code> otherwise.
     */
    public boolean isInfoEnabled() {
        return this.isEnabledFor(LogLevel.INFO);
    }

    /**
     * Checks whether this log is enabled for the trace level.
     * <p>
     * This function is intended to lessen the computational cost of
     * disabled log trace statements.</p>
     * <p>
     * For some <code>log</code> Log object, when you write,
     * <pre>
     *   log.trace("This is entry number: " + i );
     * </pre>
     * You incur the cost of constructing the message, concatenation in
     * this case, regardless of whether the message is logged or not.</p>
     * <p>
     * If you are worried about speed, then you should write
     * <pre>
     *   if (log.isTraceEnabled()) {
     *     log.trace("This is entry number: " + i);
     *   }
     * </pre></p>
     * <p>
     * This way you will not incur the cost of parameter construction
     * if logging is disabled for <code>log</code>.</p>
     *
     * @return <code>true</code> if this log is trace enabled,
     *         <code>false</code> otherwise.
     */
    public boolean isTraceEnabled() {
        return this.isEnabledFor(LogLevel.TRACE);
    }

    /**
     * Checks whether this log is enabled for the debug level.
     * <p>
     * This function is intended to lessen the computational cost of
     * disabled log debug statements.</p>
     * <p>
     * For some <code>log</code> Log object, when you write,
     * <pre>
     *   log.debug("This is entry number: " + i );
     * </pre>
     * You incur the cost of constructing the message, concatenation in
     * this case, regardless of whether the message is logged or not.</p>
     * <p>
     * If you are worried about speed, then you should write
     * <pre>
     *   if (log.isDebugEnabled()) {
     *     log.debug("This is entry number: " + i);
     *   }
     * </pre></p>
     * <p>
     * This way you will not incur the cost of parameter construction
     * if logging is disabled for <code>log</code>.</p>
     *
     * @return <code>true</code> if this log is debug enabled,
     *         <code>false</code> otherwise.
     */
    public boolean isDebugEnabled() {
        return this.isEnabledFor(LogLevel.DEBUG);
    }

    /**
     * Logs a message object with the fatal level.
     * @param  message   the message object to log.
     * @param  args      the arguments to format the message.
     */
    public void fatal(Object message, Object... args) {
        this.fatal(message, null, args);
    }

    /**
     * Logs a message object with the fatal level including the
     * stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * @param  message   the message object to log.
     * @param  t         the exception to log.
     * @param  args      the arguments to format the message.
     */
    public void fatal(Object message, Throwable t, Object... args) {
        this.log(LogLevel.FATAL, message, t, args);
    }

    /**
     * Logs a message object with the error level.
     * @param  message   the message object to log.
     * @param  args      the arguments to format the message.
     */
    public void error(Object message, Object... args) {
        this.error(message, null, args);
    }

    /**
     * Logs a message object with the error level including the
     * stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * @param  message   the message object to log.
     * @param  t         the exception to log.
     * @param  args      the arguments to format the message.
     */
    public void error(Object message, Throwable t, Object... args) {
        this.log(LogLevel.ERROR, message, t, args);
    }

    /**
     * Logs a message object with the warning level.
     * @param  message   the message object to log.
     * @param  args      the arguments to format the message.
     */
    public void warn(Object message, Object... args) {
        this.warn(message, null, args);
    }

    /**
     * Logs a message object with the warning level including the
     * stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * @param  message   the message object to log.
     * @param  t         the exception to log.
     * @param  args      the arguments to format the message.
     */
    public void warn(Object message, Throwable t, Object... args) {
        this.log(LogLevel.WARN, message, t, args);
    }

    /**
     * Logs a message object with the information level.
     * @param  message   the message object to log.
     * @param  args      the arguments to format the message.
     */
    public void info(Object message, Object... args) {
        this.info(message, null, args);
    }

    /**
     * Logs a message object with the information level including the
     * stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * @param  message   the message object to log.
     * @param  t         the exception to log.
     * @param  args      the arguments to format the message.
     */
    public void info(Object message, Throwable t, Object... args) {
        this.log(LogLevel.INFO, message, t, args);
    }

    /**
     * Logs a message object with the debug level.
     * @param  message   the message object to log.
     * @param  args      the arguments to format the message.
     */
    public void debug(Object message, Object... args) {
        this.debug(message, null, args);
    }

    /**
     * Logs a message object with the debug level including the
     * stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * @param  message   the message object to log.
     * @param  t         the exception to log.
     * @param  args      the arguments to format the message.
     */
    public void debug(Object message, Throwable t, Object... args) {
        this.log(LogLevel.DEBUG, message, t, args);
    }

    /**
     * Logs a message object with the trace level.
     * @param  message   the message object to log.
     * @param  args      the arguments to format the message.
     */
    public void trace(Object message, Object... args) {
        this.trace(message, null, args);
    }

    /**
     * Logs a message object with the trace level including the
     * stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * @param  message   the message object to log.
     * @param  t         the exception to log.
     * @param  args      the arguments to format the message.
     */
    public void trace(Object message, Throwable t, Object... args) {
        this.log(LogLevel.TRACE, message, t, args);
    }

    /**
     * Sets a per-thread diagnostic <code>context</code> entry
     * identified by <code>name</code>.
     * <p>
     * If the target {@link Logger} implementation supports
     * per-thread diagnostic contexts (Apache Log4J does), it is
     * possible to configure the logged message format to include
     * the context information.</p>
     * @param  key       the context name.
     * @param  context   the diagnostic context to be included in
     *                   the message text. The provided object will
     *                   be converted into a text string using
     *                   <code>Object.toString()</code>. If
     *                   <code>null</code> the context is removed.
     *
     * @return the previous value for the context or <code>null</code>
     *         if the context was not set.
     *
     * @see    #removeContext(Object)
     */
    public static Object setContext(Object key, Object context) {
        return LogService.getInstance()
                         .setDiagnosticContext(key.toString(), context);
    }

    /**
     * Removes a per-thread diagnostic context if it exists.
     * @param  key   the name of the context to remove.
     *
     * @return the previous value for the context or <code>null</code>
     *         if the context was not set.
     */
    public static Object removeContext(Object key) {
        return LogService.getInstance()
                         .removeDiagnosticContext(key.toString());
    }

    /**
     * Removes all per-thread diagnostic contexts.
     */
    public static void clearContexts() {
        LogService.getInstance().clearDiagnosticContexts();
    }

    /**
     * Sets whether debug and trace log requests shall be promoted
     * to a higher level (info and warning).
     * @param  promote   <code>true</code> to force debug and trace
     *                   log requests to a higher level;
     *                   <code>false</code> otherwise.
     */
    public final static void promoteDebugTraces(boolean promote) {
        LogService.getInstance().promoteDebugTraces(promote);
    }

    /**
     * Returns whether debug and trace log requests are promoted
     * to a higher level (info and warning).
     *
     * @return <code>true</code> if debug and trace log requests are
     *         promoted to a higher level; <code>false</code>
     *         otherwise.
     */
    public final static boolean areDebugTracesPromoted() {
        return LogService.getInstance().areDebugTracesPromoted();
    }

    //-------------------------------------------------------------------------
    // Logger provider contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the level object for the underlying implementation
     * corresponding to the specified log level.
     * @param  level                the log level to map.
     * @param  promoteDebugTraces   <code>true</code> to force debug
     *                              and trace log requests to a higher
     *                              level; <code>false</code> otherwise.
     *
     * @return the level object for the underlying implementation
     *         corresponding to <code>level</code> or <code>null</code>
     *         if logging is disabled for <code>level</code>.
     */
    abstract protected Object getActualLevel(LogLevel level,
                                             boolean promoteDebugTraces);

    /**
     * Logs a message object with the specified mapped level including
     * the stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * @param  level       the mapped log level.
     * @param  message     the message object to log.
     * @param  formatted   whether argument substitution occurred.
     * @param  t           the exception to log.
     * @param  args        the arguments to format the message.
     */
    abstract protected void doLog(Object level, Object message,
                                boolean formatted, Throwable t, Object... args);

    /**
     * Returns the string representation of the specified object.
     * @param  o   the object to render.
     *
     * @return the string representation of <code>o</code>.
     */
    protected String render(Object o) {
        return String.valueOf(o);
    }

    //-------------------------------------------------------------------------
    // Factory methods
    //-------------------------------------------------------------------------

    /**
     * Retrieves a logger named according to the class of the
     * invoker object.
     * <p>
     * This method is equivalent to
     * <code>Logger.getLogger(this.getClass())</code> or
     * <code>Logger.getLogger(Foo.class)</code>.</p>
     *
     * @return a <code>Logger</code> object.
     */
    public static Logger getLogger() {
        return LogService.getInstance().getLogger(
                                        CallerClassFinder.getCallerClass());
    }

    /**
     * Retrieves a logger named according to the value of the name
     * parameter. If the named logger already exists, then the
     * existing instance will be returned. Otherwise, a new instance
     * is created.
     * @param  name   the name of the logger to retrieve.
     *
     * @return a <code>Logger</code> object.
     */
    public static Logger getLogger(String name) {
        return LogService.getInstance().getLogger(name);
    }

    /**
     * Shorthand for <code>getLogger(clazz.getName())</code>.
     * @param  clazz   the name of class will be used as the name of
     *                 the logger to retrieve. See
     *                 {@link #getLogger(java.lang.String)} for more
     *                 detailed information.
     *
     * @return a <code>Logger</code> object.
     */
    public static Logger getLogger(Class<?> clazz) {
        return LogService.getInstance().getLogger(clazz);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns whether logging is enabled for the specified log level.
     * @param  level   the log level.
     *
     * @return <code>true</code> if logging is enabled for
     *         <code>level</code>; <code>false</code> otherwise.
     */
    private boolean isEnabledFor(LogLevel level) {
        return (this.getActualLevel(level,
                                    Logger.areDebugTracesPromoted()) != null);
    }

    /**
     * Logs a message object with the specified level including the
     * stack trace of the {@link java.lang.Throwable} passed as
     * parameter.
     * <p>
     * If the message object is a string and a list of arguments is
     * provided, the message is scanned for the argument substitution
     * pattern and each occurrence of the pattern is replaced by the
     * corresponding positional argument.</p>
     * <p>
     * Prior formatting and {@link #doLog logging} of the message, this
     * method checks for
     * {@link #promoteDebugTraces debug/trace message promotion} and
     * {@link #getActualLevel(LogLevel, boolean) maps} the log level.</p>
     * @param  level     the log level for the message.
     * @param  message   the message object to log.
     * @param  t         the exception to log.
     * @param  args      the arguments to format the message.
     */
    protected final void log(LogLevel level, Object message,
                                          Throwable t, Object... args) {
        Object implLevel = this.getActualLevel(level,
                                               Logger.areDebugTracesPromoted());
        if (implLevel != null) {
            String formattedMsg = this.applyFormat(message, args);
            if (formattedMsg != null) {
                this.doLog(implLevel, formattedMsg, true, t);
            }
            else {
                this.doLog(implLevel, message, false, t, args);
            }
        }
    }

    private final String applyFormat(Object message, Object... args) {
        String msg = null;
        if ((args != null) && (args.length != 0)
                           && (message instanceof String)) {
            String fmt = (String)message;
            int p = fmt.indexOf(FORMAT_ELT_PATTERN);
            if (p != -1) {
                // Substitution pattern found. => Replace it with arguments.
                final StringBuilder b = new StringBuilder(256);
                final int l = FORMAT_ELT_PATTERN.length();
                int i = 0;
                int n = 0;
                do {
                    b.append(fmt.substring(n, p))
                     .append(this.render(args[i]));
                    i++;
                    n = p + l;
                    p = fmt.indexOf(FORMAT_ELT_PATTERN, p + 1);
                }
                while ((p != -1) && (i < args.length));

                msg = b.append(fmt.substring(n)).toString();
            }
            // Else: return null as no formatting was performed.
        }
        return msg;
    }

    //-------------------------------------------------------------------------
    // CallerClassFinder nested class
    //-------------------------------------------------------------------------

    /**
     * Finds the class that invoked one of the Logger service methods.
     */
    private static final class CallerClassFinder extends SecurityManager
    {
        public CallerClassFinder() {
            super();
        }

        /**
         * Finds the class that invoked one of the Logger service
         * methods.
         *
         * @return the class of the Logger invoker.
         */
        public static Class<?> getCallerClass() {
            Class<?>[] context = new CallerClassFinder().getClassContext();

            // context[0] is this class,
            // context[1] is the Logger class,
            // context[2] is the class that invoked the Logger.
            return context[2];
        }
    }
}
