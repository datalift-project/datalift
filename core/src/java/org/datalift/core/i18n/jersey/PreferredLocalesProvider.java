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

package org.datalift.core.i18n.jersey;


import java.lang.reflect.Type;
import java.util.Locale;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

import org.datalift.fwk.i18n.PreferredLocales;


/**
 * A Jersey provider supplying an ordered list of the user's preferred
 * locales. The locales are retrieved from the HTTP request
 * <code>Accept-Language</code> header if such a request is being
 * processed or from the
 * {@link Locale#getDefault() JVM runtime environment}.
 * <p>
 * The {@link PreferredLocales} can be injected in JAX-RS resources or
 * resource methods using the @{@link Context} annotation.</p>
 *
 * @author lbihanic
 */
@Provider
public final class PreferredLocalesProvider
                    extends AbstractHttpContextInjectable<PreferredLocales>
                    implements InjectableProvider<Context, Type>
{
    //-------------------------------------------------------------------------
    // InjectableProvider contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Injectable<PreferredLocales> getInjectable(ComponentContext ic,
                                                      Context a, Type c) {
        return (c.equals(PreferredLocales.class))? this: null;
    }

    /** {@inheritDoc} */
    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    //-------------------------------------------------------------------------
    // AbstractHttpContextInjectable contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public PreferredLocales getValue(HttpContext context) {
        return getPreferredLocales(context.getRequest());
    }

    //------------------------------------------------------------------------
    // Specific implementation
    //------------------------------------------------------------------------

    /**
     * Extracts the user's preferred locales from the specified HTTP
     * request (<code>Accept-Language</code> header) and, if none is
     * available, from the
     * {@link Locale#getDefault() JVM runtime environment}.
     * @param  request    the JAX-RS HTTP request.
     *
     * @return the list of user's preferred locales, most preferred
     *         first.
     */
    public static PreferredLocales getPreferredLocales(
                                                HttpRequestContext request) {
        PreferredLocales locales = PreferredLocales.get();
        if ((locales == null) && (request != null)) {
            locales = PreferredLocales.set(request.getAcceptableLanguages());
        }
        return locales;
    }
}
