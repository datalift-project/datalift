package org.datalift.core;


import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.datalift.core.velocity.jersey.VelocityTemplateProcessor;
import org.datalift.fwk.log.Logger;


/**
 * The DataLift bootstrap class for JAX-RS.
 * <p>
 * This class simply returns the JAX-RS resources that are loaded
 * by the {@link ApplicationLoader} which is the actual class where
 * the DataLift application initialization takes place.</p>
 *
 * @author hdevos
 */
public class DataliftApplication extends Application
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // JAX-RS Application contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // A VelocityTemplateProcessor instance shall be created for
        // each request as it accesses the HTTP request context.
        classes.add(VelocityTemplateProcessor.class);

        log.info("Registered {} resource classes: {}",
                                Integer.valueOf(classes.size()), classes);
        return classes;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Object> getSingletons() {
        Set<Object> resources = ApplicationLoader.getResources();
        log.info("Registered {} singleton resources: {}",
                                Integer.valueOf(resources.size()), resources);
        return resources;
    }
}
