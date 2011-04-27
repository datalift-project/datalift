package org.datalift.fwk.rdf;


import java.net.URL;
import java.util.Map;

import org.openrdf.query.Dataset;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;

import org.datalift.fwk.TechnicalException;


/**
 * A DataLift RDF repository.
 * <p>
 * This class provides helper methods to query the repository and
 * delegate result processing to handler object.</p>
 * <p>
 * Initial variable bindings can be specified. The mapping between Java
 * types and RDF data types is the following:</p>
 * <dl>
 *  <dt>{@link java.net.URI}, {@link URL}</dt><dd>URI</dd>
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
    /** The repository URL. */
    public final URL url;
    /** The repository display label. */
    public final String label;
    /** The login, for secured repositories. */
    protected final String username;
    /** The password, for secured repositories. */
    protected final String password;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Build a new repository.
     * @param  name       the repository name in DataLift configuration.
     * @param  url        the repository URL.
     * @param  username   the login or <code>null</code> if no
     *                    authentication is required.
     * @param  password   the password or <code>null</code> if no
     *                    authentication is required.
     * @param  label      the repository display label. If
     *                    <code>null</code>, <code>name</code> is used.
     *
     * @throws IllegalArgumentException if either <code>name</code> or
     *         <code>url</code> is null.
     * @throws RuntimeException if any error occurred connecting the
     *         repository.
     */
    public Repository(String name, URL url,
                      String username, String password, String label) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("name");
        }
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        this.name = name;
        this.url  = url;
        this.username = username;
        this.password = password;
        this.label    = ((label != null) && (label.length() != 0))? label: name;
    }

    //-------------------------------------------------------------------------
    // Repository contract definition
    //-------------------------------------------------------------------------

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
     * Returns the URL of the repository.
     * @return the URL of the repository.
     */
    public URL getUrl() {
        return this.url;
    }

    /**
     * Returns the display label for the repository.
     * @return the display label for the repository.
     */
    public String getLabel() {
        return this.label;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        boolean equals = false;
        if (o instanceof Repository) {
            Repository r = (Repository)o;
            equals = (this.url.equals(r.url)) &&
                     (((this.username == null) && (r.username == null)) ||
                      ((this.username != null)
                                        && (this.username.equals(r.username))));
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
        return this.name + ": " + this.url.toString();
    }
}
