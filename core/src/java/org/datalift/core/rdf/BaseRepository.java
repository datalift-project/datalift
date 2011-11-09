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

package org.datalift.core.rdf;


import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.Query;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfException;
import org.datalift.fwk.rdf.Repository;

import static org.datalift.core.DefaultConfiguration.REPOSITORY_DEFAULT_FLAG;
import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * An abstract superclass for {@link Repository} implementations based
 * on the <a href="http://www.openrdf.org/">Open RDF Sesame 2</a> API.
 *
 * @author hdevos
 */
abstract public class BaseRepository extends Repository
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The property suffix for repository display label. */
    public final static String REPOSITORY_LABEL = ".repository.label";
    /** The property suffix for repository login. */
    public final static String REPOSITORY_USERNAME = ".repository.username";
    /** The property suffix for repository password. */
    public final static String REPOSITORY_PASSWORD = ".repository.password";
    /** The property suffix for repository visibility flag. */
    public final static String REPOSITORY_PUBLIC_FLAG = ".repository.public";
    /**
     * The property suffix for the URL of the repository SPARQL
     * endpoint. If the property is not set, the repository
     * {@link #getUrl() connection URL} is assume to support the
     * <a href="http://www.w3.org/TR/sparql11-protocol/#query-bindings-http">SPARQL
     * protocol HTTP interface</a>.
     */
    public final static String REPOSITORY_ENDPOINT_URL =
                                                    ".repository.sparql.url";
    /** The property suffix for repository type. */
    public final static String REPOSITORY_TYPE = ".repository.type";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The URL of the repository SPARQL endpoint. */
    private final String endpointUrl;
    /** The native repository. */
    private final org.openrdf.repository.Repository target;
    /** The value factory to map initial bindings. */
    private final ValueFactory valueFactory;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Build a new repository.
     * @param  name            the repository name in DataLift
     *                         configuration.
     * @param  url             the repository URL.
     * @param  configuration   the DataLift configuration.
     *
     * @throws IllegalArgumentException if either <code>name</code> or
     *         <code>configuration</code> is null.
     * @throws RuntimeException if any error occurred connecting the
     *         repository.
     */
    protected BaseRepository(String name, String url,
                                          Configuration configuration) {
        // Read repository connection URL and display label. If label is
        // absent from configuration, use the label property name as key
        // for retrieving internationalized text from resource bundles.
        super(name, url, configuration.getProperty(name + REPOSITORY_LABEL,
                                                   name + REPOSITORY_LABEL),
                         parseAccessControlFlag(name, configuration));
        if (isBlank(this.url)) {
            throw new TechnicalException("repository.invalid.url",
                                                        this.name, this.url);
        }
        String sparqlEndpoint = configuration.getProperty(
                                                name + REPOSITORY_ENDPOINT_URL);
        this.endpointUrl = (isBlank(sparqlEndpoint))? this.url: sparqlEndpoint;
        this.target = this.newNativeRepository(configuration);
        this.valueFactory = this.target.getValueFactory();
    }

    //-------------------------------------------------------------------------
    // BaseRepository contract definition
    //-------------------------------------------------------------------------

    /**
     * Creates and configures the underlying OpenRDF Sesame 2
     * repository.
     * <p>
     * This method is invoked after all common repository parameters
     * ({@link #name}, {@link #url}...) have been set.</p>
     * @param  configuration   the DataLift configuration.
     *
     * @return a configured native repository.
     */
    abstract protected org.openrdf.repository.Repository
                            newNativeRepository(Configuration configuration);

    //-------------------------------------------------------------------------
    // Repository contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public org.openrdf.repository.Repository getNativeRepository() {
        return this.target;
    }

    /** {@inheritDoc} */
    @Override
    public RepositoryConnection newConnection() {
        try {
            return this.target.getConnection();
        }
        catch (RepositoryException e) {
            throw new TechnicalException("repository.connect.error", e,
                                         this.name, this.url, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean ask(String query, Map<String,Object> bindings,
                                     Dataset dataset, String baseUri) {
        RepositoryConnection cnx = this.newConnection();
        try {
            BooleanQuery q = cnx.prepareBooleanQuery(SPARQL, query, baseUri);
            this.setBindings(q, bindings);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            boolean result = q.evaluate();
            if (log.isTraceEnabled()) {
                log.trace("{} {} -> {}", query, bindings,
                                         Boolean.valueOf(result));
            }
            return result;
        }
        catch (OpenRDFException e) {
            throw new QueryException(query, e).populate(
                                                    bindings, dataset, baseUri);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void construct(String query, Map<String,Object> bindings,
                          RDFHandler handler,
                          Dataset dataset, String baseUri) throws RdfException {
        RepositoryConnection cnx = this.newConnection();
        try {
            GraphQuery q = cnx.prepareGraphQuery(SPARQL, query, baseUri);
            this.setBindings(q, bindings);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            q.evaluate(handler);
        }
        catch (RDFHandlerException e) {
            throw new QueryException(query, e).populate(
                                                    bindings, dataset, baseUri);
        }
        catch (OpenRDFException e) {
            throw new QueryException(query, e).populate(
                                                    bindings, dataset, baseUri);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void select(String query, Map<String,Object> bindings,
                       TupleQueryResultHandler handler,
                       Dataset dataset, String baseUri) throws RdfException {
        RepositoryConnection cnx = this.newConnection();
        try {
            TupleQuery q = cnx.prepareTupleQuery(SPARQL, query, baseUri);
            this.setBindings(q, bindings);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            q.evaluate(handler);
        }
        catch (TupleQueryResultHandlerException e) {
            throw new QueryException(query, e).populate(
                                                    bindings, dataset, baseUri);
        }
        catch (OpenRDFException e) {
            throw new QueryException(query, e).populate(
                                                    bindings, dataset, baseUri);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    /** {@inheritDoc}
     * @return the URL of the repository SPARQL endpoint, as defined by
     * the {@link #REPOSITORY_ENDPOINT_URL} configuration property or
     * the {@link #getUrl() repository connection URL} if the property
     * is not set.
     */
    @Override
    public String getEndpointUrl() {
        return this.endpointUrl;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Sets the initial variable bindings for a query, performing Java
     * to RDF type mapping.
     * @param  query      the query.
     * @param  bindings   the initial bindings.
     */
    private void setBindings(Query query, Map<String,Object> bindings) {
        if (bindings != null) {
            for (Entry<String,Object> e : bindings.entrySet()) {
                query.setBinding(e.getKey(), this.mapBinding(e.getValue()));
            }
        }
    }

    /**
     * Maps a Java object to an RDF data type.
     * @param  o   the Java object to map.
     *
     * @return the corresponding RDF type object.
     * @throws UnsupportedOperationException if no valid mapping can
     *         be found the Java type.
     */
    private Value mapBinding(Object o) {
        Value v = null;

        if (o instanceof URI) {
            v = this.valueFactory.createURI(o.toString());
        }
        else if (o instanceof String) {
            v = this.valueFactory.createLiteral(o.toString());
        }
        else if (o instanceof Integer) {
            v = this.valueFactory.createLiteral(((Integer)o).intValue());
        }
        else if (o instanceof Long) {
            v = this.valueFactory.createLiteral(((Long)o).longValue());
        }
        else if (o instanceof Boolean) {
            v = this.valueFactory.createLiteral(((Boolean)o).booleanValue());
        }
        else if (o instanceof Double) {
            v = this.valueFactory.createLiteral(((Double)o).doubleValue());
        }
        else if (o instanceof Byte) {
            v = this.valueFactory.createLiteral(((Byte)o).byteValue());
        }
        else if (o instanceof URL) {
            v = this.valueFactory.createURI(o.toString());
        }
        else {
            throw new UnsupportedOperationException(o.getClass().getName());
        }
        return v;
    }

    /**
     * Reads and parses the repository
     * {@link #REPOSITORY_PUBLIC_FLAG access control flag}.
     * @param  name            the repository name in DataLift
     *                         configuration.
     * @param  configuration   the DataLift configuration.
     *
     * @return <code>true</code> if the repository is publicly
     *         accessible, <code>false</code> otherwise.
     */
    private static boolean parseAccessControlFlag(String name,
                                                  Configuration configuration) {
        // Default repository is always public.
        boolean publicFlag = parseBoolean(
                    configuration.getProperty(name + REPOSITORY_DEFAULT_FLAG));
        if (! publicFlag) {
            // Not the default repository. => Check for public flag.
            publicFlag = parseBoolean(
                    configuration.getProperty(name + REPOSITORY_PUBLIC_FLAG));
        }
        return publicFlag;
    }

    private static boolean parseBoolean(String s) {
        boolean b = false;

        if (! isBlank(s)) {
            s = s.trim().toLowerCase();
            b = ((s.equals(Boolean.TRUE.toString()))
                                    || (s.equals("yes")) || (s.equals("1")));
        }
        return b;
    }
}
