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

package org.datalift.fwk.rdf;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Helper class to ease logging of SPARQL queries by providing a
 * {@link #toString() shortened description} of the query text,
 * without namespace prefixes declarations and a possibly truncated
 * query body.
 *
 * @author lbihanic
 */
public class QueryDescription
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static Pattern QUERY_START_PATTERN = Pattern.compile(
                    "SELECT|CONSTRUCT|ASK|DESCRIBE", Pattern.CASE_INSENSITIVE);
    private final static int DEF_QUERY_DESC_LENGTH = 128;

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The full text of the SPARQL query. */
    public final String query;
    /** The maximum length of the query description to display. */
    private final int maxLength;
    /** Whether to normalize spaces when displaying the SPARQL query. */
    private final boolean normalize;
    /** The shortened description of the SPARQL query. */
    private String desc = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a SPARQL query logging helper that normalizes the
     * query text and limits it to the first 128 characters.
     * @param  query   the SPARQL query to wrap.
     */
    public QueryDescription(String query) {
        this(query, DEF_QUERY_DESC_LENGTH, true);
    }

    /**
     * Creates a SPARQL query logging helper that limits the query
     * text to the first 128 characters.
     * @param  query       the SPARQL query to wrap.
     * @param  normalize   whether to normalize spaces.
     */
    public QueryDescription(String query, boolean normalize) {
        this(query, DEF_QUERY_DESC_LENGTH, normalize);
    }

    /**
     * Creates a SPARQL query logging helper wrapping the specified
     * query.
     * @param  query       the SPARQL query to wrap.
     * @param  max         the maximum length of the query
     *                     description.
     * @param  normalize   whether to normalize spaces.
     */
    public QueryDescription(String query, int max, boolean normalize) {
        this.query = query;
        this.maxLength = max;
        this.normalize = normalize;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (this.desc == null) {
            this.desc = getQueryDesc(this.query,
                                     this.maxLength, this.normalize);
        }
        return this.desc;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns a shortened description of the specified SPARQL query:
     * spaces in the query text are normalized and the description
     * length is limited to the first 128 characters.
     * @param  query       the SPARQL query.
     *
     * @return a shortened description of the specified SPARQL query.
     */
    public final static String getQueryDesc(String query) {
        return getQueryDesc(query, DEF_QUERY_DESC_LENGTH, true);
    }

    /**
     * Returns a shortened description of the specified SPARQL query,
     * limited to the first 128 characters.
     * @param  query       the SPARQL query.
     * @param  normalize   whether to normalize spaces.
     *
     * @return a shortened description of the specified SPARQL query.
     */
    public final static String getQueryDesc(String query, boolean normalize) {
        return getQueryDesc(query, DEF_QUERY_DESC_LENGTH, normalize);
    }

    /**
     * Returns a shortened description of the specified SPARQL query,
     * stripping the namespace prefixes declarations and possibly
     * truncating the query body.
     * @param  query       the SPARQL query.
     * @param  max         the maximum length of the query
     *                     description.
     * @param  normalize   whether to normalize spaces.
     *
     * @return a shortened description of the specified SPARQL query.
     */
    public final static String getQueryDesc(String query,
                                            int max, boolean normalize) {
        String desc = "";
        if (query != null) {
            // Strip prefix declarations.
            Matcher m = QUERY_START_PATTERN.matcher(query);
            if (m.find()) {
                query = query.substring(m.start());
            }
            if (normalize) {
                // Normalize query string.
                query = query.replaceAll("\\s+", " ");
            }
            // Get the N first chars of the query string, minus prefixes.
            desc = ((max > 3) && (query.length() > max))?
                                    query.substring(0, max - 3) + "...": query;
        }
        return desc;
    }
}
