/*
 * Copyright / Copr. 2010-2014 Atos - Public Sector France -
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

package org.datalift.core.util.web.jersey;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.container.filter.UriConnegFilter;

import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.rdf.RdfFormat;

import static org.datalift.fwk.MediaTypes.*;


/**
 * A URI-based content negotiation filter mapping a dot-declared
 * suffix in URI to media type that is the value of the
 * <code>Accept</code> header or a language that is the value of
 * the <code>Accept-Language</code> header.
 * <p>
 * This filter may be used when the acceptable media type and
 * acceptable language need to be declared in the URI.</p>
 * <p>
 * Default mappings are provided for both
 * {@link #DEFAULT_MEDIA_EXTENSIONS media types} and
 * {@link #DEFAULT_LANGUAGE_EXTENSIONS languages}.</p>
 * 
 * @author lbihanic
 */
public class SuffixContentNegotiationFilter extends UriConnegFilter
{
    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * The default media extension mappings.
     * <dl>
     * <dt>"html"</dt>
     * <dd>{@link MediaTypes#TEXT_HTML text/html}</dd>
     * <dt>"xml"</dt>
     * <dd>{@link MediaTypes#APPLICATION_XML application/xml}</dd>
     * <dt>"json"</dt>
     * <dd>{@link MediaTypes#APPLICATION_JSON application/json}</dd>
     * <dt>"csv"</dt>
     * <dd>{@link MediaTypes#TEXT_CSV text/csv}</dd>
     * <dt>"atom"</dt>
     * <dd>{@link MediaTypes#APPLICATION_ATOM_XML application/atom+xml}</dd>
     * <dt>"svg"</dt>
     * <dd>{@link MediaTypes#APPLICATION_SVG_XML application/svg+xml}</dd>
     * </dl>
     * <p>
     * and all the media types defined in {@link RdfFormat}...</p>
     */
    public final static  Map<String, MediaType> DEFAULT_MEDIA_EXTENSIONS;
    /**
     * The default language extension mappings.
     * <ul>
     * <li>en</li>
     * <li>fr</li>
     * <li>de</li>
     * <li>es</li>
     * <li>it</li>
     * <li>jp</li>
     * </ul>
     */
    public final static Map<String, String> DEFAULT_LANGUAGE_EXTENSIONS;

    /** The generic media extension mappings, as a raw array. */
    private final static String[][] DEFAULT_MEDIA_MAPPINGS = new String[][] {
            { "html", TEXT_HTML },
            { "xml",  APPLICATION_XML },
            { "json", APPLICATION_JSON },
            { "csv",  TEXT_CSV },
            { "atom", APPLICATION_ATOM_XML },
            { "svg",  APPLICATION_SVG_XML } };
    /** The default language extension mappings, as a raw array. */
    private final static String[][] DEFAULT_LANGUAGE_MAPPINGS = new String[][] {
            { "en", "en" }, { "fr", "fr" }, { "de", "de" },
            { "es", "es" }, { "it", "it" }, { "jp", "jp" } };

    // ------------------------------------------------------------------------
    // Class initialization
    // ------------------------------------------------------------------------

    static {
        // Populate the default media extension mappings.
        Map<String, MediaType> mediaExtensions =
                                            new HashMap<String, MediaType>();
        // Generic media types.
        for (String[] mapping : DEFAULT_MEDIA_MAPPINGS) {
            mediaExtensions.put(mapping[0], valueOf(mapping[1]));
        }
        // RDF media types.
        for (RdfFormat fmt : RdfFormat.values()) {
            if (fmt.canOutput()) {
                for (String ext : fmt.extensions) {
                    mediaExtensions.put(ext, fmt.getMimeType());
                }
            }
        }
        DEFAULT_MEDIA_EXTENSIONS =
                            Collections.unmodifiableMap(mediaExtensions);
        // Populate the default language extension mappings.
        Map<String, String> languageExtensions = new HashMap<String, String>();
        for (String[] mapping : DEFAULT_LANGUAGE_MAPPINGS) {
            languageExtensions.put(mapping[0], mapping[1]);
        }
        DEFAULT_LANGUAGE_EXTENSIONS =
                            Collections.unmodifiableMap(languageExtensions);
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a request filter with the default
     * {@link #DEFAULT_MEDIA_EXTENSIONS suffix-to-media type mappings} and
     * {@link #DEFAULT_LANGUAGE_EXTENSIONS suffix-to-language mappings}.
     */
    public SuffixContentNegotiationFilter() {
        this(DEFAULT_MEDIA_EXTENSIONS, DEFAULT_LANGUAGE_EXTENSIONS);
    }

    /**
     * Creates a request filter with the specified suffix-to-media
     * type mappings.
     * @param  mediaExtentions   the suffix to media type mappings.
     */
    protected SuffixContentNegotiationFilter(
                                    Map<String, MediaType> mediaExtentions) {
        super(mediaExtentions);
    }

    /**
     * Creates a request filter with the specified suffix-to-media
     * type mappings and suffix-to-language mappings.
     * 
     * @param mediaExtentions      the suffix to media type mappings.
     * @param languageExtentions   the suffix to language mappings.
     */
    protected SuffixContentNegotiationFilter(
                                    Map<String, MediaType> mediaExtentions,
                                    Map<String, String> languageExtentions) {
        super(mediaExtentions, languageExtentions);
    }
}
