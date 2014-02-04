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

package org.datalift.core;


import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.datalift.core.i18n.jersey.PreferredLocalesProvider;
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

    // Class-level booleans to prevent multiple logging of registered
    // JAX-RS resources as Jersey invokes several times the getClasses()
    // and getSingletons() methods at startup.
    private static volatile boolean classesLogged   = false;
    private static volatile boolean singletonLogged = false;

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
        // The PreferredLocalesProvider makes the user's preferred locales
        // (PreferredLocales) available for Jersey context injection.
        classes.add(PreferredLocalesProvider.class);

        if (! classesLogged) {
            log.debug("Registered {} resource classes/providers: {}",
                                Integer.valueOf(classes.size()), classes);
            classesLogged = true;
        }
        return classes;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Object> getSingletons() {
        Set<Object> resources = ApplicationLoader.getDefault().getResources();

        if (! singletonLogged) {
            log.debug("Registered {} singleton resources/providers: {}",
                                Integer.valueOf(resources.size()), resources);
            singletonLogged = true;
        }
        return resources;
    }
}
