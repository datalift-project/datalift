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


import java.util.Properties;
import java.util.ServiceLoader;


/**
 * An abstract class that defines the contract for log service
 * implementations.
 * <p>
 * A <code>LogService</code> implementation acts as a factory for
 * a specific {@link Logger} implementation. Concrete subclasses
 * shall implement the <code>createLogger()</code> method to return
 * their specific logger instance.</p>
 *
 * @author lbihanic
 */
public abstract class LogService
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /**
     * Cached instance of the configured <code>LogService</code>
     * implementation.
     */
    private static LogService instance = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public LogService() {
        super();
    }

    //-------------------------------------------------------------------------
    // LogService contract definition
    //-------------------------------------------------------------------------

    /**
     * Retrieves a logger named according to the value of the name
     * parameter.
     * @param  name   the name of the logger to retrieve.
     *
     * @return a <code>Logger</code> object.
     */
    abstract public Logger getLogger(String name);

    /**
     * Shorthand for <code>getLogger(clazz.getName())</code>.
     * @param  clazz   the name of class will be used as the name of
     *                 the logger to retrieve. See
     *                 {@link #getLogger(java.lang.String)} for more
     *                 detailed information.
     *
     * @return a <code>Logger</code> object.
     */
    public Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz");
        }
        return this.getLogger(clazz.getName());
    }

    /**
     * Sets a per-thread diagnostic <code>context</code> entry
     * identified by <code>name</code>.
     * <p>
     * If the target {@link Logger} implementation supports
     * per-thread diagnostic contexts (Apache Log4J does), it is
     * possible to configure the logged message format to include
     * the context information.</p>
     * @param  name      the context name.
     * @param  context   the diagnostic context to be included in
     *                   the message text. The provided object will
     *                   be converted into a text string using
     *                   <code>Object.toString()</code>
     *
     * @return the previous value for the context or <code>null</code>
     *         if the context was not set.
     */
    abstract public Object setDiagnosticContext(String name, Object context);

    /**
     * Removes a per-thread diagnostic context if it exists.
     * @param  name   the name of the context to remove.
     *
     * @return the previous value for the context or <code>null</code>
     *         if the context was not set.
     */
    abstract public Object removeDiagnosticContext(String name);

    /**
     * Removes all per-thread diagnostic contexts.
     */
    abstract public void clearDiagnosticContexts();

    /**
     * Sets whether debug and trace log requests shall be promoted
     * to a higher level (info and warning).
     * @param  promote   <code>true</code> to force debug and trace
     *                   log requests to a higher level;
     *                   <code>false</code> otherwise.
     */
    abstract public void promoteDebugTraces(boolean promote);

    /**
     * Returns whether debug and trace log requests are promoted
     * to a higher level (info and warning).
     *
     * @return <code>true</code> if debug and trace log requests are
     *         promoted to a higher level; <code>false</code>
     *         otherwise.
     */
    abstract public boolean areDebugTracesPromoted();

    /**
     * Initializes this log service.
     * @param  props   configuration data or <code>null</code>.
     */
    abstract public void init(Properties props);

    /**
     * Shuts this log service down.
     */
    abstract public void shutdown();

    //-------------------------------------------------------------------------
    // Factory methods
    //-------------------------------------------------------------------------

    /**
     * Returns an instance of the configured <code>LogService</code>.
     * <p>
     * This method uses the {@link ServiceLoader JAR service provider}
     * mechanism to discover the LogService implementation. The
     * implementation selected is the first listed in the first file
     * named
     * <code>META-INF/services/org.datalift.fwk.log.LogService</code>
     * found in the classpath.</p>
     *
     * @return an instance of <code>LogService</code>.
     */
    public static LogService getInstance() {
        if (instance == null) {
            selectAndConfigure(null);
        }
        return instance;
    }

    /**
     * Selects and configures a log service from the configured
     * implementations and properties.
     * @param  props   the properties to configure the log service.
     *
     * @return a log service.
     * @throws IllegalStateException if no suitable log service
     *         implementation was found.
     */
    public static LogService selectAndConfigure(Properties props) {
        for (LogService s : ServiceLoader.load(LogService.class)) {
            try {
                s.init(props);
                instance = s;
                break;
            }
            catch (Exception e) {
                System.err.println("Failed to initialize LogService provider "
                                   + s.toString());
            }
        }
        if (instance == null) {
            throw new IllegalStateException(
                        "No provider found for " + LogService.class.getName());
        }
        return instance;
    }
}
