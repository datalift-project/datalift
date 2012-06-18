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

package org.datalift.fwk.rdf;


import java.util.Map;

import org.openrdf.query.Dataset;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;

import org.datalift.fwk.TechnicalException;

import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A DataLift RDF repository.
 * <p>
 * This class provides helper methods to query the repository and
 * delegate result processing to handler object.</p>
 * <p>
 * Initial variable bindings can be specified. The mapping between Java
 * types and RDF data types is the following:</p>
 * <dl>
 *  <dt>{@link java.net.URI}, {@link java.net.URL}</dt><dd>URI</dd>
 *  <dt>String, Integer, Long, Boolean, Double, Byte</dt><dd>Literal</dd>
 * </dl>
 *
 * @author hdevos
 */
public abstract class Repository
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The repository name in the DataLift configuration. */
    public final String name;
    /** The repository connection string. */
    protected final String url;
    /** The repository display label. */
    protected final String label;
    /** Whether this repository is publicly accessible. */
    protected final boolean isPublic;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Build a new repository.
     * @param  name    the repository name in DataLift configuration.
     * @param  url     the repository URL.
     * @param  label   the repository display label. If
     *                 <code>null</code>, <code>name</code> is used.
     *
     * @throws IllegalArgumentException if either <code>name</code> is
     *         <code>null</code>.
     * @throws RuntimeException if any error occurred connecting the
     *         repository.
     */
    public Repository(String name, String url, String label, boolean isPublic) {
        if (isBlank(name)) {
            name = null;
        }
        this.name     = name;
        this.url      = url;
        this.label    = (! isBlank(label))? label: name;
        this.isPublic = isPublic;
    }

    //-------------------------------------------------------------------------
    // Repository contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the internal RDF store representation.
     * @return the internal RDF store.
     * @throws TechnicalException if any error occurred accessing
     *         the RDF store.
     */
    abstract public org.openrdf.repository.Repository getNativeRepository();

    /**
     * Returns a connection to the RDF store.
     * @return a connection to the RDF store.
     * @throws TechnicalException if any error occurred accessing
     *         the RDF store.
     */
    abstract public RepositoryConnection newConnection();

    /**
     * Executes the specified ASK SPARQL query on the default graph.
     * @param  query   the ASK query.
     *
     * @return the ASK query result.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    public final boolean ask(String query) {
        return this.ask(query, null, null, null);
    }

    /**
     * Executes the specified ASK SPARQL query on the default graph
     * with a set of initial variable bindings.
     * @param  query      the ASK query.
     * @param  bindings   the initial bindings or <code>null</code> if
     *                    no variables shall be initially bound.
     *
     * @return the ASK query result.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    public final boolean ask(String query, Map<String,Object> bindings) {
        return this.ask(query, bindings, null, null);
    }

    /**
     * Executes the specified ASK SPARQL query on a specific dataset
     * with a set of initial bindings.
     * @param  query      the ASK query.
     * @param  bindings   the initial bindings or <code>null</code> if
     *                    no variables shall be initially bound.
     * @param  dataset    the specific dataset to query.
     * @param  baseUri    the base URI for relative URI resolution.
     *
     * @return the ASK query result.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    abstract public boolean ask(String query, Map<String,Object> bindings,
                                Dataset dataset, String baseUri);

    /**
     * Executes the specified SELECT SPARQL query on the default graph.
     * The query results are directly handed over to the specified
     * handler object.
     * @param  query      the SELECT query.
     * @param  handler    the query result handler.
     *
     * @throws RdfException      if any error was reported by the
     *         result handler.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    public final void select(String query, TupleQueryResultHandler handler)
                                                        throws RdfException {
        this.select(query, null, handler, null, null);
    }

    /**
     * Executes the specified SELECT SPARQL query on the default graph
     * with a set of initial bindings on a specific dataset. The query
     * results are directly handed over to the specified handler object.
     * @param  query      the SELECT query.
     * @param  bindings   the initial bindings or <code>null</code> if
     *                    no variables shall be initially bound.
     * @param  handler    the query result handler.
     *
     * @throws RdfException      if any error was reported by the
     *         result handler.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    public final void select(String query, Map<String,Object> bindings,
                             TupleQueryResultHandler handler)
                                                        throws RdfException {
        this.select(query, bindings, handler, null, null);
    }

    /**
     * Executes the specified SELECT SPARQL query on a specific dataset
     * with a set of initial bindings. The query results are directly
     * handed over to the specified handler object.
     * @param  query      the SELECT query.
     * @param  bindings   the initial bindings or <code>null</code> if
     *                    no variables shall be initially bound.
     * @param  handler    the query result handler.
     * @param  dataset    the specific dataset to query.
     * @param  baseUri    the base URI for relative URI resolution.
     *
     * @throws RdfException      if any error was reported by the
     *         result handler.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    abstract public void select(String query, Map<String,Object> bindings,
                                TupleQueryResultHandler handler,
                                Dataset dataset, String baseUri)
                                                        throws RdfException;

    /**
     * Executes the specified CONSTRUCT or DESCRIBE SPARQL query on the
     * on the default graph. The query results are directly handed over
     * to the specified handler object.
     * @param  query      the CONSTRUCT or DESCRIBE query.
     * @param  handler    the query result handler.
     *
     * @throws RdfException      if any error was reported by the
     *         result handler.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    public final void construct(String query, RDFHandler handler)
                                                        throws RdfException {
        this.construct(query, null, handler, null, null);
    }

    /**
     * Executes the specified CONSTRUCT or DESCRIBE SPARQL query on the
     * on the default graph with a set of initial bindings. The query
     * results are directly handed over to the specified handler object.
     * @param  query      the CONSTRUCT or DESCRIBE query.
     * @param  bindings   the initial bindings or <code>null</code> if
     *                    no variables shall be initially bound.
     * @param  handler    the query result handler.
     *
     * @throws RdfException      if any error was reported by the
     *         result handler.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    public final void construct(String query, Map<String,Object> bindings,
                                RDFHandler handler) throws RdfException {
        this.construct(query, bindings, handler, null, null);
    }

    /**
     * Executes the specified CONSTRUCT or DESCRIBE SPARQL query on a
     * specific dataset with a set of initial bindings. The query
     * results are directly handed over to the specified handler object.
     * @param  query      the CONSTRUCT or DESCRIBE query.
     * @param  bindings   the initial bindings or <code>null</code> if
     *                    no variables shall be initially bound.
     * @param  handler    the query result handler.
     * @param  dataset    the specific dataset to query.
     * @param  baseUri    the base URI for relative URI resolution.
     *
     * @throws RdfException      if any error was reported by the
     *         result handler.
     * @throws RdfQueryException if any error occurred executing
     *         the query.
     */
    abstract public void construct(String query, Map<String,Object> bindings,
                                   RDFHandler handler,
                                   Dataset dataset, String baseUri)
                                                        throws RdfException;

    /**
     * Returns the name of the repository in the configuration.
     * @return the name of the repository.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the connection URL to the repository.
     * @return the connection URL to the repository.
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Returns the URL of the repository SPARQL endpoint.
     * <p>
     * The default implementation returns the repository
     * {@link #getUrl() connection URL}.</p>
     * @return the URL of the repository SPARQL endpoint.
     */
    public String getEndpointUrl() {
        return this.getUrl();
    }

    /**
     * Returns the display label for the repository.
     * @return the display label for the repository.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Returns Whether this repository is publicly accessible.
     * @return <code>true</code> is this repository is publicly
     *         accessible; <code>false</code> otherwise.
     */
    public boolean isPublic() {
        return this.isPublic;
    }

    /**
     * Shuts down this repository.
     */
    abstract public void shutdown();

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        boolean equals = false;
        if (o instanceof Repository) {
            Repository r = (Repository)o;
            equals = (this.url.equals(r.url));
        }
        return equals;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.url.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return (this.name != null)? this.name + ": " + this.url: this.url;
    }
}
