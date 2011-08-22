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

package org.datalift.fwk.sparql;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.datalift.fwk.log.Logger;


/**
 * A utility class to load SPARQL queries from properties file and
 * automatically prepend a list of common prefix mappings.
 *
 * @author lbihanic
 */
public class SparqlQueries
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Default SPARQL queries definition file name. */
    public final static String QUERIES_DEFAULT_FILE =
                                                "sparql-queries.properties";
    /** The property key for the list of common prefix mappings. */
    public final static String PREFIX_MAPPINGS_PROPERTY = "prefixMappings";

    //-------------------------------------------------------------------------
    // Class member definitions
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The prefix mappings, formatted. */
    private final String namespacePrefixes;
    /** The defined SPARQL queries. */
    private final Map<String,String> queries;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Loads SPARQL queries from the
     * {@link SparqlQueries#QUERIES_DEFAULT_FILE default queries file}
     * on behalf of the specified object.
     * <p>
     * This method is a shortcut for
     * <code>new SparqlQueries(owner.getClass())</code>.</p>
     * @param  owner   the object owning the queries, used to resolve
     *                 the properties file path in the classpath.
     *
     * @throws NullPointerException if <code>owner</code> is
     *         <code>null</code>.
     * @throws RuntimeException if any error occurred accessing the
     *         properties file or loading the queries.
     *
     * @see    #SparqlQueries(String, Object)
     * @see    #QUERIES_DEFAULT_FILE
     */
    public SparqlQueries(Object owner) {
        this(QUERIES_DEFAULT_FILE, owner.getClass());
    }
    /**
     * Loads SPARQL queries from the
     * {@link SparqlQueries#QUERIES_DEFAULT_FILE default queries file}
     * on behalf of the specified class.
     * @param  owner   the class owning the queries, used to resolve
     *                 the properties file path in the classpath.
     *
     * @throws NullPointerException if <code>owner</code> is
     *         <code>null</code>.
     * @throws RuntimeException if any error occurred accessing the
     *         properties file or loading the queries.
     *
     * @see    #SparqlQueries(String, Class)
     * @see    #QUERIES_DEFAULT_FILE
     */
    public SparqlQueries(Class<?> owner) {
        this(QUERIES_DEFAULT_FILE, owner);
    }

    /**
     * Loads SPARQL queries on behalf of the specified object.
     * <p>
     * This method is a shortcut for
     * <code>new SparqlQueries(path, owner.getClass())</code>.</p>
     * @param  path    the path to the properties file containing the
     *                 SPARQL queries.
     * @param  owner   the object owning the queries, used to resolve
     *                 the properties file path in the classpath.
     *
     * @throws NullPointerException if either <code>path</code> or
     *         <code>owner</code> is <code>null</code>.
     * @throws RuntimeException if any error occurred accessing the
     *         properties file or loading the queries.
     *
     * @see    #SparqlQueries(String, Class)
     */
    public SparqlQueries(String path, Object owner) {
        this(path, owner.getClass());
    }

    /**
     * Loads SPARQL queries on behalf of the specified class.
     * @param  path    the path to the properties file containing the
     *                 SPARQL queries.
     * @param  owner   the class owning the queries, used to resolve
     *                 the properties file path in the classpath.
     *
     * @throws NullPointerException if either <code>path</code> or
     *         <code>owner</code> is <code>null</code>.
     * @throws RuntimeException if any error occurred accessing the
     *         properties file or loading the queries.
     */
    public SparqlQueries(String path, Class<?> owner) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }
        if (owner == null) {
            throw new IllegalArgumentException("owner");
        }
        try {
            // Load SPARQL query definitions
            InputStream src = owner.getResourceAsStream(path);
            if (src== null) {
                throw new RuntimeException(new FileNotFoundException(path));
            }
            Properties p = new Properties();
            p.load(src);
            // Get default prefix mappings
            this.namespacePrefixes = parsePrefixDecl(
                                (String)(p.remove(PREFIX_MAPPINGS_PROPERTY)));
            // Build queries
            Map<String,String> queries = new TreeMap<String,String>();
            for (Object o : p.keySet()) {
                String key = (String)o;
                if ((key != null) && (key.length() != 0)) {
                    String query = p.getProperty(key);
                    if ((query != null) && (query.length() != 0)) {
                        queries.put(key, namespacePrefixes + query);
                    }
                    else {
                        log.warn("No SPARQL query defined for {}", key);
                    }
                }
                // Else: ignore...
            }
            this.queries = queries;
            if (this.queries.isEmpty()) {
                log.warn("No query definitions found in \"{}\"", path);
            }
            else {
                log.debug("Loaded {} queries from \"{}\" for {}",
                          Integer.valueOf(this.queries.size()), path, owner);
            }
        }
        catch (Exception e) {
            log.error("SPARQL queries definitions ({}) loading failed: {}", e,
                      path, e);
            throw new RuntimeException(e);
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns a SPARQL query.
     * @param  key   the name of the query
     *
     * @return the SPARQL query string.
     * @throws IllegalArgumentException if <code>key</code> is unknown.
     */
    public String get(String key) {
        if (! this.queries.containsKey(key)) {
            throw new IllegalArgumentException(key + " unknown");
        }
        return this.queries.get(key);
    }

    /**
     * Parses and reconstructs the default prefix declarations for
     * all loaded SPARQL queries.
     * @param  s   the prefix declarations to parse, maybe
     *             <code>null</code> or empty.
     *
     * @return the reconstructed prefix declarations, with a single
     *         prefix declaration per line ant trimmed spaces.
     */
    private String parsePrefixDecl(String s) {
        StringBuilder buf = new StringBuilder();

        if ((s != null) && (s.trim().length() != 0)) {
            Matcher m = Pattern.compile("PREFIX\\s+(.+?)\\s*:\\s*<(.+?)>\\s*")
                               .matcher(s);
            while (m.find()) {
                buf.append("PREFIX ")
                   .append(m.group(1)).append(": <")
                   .append(m.group(2)).append(">\n");
            }
            buf.append('\n');
        }
        return buf.toString();
    }
}
