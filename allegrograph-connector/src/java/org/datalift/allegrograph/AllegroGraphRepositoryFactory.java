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

package org.datalift.allegrograph;


import java.net.URI;
import java.util.regex.Pattern;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGServer;

import org.datalift.core.TechnicalException;
import org.datalift.core.rdf.BaseRepository;
import org.datalift.core.rdf.RepositoryFactory;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;

import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A {@link RepositoryFactory} implementation for accessing
 * <a href="http://www.franz.com/agraph/allegrograph/">AllegroGraph</a>
 * repositories.
 *
 * @author lbihanic
 */
public class AllegroGraphRepositoryFactory extends RepositoryFactory
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /**
     * The type string to {@link BaseRepository#REPOSITORY_TYPE flag}
     * AllegroGraph repositories in configuration.
     */
    public final static String ALLEGROGRAPH_REPOSITORY_TYPE = "allegrograph";

    /** The regex pattern that AllegroGraph connection URL shall comply with. */
    private final static Pattern AG_URL_PATTERN = Pattern.compile(
            "http://(.+?)/((catalogs/(\\w+?)/)??)repositories/(\\w+?)(/??)");

    //------------------------------------------------------------------------
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

        String type = configuration.getProperty(
                                        name + BaseRepository.REPOSITORY_TYPE);
        if ((ALLEGROGRAPH_REPOSITORY_TYPE.equals(type))
                                && (AG_URL_PATTERN.matcher(url).matches())) {
            try {
                r = new AllegroGraphRepository(name, url, configuration);
            }
            catch (Exception e) {
                // Repository not available or not a AllegroGraph repository.
                // => Ignore error and return null (i.e. not for me!).
                log.warn("Failed to connect to {}: {}", e, url, e.getMessage());
            }
        }
        // Else: Not an AllegroGraph repository.

        return r;
    }

    //-------------------------------------------------------------------------
    // AllegroGraphRepository nested class
    //-------------------------------------------------------------------------

    /**
     * A {@link Repository} implementation to access remote AllegroGraph
     * repositories.
     */
    public final static class AllegroGraphRepository extends BaseRepository
    {
        //---------------------------------------------------------------------
        // Constructors
        //---------------------------------------------------------------------

        /**
         * Build a new DataLift repository accessing a remote
         * AllegroGraph repository.
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
        public AllegroGraphRepository(String name, String url,
                                      Configuration configuration) {
            super(name, url, configuration);
        }

        //---------------------------------------------------------------------
        // BaseRepository contract support
        //---------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        protected org.openrdf.repository.Repository
                            newNativeRepository(Configuration configuration) {
            org.openrdf.repository.Repository r = null;

            String username = configuration.getProperty(
                                            this.name + REPOSITORY_USERNAME);
            String password = configuration.getProperty(
                                            this.name + REPOSITORY_PASSWORD);
            try {
                String catalog = null;
                String repository = null;

                URI u = new URI(url);
                // Extract the first four elements of the URL path.
                // AllegroGraph connection URL format is:
                // http://<host>:<port>/[catalogs/<catalog id>/]repositories/<repository id>
                String[] elts = u.getPath().split("/", 5);
                // Start at 1 to ignore empty string before leading '/' char.
                for (int i=1; i<elts.length; i+=2) {
                    if ("catalogs".equals(elts[i])) {
                        catalog = elts[i+1];
                    }
                    if ("repositories".equals(elts[i])) {
                        repository = elts[i+1];
                    }
                }
                String serverUrl = u.getScheme() + "://" + u.getAuthority();
                AGServer server = new AGServer(serverUrl, username, password);
                AGCatalog c = (isBlank(catalog))? server.getRootCatalog():
                                                  server.getCatalog(catalog);
                r = c.createRepository(repository);
                r.initialize();
                log.info("AllegroGraph repository successfully connected: {}",
                         this.url);
            }
            catch (Exception e) {
                throw new TechnicalException("repository.connect.error", e,
                                        this.name, this.url, e.getMessage());
            }
            return r;
        }
    }
}
