package org.datalift.fwk;


/**
 * A lifecycle interface for DataLift components that need to be
 * notified of application start-up and shutdown or need access to
 * the application configuration data.
 *
 * @author lbihanic
 */
public interface LifeCycle
{
    /**
     * Component initialization.
     * @param  configuration   the DataLift configuration.
     */
    public void init(Configuration configuration);

    /**
     * Component shutdown.
     * @param  configuration   the DataLift configuration.
     */
    public void shutdown(Configuration configuration);
}
