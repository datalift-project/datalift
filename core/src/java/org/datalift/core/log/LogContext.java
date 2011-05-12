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
