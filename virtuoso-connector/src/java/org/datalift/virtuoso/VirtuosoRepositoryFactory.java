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

package org.datalift.virtuoso;


import virtuoso.sesame2.driver.VirtuosoRepository;

import org.datalift.core.TechnicalException;
import org.datalift.core.rdf.BaseRepository;
import org.datalift.core.rdf.RepositoryFactory;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.StringUtils;


/**
 * A {@link RepositoryFactory} implementation for accessing
 * <a href="http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main">OpenLink
 * Virtuoso</a> repositories.
 *
 * @author lbihanic
 */
public final class VirtuosoRepositoryFactory extends RepositoryFactory
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The URL scheme for Virtuoso JDBC repositories. */
    public final static String VIRTUOSO_URL_SCHEME  = "jdbc:virtuoso:";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // RepositoryFactory contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Repository newRepository(String name, String url,
                                                 Configuration configuration) {
        Repository r = null;

        if (url.startsWith(VIRTUOSO_URL_SCHEME)) {
            try {
                r = new VirtuosoJdbcRepository(name, url, configuration);
            }
            catch (Exception e) {
                // Repository not available or not a native Sesame repository
                // (e.g. maybe a Virtuoso or AllegroGraph wrapper).
                // => Ignore error and return null (i.e. not for me!).
                log.warn("Failed to connect to {}: {}", e, url, e.getMessage());
            }
        }
        // Else: Not a Virtuoso repository.

        return r;
    }

    //-------------------------------------------------------------------------
    // VirtuosoJdbcRepository nested class
    //-------------------------------------------------------------------------

    /**
     * A {@link Repository} implementation to access remote Virtuoso
     * repositories over JDBC.
     */
    public final static class VirtuosoJdbcRepository extends BaseRepository
    {
        //---------------------------------------------------------------------
        // Constructors
        //---------------------------------------------------------------------

        /**
         * Build a new DataLift repository accessing a remote Virtuoso
         * repository over JDBC.
         * @param  name            the repository name in DataLift
         *                         configuration.
         * @param  url             the repository URL.
         * @param  configuration   the DataLift configuration.
         *
         * @throws IllegalArgumentException if either <code>name</code>
         *         or <code>configuration</code> is null.
         * @throws RuntimeException if any error occurred connecting the
         *         repository.
         */
        public VirtuosoJdbcRepository(String name, String url,
                                                 Configuration configuration) {
            super(name, url, configuration);
            // Check that repository configuration is consistent.
            final String endpointProp = name + REPOSITORY_ENDPOINT_URL;
            if (StringUtils.isBlank(configuration.getProperty(endpointProp))) {
                // Virtuoso JDBC port does not support direct SPARQL queries.
                // A dedicated SPARQL server port must be provided.
                throw new TechnicalException("configuration.missing.property",
                                             endpointProp);
            }
        }

        //---------------------------------------------------------------------
        // BaseRepository contract support
        //---------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        protected org.openrdf.repository.Repository
                            newNativeRepository(Configuration configuration) {
            org.openrdf.repository.Repository repository = null;

            String username = configuration.getProperty(
                                            this.name + REPOSITORY_USERNAME);
            String password = configuration.getProperty(
                                            this.name + REPOSITORY_PASSWORD);
            try {
                repository = new VirtuosoRepository(this.url,
                                                    username, password);
                repository.initialize();
            }
            catch (Exception e) {
                throw new TechnicalException("repository.connect.error", e,
                                        this.name, this.url, e.getMessage());
            }
            return repository;
        }
    }
}
