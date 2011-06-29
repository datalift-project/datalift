package org.datalift.core.rdf.sesame;


import org.openrdf.repository.Repository;
import org.openrdf.repository.http.HTTPRepository;

import org.datalift.core.TechnicalException;
import org.datalift.core.rdf.BaseRepository;
import org.datalift.fwk.Configuration;


/**
 * A Repository implementation to access remote repositories over
 * HTTP using the
 * <a href="http://www.openrdf.org/">Open RDF Sesame 2</a> API.
 *
 * @author hdevos
 */
public final class HttpRepository extends BaseRepository
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Build a new repository.
     * @param  configuration   the DataLift configuration
     * @param  name            the repository name in DataLift
     *                         configuration.
     *
     * @throws IllegalArgumentException if either <code>name</code> or
     *         <code>configuration</code> is null.
     * @throws RuntimeException if any error occurred connecting the
     *         repository.
     */
    public HttpRepository(Configuration configuration, String name) {
        super(configuration, name);
    }

    //-------------------------------------------------------------------------
    // BaseRepository contract support
    //-------------------------------------------------------------------------

    @Override
    protected Repository newNativeRepository(Configuration configuration,
                                             String name) {
        Repository repository = null;
        try {
            repository = new HTTPRepository(this.url.toString());
            repository.initialize();
        }
        catch (Exception e) {
            throw new TechnicalException("repository.connect.error", e,
                                         name, this.url, e.getMessage());
        }
        return repository;
    }
}
