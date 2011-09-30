package org.datalift.core.i18n.jersey;


import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
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
import org.datalift.fwk.util.StringUtils;


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
public class PreferredLocalesProvider
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
        List<Locale> locales = new LinkedList<Locale>();

        // Get acceptable locales from HTTP request.
        if (request != null) {
            locales.addAll(request.getAcceptableLanguages());
        }
        if (locales.isEmpty()) {
            // Not processing an HTTP request. => Get user locales from JVM.
            Locale l = Locale.getDefault();
            locales.add(l);
            // If a variant is present, add a locale without it.
            String s = l.getVariant();
            if (! StringUtils.isBlank(s)) {
                locales.add(new Locale(l.getLanguage(), l.getCountry()));
            }
            // If a country is present, add a locale without it. 
            s = l.getCountry();
            if (! StringUtils.isBlank(s)) {
                locales.add(new Locale(l.getLanguage()));
            }
            // Add English default locales.
            locales.add(Locale.US);
            locales.add(Locale.ENGLISH);
        }
        // Add empty locale for accessing default bundle (no locale suffix).
        locales.add(Locale.ROOT);

        return new PreferredLocales(locales);
    }
}
