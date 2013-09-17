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

package org.datalift.core.log.log4j;


import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.apache.log4j.Priority;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;

import static org.apache.log4j.Level.*;


/**
 * A {@link java.util.logging.Handler handler} that redirects
 * java.util.logging messages to Log4J.
 * <p>
 * Adapted from
 * <a href="http://yajul.sourceforge.net/">JuliToLog4jHandler</a>.</p>
 */
public final class JulToLog4jHandler extends Handler
{
    private final LoggerRepository loggerRepository;

    private JulToLog4jHandler(LoggerRepository loggerRepository) {
        super();

        if (loggerRepository == null) {
            loggerRepository = org.apache.log4j.LogManager.getLoggerRepository();
        }
        this.loggerRepository = loggerRepository;
    }

    @Override
    public void publish(LogRecord record) {
        Logger l = this.getLogger(record);
        l.log(this.getLevel(record),
              this.getMessage(record), record.getThrown());
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        boolean loggable = this.getLogger(record).isEnabledFor(
                                                        this.getLevel(record));
        return (loggable)? super.isLoggable(record): false;
    }

    @Override
    public Level getLevel() {
        return Level.ALL;
    }

    @Override
    public void flush() {
        // Nothing to do.
    }

    @Override
    public void close() {
        // Nothing to do.
    }

    private Logger getLogger(LogRecord record) {
        return this.loggerRepository.getLogger(record.getLoggerName());
    }

    private String getMessage(LogRecord record) {
        String message = record.getMessage();
        // Format message
        try {
            Object parameters[] = record.getParameters();
            if ((parameters != null) && (parameters.length != 0)) {
                message = MessageFormat.format(message, parameters);
            }
        }
        catch (Exception e) { /* Ignore... */ }

        return message;
    }

    private Priority getLevel(LogRecord record) {
        Level level = record.getLevel();
        if (Level.SEVERE == level) {
            return ERROR;
        }
        else if (Level.WARNING == level) {
            return WARN;
        }
        else if ((Level.INFO == level) || (Level.CONFIG == level)) {
            return INFO;
        }
        else if (Level.FINE == level) {
            return DEBUG;
        }
        else if ((Level.FINER == level) || (Level.FINEST == level)) {
            return TRACE;
        }
        else if (Level.ALL == level) {
            return ALL;
        }
        return OFF;
    }

    public static void install(LoggerRepository loggerRepository) {
        java.util.logging.Logger rootLogger =
                                    LogManager.getLogManager().getLogger("");
        // Remove old handlers
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        // Add our own handler
        rootLogger.addHandler(new JulToLog4jHandler(loggerRepository));
    }

    public static void uninstall() {
        java.util.logging.Logger rootLogger =
                                    LogManager.getLogManager().getLogger("");
        // Remove our handlers
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof JulToLog4jHandler) {
                rootLogger.removeHandler(handler);
            }
        }
    }
}
