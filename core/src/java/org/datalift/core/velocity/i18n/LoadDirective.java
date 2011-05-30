package org.datalift.core.velocity.i18n;


import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.StringUtils;

import com.sun.jersey.api.core.HttpRequestContext;


public class LoadDirective extends Directive
{
    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Directive contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @return The name of this directive: "<code>load</code>".
     */
    @Override
    public String getName() {
        return "load";
    }

    /**
     * {@inheritDoc}
     * @return The directive type: {@link LINE}.
     */
    @Override
    public int getType() {
        return LINE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node) 
                        throws IOException, ResourceNotFoundException,
                               ParseErrorException, MethodInvocationException {
        // Get user locales.
        List<Locale> locales = null;
        HttpRequestContext httpRequest =
                                (HttpRequestContext)(context.get("request"));
        if (httpRequest != null) {
            // Get acceptable locales from HTTP request.
            locales = new ArrayList<Locale>(httpRequest.getAcceptableLanguages());
        }
        if ((locales == null) || (locales.isEmpty())) {
            // Not processing an HTTP request. => Get user locales from JVM.
            locales = new ArrayList<Locale>();
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
        // Add default bundle (no locale).
        locales.add(Locale.ROOT);
        // Reverse locale list to get least wanted locales first.
        Collections.reverse(locales);
        // Get existing bundle list, to add new bundles.
        BundleList bundleList = (BundleList)(context.get(BundleList.KEY));
        if (bundleList == null) {
            bundleList = new BundleList();
            context.put(BundleList.KEY, bundleList);
        }

        // Load requested bundles to add them to the bundle list.
        for (int i=0; i<node.jjtGetNumChildren(); i++) {
            String bundleName = String.valueOf(node.jjtGetChild(i).value(context));

//          // Get module name.
//          String templateName = context.getCurrentResource().getName();
//          int start = (templateName.charAt(0) == '/')? 1: 0;
//          int sep   = templateName.indexOf('/', start);
//          String module =
//                  (sep != -1)? templateName.substring(start, sep + 1): "";
//          bundleName = module + bundleName;

            Properties bundleData = null;
            for (Locale locale : locales) {
                // Build properties resource bundle name for locale.
                StringBuilder buf = new StringBuilder(bundleName);
                if (locale != Locale.ROOT) {
                    buf.append('_').append(locale);
                }
                String propsName = buf.append(".properties").toString();

                if (this.rsvc.getLoaderNameForResource(propsName) != null) {
                    try {
                        Object o = this.rsvc.getContent(propsName,
                                                        "ISO-8859-1").getData();
                        Properties p = new Properties(bundleData);
                        p.load(new StringReader((String)o));
                        bundleData = p;
                    }
                    catch (Exception e) {
                        log.error("Failed to load resource bundle {}", e,
                                  propsName);
                    }
                }
                // Else: Properties resource bundle not found. => Ignore...
            }
            if (bundleData != null) {
                bundleList.addProperties(bundleData);
            }
            else {
                log.warn("Can't find bundle {}", bundleName);
            }
        }
        return true;
    }
}
