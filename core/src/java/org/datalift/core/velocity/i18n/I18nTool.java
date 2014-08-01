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

package org.datalift.core.velocity.i18n;

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.tools.config.DefaultKey;

import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;


/**
 * Velocity tool for substituting an internationalized message
 * identifier with the best available translation
 * {@link LoadDirective loaded from resource bundles} based on the
 * user's preferred locales retrieved from the user's session, an
 * HTTP query parameter or the HTTP <code>Accept-Language</code>
 * header sent by the Web browser.
 * <p>
 * Compared to the {@link I18nDirective corresponding directive},
 * this tool provides several advantages:</p>
 * <ul>
 *  <li>Usable everywhere in Velocity templates, e.g. to assign
 *   variables</li>
 *  <li>Helper methods to directly escape the formatted text
 *   without need to use Velocity Escape tool</li>
 *  <li>Accessor methods to user's locale information, e.g. language
 *   to set the <code>lang</code> attribute of the &lt;html&gt; tag</li>
 * </ul>
 * 
 * @author lbihanic
 */
@DefaultKey(I18nTool.KEY)
public class I18nTool
{
    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** The key in Velocity context for the internationalization tool. */
    public static final String KEY = "i18n";

    // ------------------------------------------------------------------------
    // Class members
    // ------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger();

    // ------------------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------------------

    /** The available resource bundles, loaded by the template. */
    private final BundleList bundles = new BundleList();

    // ------------------------------------------------------------------------
    // I18nTool contract definition
    // ------------------------------------------------------------------------

    /**
     * Returns the user's preferred locale.
     * @return the user's preferred {@link Locale locale}.
     */
    public Locale getLocale() {
	Locale locale = null;

	List<Locale> l = PreferredLocales.get();
	if ((l != null) && (! l.isEmpty())) {
	    locale = l.get(0);
	}
	return locale;
    }

    /**
     * Returns the language code from the
     * {@link #locale() user's preferred locale}.
     * @return the language code from the user's preferred locale or
     *         <code>en</code> (English) if no locale information is
     *         available.
     */
    public String getLanguage() {
	String lang = "en";

	Locale l = getLocale();
	if (l != null) {
	    lang = l.getLanguage();
	}
	return lang;
    }

    /**
     * Returns the country code from the
     * {@link #locale() user's preferred locale}.
     * @return the country code from the user's preferred locale or
     *         an empty string if no locale information is available
     *         or the locale has no country code.
     */
    public String getCountry() {
	String c = "";

	Locale l = getLocale();
	if (l != null) {
	    c = l.getCountry();
	}
	return c;
    }

    /**
     * Formats the specified internationalized message with the
     * specified arguments.
     * @param  key    the message identifier.
     * @param  args   the message arguments.
     * 
     * @return the formatted message.
     */
    public String format(String key, Object... args) {
	String msg = key;

	if (! this.bundles.isEmpty()) {
	    // Get message text from bundles.
	    msg = this.bundles.getValue(key);
	    if (msg == null) {
		// No entry found. => Use key as message format.
		msg = key;
	    }
	    final int params = (args != null)? args.length: 0;
	    if ((params > 0) && (msg != null) && (msg.indexOf('{') != -1)) {
		// Replaces message format parameters with arguments.
		MessageFormat fmt = new MessageFormat(msg);
		// Use user's preferred locale when formatting dates, numbers...
                Locale l = this.getLocale();
		if (l != null) {
		    fmt.setLocale(l);
		}
		msg = fmt.format(args);
	    }
	    log.trace("I18N: Rendered \"{}\" -> \"{}\"", key, msg);
	} else {
	    log.warn("I18N: Failed to resolved key \"{}\"" +
		     ": no bundle defined in template", key);
	}
	return msg;
    }

    /**
     * Formats the specified internationalized message with the
     * specified arguments and escape the resulting string using HTML
     * entities.
     * @param  key    the message identifier.
     * @param  args   the message arguments.
     *
     * @return the formatted message, escaped for inserting in an HTML
     *         page.
     */
    public String html(String key, Object... args) {
        String s = this.format(key, args);
        return (s != null)? StringEscapeUtils.escapeHtml(s): null;
    }

    /**
     * Formats the specified internationalized message with the
     * specified arguments and escape the resulting string using
     * JavaScript String rules.
     * @param  key    the message identifier.
     * @param  args   the message arguments.
     *
     * @return the formatted message, escaped for inserting in
     *         Javascript code.
     */
    public String javascript(String key, Object... args) {
        String s = this.format(key, args);
        return (s != null)? StringEscapeUtils.escapeJavaScript(s): null;
    }

    /**
     * Formats the specified internationalized message with the
     * specified arguments and escape the resulting string using XML
     * entities.
     * @param  key    the message identifier.
     * @param  args   the message arguments.
     *
     * @return the formatted message, escaped for inserting in an XML
     *         document.
     */
    public String xml(String key, Object... args) {
        String s = this.format(key, args);
        return (s != null)? StringEscapeUtils.escapeXml(s): null;
    }

    /**
     * Formats the specified internationalized message with the
     * specified arguments and escape the resulting string to be
     * suitable to use as an HTTP parameter value. <br/>Uses UTF-8
     * as default character encoding.
     * @param  key    the message identifier.
     * @param  args   the message arguments.
     *
     * @return the formatted message, escaped to be suitable to use
     *         as an HTTP parameter value.
     */
    public String url(String key, Object... args) {
        String s = this.format(key, args);
        try {
            return (s != null)? URLEncoder.encode(s, "UTF-8"): null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the list of bundles loaded from the templates.
     * @return the available resource bundles, possibly an empty list.
     */
    /* package */ BundleList getBundles() {
        return this.bundles;
    }
}
