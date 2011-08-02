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

package org.datalift.core.log;


import org.datalift.fwk.log.Logger;


/**
 * Predefined values for log diagnostics contexts.
 *
 * @see Logger#setContext(Object, Object)
 *
 * @author lbihanic
 */
public enum LogContext
{
    //-------------------------------------------------------------------------
    // Enumeration values
    //-------------------------------------------------------------------------

    User,
    Timer,
    Module,
    Resource;

    //-------------------------------------------------------------------------
    // Utility methods
    //-------------------------------------------------------------------------

    /**
     * Sets the {@link #Module} and {@link #Resource} contexts,
     * if not already set. Otherwise, the contexts are left unchanged.
     * @param  module     the new value for the {@link #Module}
     *                    context.
     * @param  resource   the new value for the {@link #Resource}
     *                    context.
     */
    public static void setContexts(Object module, Object resource) {
        Object prevCtx = Logger.setContext(Module, module);
        if (prevCtx != null) {
            Logger.setContext(Module, prevCtx);
        }
        prevCtx = Logger.setContext(Resource, resource);
        if (prevCtx != null) {
            Logger.setContext(Resource, prevCtx);
        }
    }

    /**
     * Sets the {@link #Module} and {@link #Resource} contexts.
     * @param  module     the new value for the {@link #Module}
     *                    context.
     * @param  resource   the new value for the {@link #Resource}
     *                    context.
     *
     * @return the previous context values as an array of two objects.
     */
    public static Object[] pushContexts(Object module, Object resource) {
        Object[] prevCtx = new Object[2];
        prevCtx[0] = Logger.setContext(Module, module);
        prevCtx[1] = Logger.setContext(Resource, resource);
        return prevCtx;
    }

    /**
     * Clears all log contexts.
     */
    public static void resetContexts() {
        Logger.clearContexts();
    }

    /**
     * Clears all log contexts the sets the {@link #Module},
     * {@link #Resource} and {@link #Timer} contexts.
     * @param  module     the new value for the {@link #Module}
     *                    context.
     * @param  resource   the new value for the {@link #Resource}
     *                    context.
     */
    public static void resetContexts(Object module, Object resource) {
        resetContexts();
        Logger.setContext(Module, module);
        Logger.setContext(Resource, resource);
        Logger.setContext(Timer, new TimerContext());
    }
}
