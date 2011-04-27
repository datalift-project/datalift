package org.datalift.core.log;

import org.datalift.fwk.log.Logger;


/**
 * An object counting the number of seconds since its creation.
 * Instances of this class are intended to be used as
 * {@link Logger#setContext(Object, Object) diagnostic contexts}
 * to trace request execution time.
 *
 * @author lbihanic
 */
public class TimerContext
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Context creation time. */
    private final long startTime = System.currentTimeMillis();

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    @Override
    /**
     * {@inheritDoc}
     * <p>This implementation return the number of seconds (as a
     * double) since the object creation.</p>
     */
    public String toString() {
        long duration = System.currentTimeMillis() - this.startTime;
        return Double.toString(duration / 1000.0);
    }
}
