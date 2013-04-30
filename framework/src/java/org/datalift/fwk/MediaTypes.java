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

package org.datalift.fwk;


import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.core.MediaType;

import org.datalift.fwk.util.web.Charsets;


/**
 * An extension to JAX-RS {@link MediaType standard media types} with
 * RDF-related media types. Instances are immutable.
 *
 * @author lbihanic
 */
public class MediaTypes extends MediaType
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** "application/rdf+xml" */
    public final static String APPLICATION_RDF_XML = "application/rdf+xml";
    /** "application/rdf+xml" */
    public final static MediaType APPLICATION_RDF_XML_TYPE =
                                        MediaType.valueOf(APPLICATION_RDF_XML);

    /** "text/turtle" */
    public final static String TEXT_TURTLE = "text/turtle";
    /** "text/turtle" */
    public final static MediaType TEXT_TURTLE_TYPE =
                                        MediaType.valueOf(TEXT_TURTLE);
    /** "application/x-turtle", a deprecated alias for {@link #TEXT_TURTLE} */
    public final static String APPLICATION_TURTLE = "application/x-turtle";
    /**
     * "application/x-turtle", a deprecated alias for
     * {@link #TEXT_TURTLE_TYPE}
     */
    public final static MediaType APPLICATION_TURTLE_TYPE =
                                        MediaType.valueOf(APPLICATION_TURTLE);

    /** "text/n3" */
    public final static String TEXT_N3 = "text/n3";
    /** "text/n3" */
    public final static MediaType TEXT_N3_TYPE = MediaType.valueOf(TEXT_N3);
    /** "text/rdf+n3", a deprecated alias for {@link #TEXT_N3} */
    public final static String TEXT_RDF_N3 = "text/rdf+n3";
    /** "text/rdf+n3", a deprecated alias for {@link #TEXT_N3_TYPE} */
    public final static MediaType TEXT_RDF_N3_TYPE =
                                            MediaType.valueOf(TEXT_RDF_N3);
    /** "application/n3", a deprecated alias for {@link #TEXT_N3} */
    public final static String APPLICATION_N3 = "application/n3";
    /** "application/n3", a deprecated alias for {@link #TEXT_N3_TYPE} */
    public final static MediaType APPLICATION_N3_TYPE =
                                            MediaType.valueOf(APPLICATION_N3);

    /** "application/n-triples" */
    public final static String APPLICATION_NTRIPLES = "application/n-triples";
    /** "application/n-triples" */
    public final static MediaType APPLICATION_NTRIPLES_TYPE =
                                    MediaType.valueOf(APPLICATION_NTRIPLES);

    /** "application/sparql-query" */
    public final static String APPLICATION_SPARQL_QUERY =
                                            "application/sparql-query";
    /** "application/sparql-query" */
    public final static MediaType APPLICATION_SPARQL_QUERY_TYPE =
                            MediaType.valueOf(APPLICATION_SPARQL_QUERY);
    /** "application/sparql-results+xml" */
    public final static String APPLICATION_SPARQL_RESULT_XML =
                                            "application/sparql-results+xml";
    /** "application/sparql-results+xml" */
    public final static MediaType APPLICATION_SPARQL_RESULT_XML_TYPE =
                            MediaType.valueOf(APPLICATION_SPARQL_RESULT_XML);
    /** "application/sparql-results+json" */
    public final static String APPLICATION_SPARQL_RESULT_JSON =
                                            "application/sparql-results+json";
    /** "application/sparql-results+json" */
    public final static MediaType APPLICATION_SPARQL_RESULT_JSON_TYPE =
                            MediaType.valueOf(APPLICATION_SPARQL_RESULT_JSON);

    /** "application/trix" */
    public final static String APPLICATION_TRIX = "application/trix";
    /** "application/trix" */
    public final static MediaType APPLICATION_TRIX_TYPE =
                                            MediaType.valueOf(APPLICATION_TRIX);
    /** "application/x-trig" */
    public final static String APPLICATION_TRIG = "application/x-trig";
    /** "application/x-trig" */
    public final static MediaType APPLICATION_TRIG_TYPE =
                                            MediaType.valueOf(APPLICATION_TRIG);

    /** "text/csv" */
    public final static String TEXT_CSV = "text/csv";
    /** "text/csv" */
    public final static MediaType TEXT_CSV_TYPE =
                                        MediaType.valueOf(TEXT_CSV);
    /** "application/csv", a deprecated alias for {@link #TEXT_CSV}. */
    public final static String APPLICATION_CSV = "application/csv";
    /** "application/csv", a deprecated alias for {@link #TEXT_CSV_TYPE} */
    public final static MediaType APPLICATION_CSV_TYPE =
                                        MediaType.valueOf(APPLICATION_CSV);
    /**
     * "text/comma-separated-values", a deprecated alias for
     * {@link #TEXT_CSV}.
     */
    public final static String TEXT_COMMA_SEPARATED_VALUES =
                                        "text/comma-separated-values";
    /**
     * "text/comma-separated-values", a deprecated alias for
     * {@link #TEXT_CSV_TYPE}.
     */
    public final static MediaType TEXT_COMMA_SEPARATED_VALUES_TYPE =
                                MediaType.valueOf(TEXT_COMMA_SEPARATED_VALUES);

    public final static String CHARSET_PARAMETER = "charset";
    /**
     * The suffix specifying the content is UTF-8 encoded
     * in the HTTP Content-Type header.
     */
    public final static String UTF8_ENCODED =
                        "; " + CHARSET_PARAMETER + '=' + Charsets.UTF8_CHARSET;
    /** text/html; charset=utf-8 */
    public final static String TEXT_HTML_UTF8 = TEXT_HTML + UTF8_ENCODED;
    /** application/json; charset=utf-8 */
    public final static String APPLICATION_JSON_UTF8 =
                                            APPLICATION_JSON + UTF8_ENCODED;

    /** The supported RDF MIME type names. */ 
    private final static Collection<String> RDF_TYPES =
            Arrays.asList(APPLICATION_RDF_XML,
                          TEXT_TURTLE, APPLICATION_TURTLE,
                          TEXT_N3, TEXT_RDF_N3, APPLICATION_N3,
                          APPLICATION_NTRIPLES, APPLICATION_TRIG,
                          APPLICATION_TRIX);

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor, private on purpose.
     */
    private MediaTypes() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // Helpers methods
    //-------------------------------------------------------------------------

    /**
     * Returns the {@link #CHARSET_PARAMETER character set} parameter
     * for the specified media type or <code>null</code> if none was set.
     * @return the "<code>charset</code>" parameter.
     */
    public static String getCharset(MediaType type) {
        return type.getParameters().get(CHARSET_PARAMETER);
    }

    /**
     * Returns the string representation of a media type, without
     * the additional parameters.
     * @return the short string representation of the type.
     */
    public static String toString(MediaType type) {
        return toString(type, false);
    }

    /**
     * Returns the string representation of a media type.
     * @param  includeParameters   whether to include the type
     *                             parameters (charset, language...).
     * @return the string representation of the type.
     */
    public static String toString(MediaType type, boolean includeParameters) {
        return (includeParameters)? type.toString():
                                    type.getType() + '/' + type.getSubtype();
    }

    /**
     * Returns whether the specified MIME type is an RDF data type.
     * @param  type   the MIME type.
     * @return <code>true</code> if the specified MIME type is an
     *         RDF data type; <code>false</code> otherwise.
     */
    public static boolean isRdf(MediaType type) {
        return (type != null)? RDF_TYPES.contains(
                            type.getType() + '/' + type.getSubtype()): false;
    }
}
