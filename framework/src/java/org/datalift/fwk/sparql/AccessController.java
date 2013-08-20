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


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.StringUtils;


/**
 * A standard interface for modules controlling access to the data
 * present in the Datalift RDF stores.
 *
 * @author lbihanic
 */
public interface AccessController
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Name of the user context variable in the access control ASK queries. */
    public final static String USER_CONTEXT_VARIABLE = "context";

    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------

    /**
     * Returns whether access control restrictions apply on the
     * specified RDF store.
     * @param  repository   the RDF store.
     * @return <code>true</code> if access control restrictions apply
     *         on <code>repository</code>; <code>false</code> otherwise.
     */
    public boolean isSecured(Repository repository);

    /**
     * Ensures the specified query complies with the access control
     * rules defined for the specified repository.
     * @param  query              the SPARQL query.
     * @param  repository         the target RDF store.
     * @param  defaultGraphUris   the URIs of the default graphs on which
     *                            evaluating the query, if any.
     * @param  namedGraphUris     the URIs of the named graphs on which
     *                            evaluating the query, if any.
     *
     * @return A {@link ControlledQuery} object with the
     *         <code>query</code>, <code>defaultGraphUris</code> and
     *         <code>namedGraphUris</code> updated to reflect the
     *         access restrictions on the specified RDF store.
     * @throws SecurityException if the user is not allowed to perform
     *         SPARQL queries on the specified RDF store.
     */
    public ControlledQuery checkQuery(String query, Repository repository,
                                      List<String> defaultGraphUris,
                                      List<String> namedGraphUris)
                                                    throws SecurityException;

    /**
     * Notifies this access controller that updates have occurred on
     * one of the (public) Datalift RDF stores.
     */
    public void refresh();

    //-------------------------------------------------------------------------
    // ControlledQuery nested class
    //-------------------------------------------------------------------------

    /**
     * Result of access control evaluation of a SPARQL query.
     */
    public class ControlledQuery
    {
        /**
         * The SPARQL query, potentially updated to include access
         * control restrictions.
         */
        public final String query;
        /** The query type: ASK, CONSTRUCT, DESCRIBE, SELECT, UPDATE... */
        public final String queryType;
        /**
         * The URIs of the default graphs on which evaluating the query,
         * potentially modified to enforce access control restrictions.
         */
        public final List<String> defaultGraphUris;
        /**
         * The URIs of the named graphs on which evaluating the query,
         * potentially modified to enforce access control restrictions.
         */
        public final List<String> namedGraphUris;
        /**
         * The named graphs accessible to the current user in the target
         * RDF store.
         */
        public final Collection<String> accessibleGraphs;

        /**
         * Creates a new ControlledQuery object.
         * @param  query              the (updated) SPARQL query.
         * @param  defaultGraphUris   the (modified) list of default
         *                            graph URIs, if any.
         * @param  namedGraphUris     the (modified) list of named
         *                            graph URIs, if any.
         * @param  accessibleGraphs   The named graphs accessible to
         *                            the user.
         */
        public ControlledQuery(String query, String type,
                               List<String> defaultGraphUris,
                               List<String> namedGraphUris,
                               Collection<String> accessibleGraphs) {
            if (StringUtils.isBlank(query)) {
                throw new IllegalArgumentException("query");
            }
            this.query = query;
            this.queryType = type;
            this.defaultGraphUris = (defaultGraphUris != null)?
                                defaultGraphUris: new LinkedList<String>();
            this.namedGraphUris   = (namedGraphUris != null)?
                                namedGraphUris: new LinkedList<String>();
            if (accessibleGraphs == null) {
                accessibleGraphs = Collections.emptySet();
            }
            this.accessibleGraphs = 
                        Collections.unmodifiableCollection(accessibleGraphs);
        }
    }
}
