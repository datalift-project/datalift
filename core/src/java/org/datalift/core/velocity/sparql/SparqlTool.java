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

package org.datalift.core.velocity.sparql;


import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.tools.config.DefaultKey;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryConnection;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.core.rdf.QueryException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfQueryException;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.AccessController;
import org.datalift.fwk.sparql.AccessController.ControlledQuery;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * The SPARQL Velocity tool allows querying Datalift RDF stores from
 * within Velocity templates and easily processing the results.
 * <p>
 * <i>Security notice</i>: The SPARQL tool relies on the installed
 * {@link AccessController} component (if any) to enforce the access
 * control rules on the queried RDF stores. Hence, for a given SPARQL
 * query, the resulting data may vary according to the access rights
 * granted to the user (logged in or anonymous). The Velocity templates
 * shall thus always expect some data to be missing.</p>
 * 
 * @author lbihanic
 */
@DefaultKey("sparql")
public final class SparqlTool
{
    private final static Logger log = Logger.getLogger();

    private final Configuration cfg;
    private final AccessController accessController;
    private final Map<String,URI> prefixes = new LinkedHashMap<String,URI>();

    public SparqlTool() {
        this.cfg = Configuration.getDefault();
        Collection<AccessController> acs =
                                    this.cfg.getBeans(AccessController.class);
        this.accessController = (! acs.isEmpty())? acs.iterator().next(): null;
    }

    /**
     * Registers a namespace prefix
     * @param  prefix   the prefix.
     * @param  uri      the namespace URI.
     *
     * @throws IllegalArgumentException if <code>prefix</code> is
     *         <code>null</code> or <code>uri</code> is not a valid URI.
     */
    public void prefix(String prefix, String uri) {
        try {
            if (! isSet(prefix)) {
                throw new IllegalArgumentException("prefix");
            }
            if (! isSet(uri)) {
                throw new IllegalArgumentException("uri");
            }
            this.prefixes.put(prefix, URI.create(uri));
        }
        catch (RuntimeException e) {
            log.error("Failed to register prefix: \"{}\" -> \"{}\"", e,
                      prefix, uri);
            throw e;
        }
    }

    /**
     * Executes the specified ASK query against Datalift
     * {@link Configuration#getDefaultRepository() default RDF store}.
     * @param  query   the ASK query.
     *
     * @return the query result as a boolean.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     *
     * @see    #ask(String, String)
     */
    public boolean ask(String query) {
        return this.ask(this.cfg.getDefaultRepository(), query);
    }

    /**
     * Executes the specified ASK query against the specified Datalift
     * RDF store.
     * @param  repository   the name of the RDF store to query, as
     *                      specified in Datalift configuration.
     * @param  query        the ASK query.
     *
     * @return the query result as a boolean.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    public boolean ask(String repository, String query) {
        return this.ask(this.cfg.getRepository(repository), query);
    }

    /**
     * Executes the specified ASK query against the specified Datalift
     * RDF store.
     * @param  repository   the Datalift RDF store to query.
     * @param  query        the ASK query.
     *
     * @return the query result as a boolean.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    private boolean ask(Repository repository, String query) {
        RepositoryConnection cnx = null;
        try {
            cnx = repository.newConnection();
            query = this.addPrefixes(query);
            BooleanQuery q = cnx.prepareBooleanQuery(SPARQL, query);
            this.checkAccessControls(query, q, repository);
            return q.evaluate();
        }
        catch (OpenRDFException e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            close(cnx);
            throw new QueryException(query, e);
        }
    }

    /**
     * Executes the specified SELECT query against Datalift
     * {@link Configuration#getDefaultRepository() default RDF store}.
     * @param  query   the SELECT query.
     *
     * @return the query result as an iterator on the matched bindings.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     *
     * @see    #select(String, String)
     */
    public Iterator<Map<String,Value>> select(String query) {
        return this.select(this.cfg.getDefaultRepository(), query);
    }

    /**
     * Executes the specified SELECT query against the specified
     * Datalift RDF store.
     * @param  repository   the name of the RDF store to query, as
     *                      specified in Datalift configuration.
     * @param  query        the SELECT query.
     *
     * @return the query result as an iterator on the matched bindings.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    public Iterator<Map<String,Value>> select(String repository, String query) {
        return this.select(this.cfg.getRepository(repository), query);
    }

    /**
     * Executes the specified SELECT query against the specified
     * Datalift RDF store.
     * @param  repository   the Datalift RDF store to query.
     * @param  query        the SELECT query.
     *
     * @return the query result as an iterator on the matched bindings.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    private Iterator<Map<String,Value>> select(Repository repository,
                                               String query) {
        if (! isSet(query)) {
            throw new IllegalArgumentException("query");
        }
        RepositoryConnection cnx = null;
        try {
            cnx = repository.newConnection();
            query = this.addPrefixes(query);
            TupleQuery q = cnx.prepareTupleQuery(SPARQL, query);
            this.checkAccessControls(query, q, repository);
            return new SelectResultIterator(cnx, q.evaluate());
        }
        catch (OpenRDFException e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            close(cnx);
            throw new QueryException(query, e);
        }
    }

    /**
     * Executes the specified CONSTRUCT (or DESCRIBE) query against
     * Datalift
     * {@link Configuration#getDefaultRepository() default RDF store}.
     * @param  query   the CONSTRUCT (or DESCRIBE) query.
     *
     * @return the query result as a {@link Statement} iterator.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     *
     * @see    #construct(String, String)
     */
    public Iterator<Statement> construct(String query) {
        return this.construct(this.cfg.getDefaultRepository(), query);
    }

    /**
     * Executes the specified CONSTRUCT (or DESCRIBE) query against
     * the specified Datalift RDF store.
     * @param  repository   the name of the RDF store to query, as
     *                      specified in Datalift configuration.
     * @param  query        the CONSTRUCT (or DESCRIBE) query.
     *
     * @return the query result as a {@link Statement} iterator.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    public Iterator<Statement> construct(String repository, String query) {
        return this.construct(this.cfg.getRepository(repository), query);
    }

    /**
     * Executes the specified CONSTRUCT (or DESCRIBE) query against
     * the specified Datalift RDF store.
     * @param  repository   the Datalift RDF store to query.
     * @param  query        the CONSTRUCT (or DESCRIBE) query.
     *
     * @return the query result as a {@link Statement} iterator.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    private Iterator<Statement> construct(Repository repository, String query) {
        if (! isSet(query)) {
            throw new IllegalArgumentException("query");
        }
        RepositoryConnection cnx = null;
        try {
            cnx = repository.newConnection();
            query = this.addPrefixes(query);
            GraphQuery q = cnx.prepareGraphQuery(SPARQL, query);
            this.checkAccessControls(query, q, repository);
            return new StatementIterator(cnx, q.evaluate());
        }
        catch (OpenRDFException e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            close(cnx);
            throw new QueryException(query, e);
        }
    }

    /**
     * Executes a DESCRIBE query for the specified RDF resource against
     * Datalift
     * {@link Configuration#getDefaultRepository() default RDF store}.
     * @param  uri   the URI of the RDF resource to retrieve.
     *
     * @return the query result as a {@link DescribeResult} object.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     *
     * @see    #describe(String, String)
     */
    public DescribeResult describe(String uri) {
       return this.describe(this.cfg.getDefaultRepository(), uri); 
    }

    /**
     * Executes a DESCRIBE query for the specified RDF resource against
     * the specified Datalift RDF store.
     * @param  repository   the name of the RDF store to query, as
     *                      specified in Datalift configuration.
     * @param  uri          the URI of the RDF resource to retrieve.
     *
     * @return the query result as a {@link DescribeResult} object.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    public DescribeResult describe(String repository, String uri) {
        return this.describe(this.cfg.getRepository(repository), uri);
    }

    /**
     * Executes a DESCRIBE query for the specified RDF resource against
     * the specified Datalift RDF store.
     * @param  repository   the Datalift RDF store to query.
     * @param  uri          the URI of the RDF resource to retrieve.
     *
     * @return the query result as a {@link DescribeResult} object.
     * @throws RdfQueryException if any error occurred executing the
     *         query or processing the result.
     */
    private DescribeResult describe(Repository repository, String uri) {
        if (! isSet(uri)) {
            throw new IllegalArgumentException("uri");
        }
        String query = "DESCRIBE <" + this.resolvePrefixes(uri) + '>';
        RepositoryConnection cnx = null;
        try {
            cnx = repository.newConnection();
            query = this.addPrefixes(query);
            GraphQuery q = cnx.prepareGraphQuery(SPARQL, query);
            this.checkAccessControls(query, q, repository);
            return new DescribeResult(uri, q.evaluate());
        }
        catch (RdfQueryException e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            close(cnx);
            throw e;
        }
        catch (Exception e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            close(cnx);
            throw new QueryException(query, e);
        }
    }

    private void checkAccessControls(String query, Query compiledQuery,
                                     Repository repository) {
        List<String> defaultGraphUris = null;
        List<String> namedGraphUris = null;

        // Enforce access control policies, if any.
        if (this.accessController != null) {
            ControlledQuery q = this.accessController.checkQuery(
                                            query, repository, null, null);
            // Get modified query, enriched with restrictions.
            query = q.query;
            // Override accessed graphs, except for ASK queries for which a
            // Sesame bug leads to "false" results whenever a DataSet is set.
            if (! "ASK".equals(q.queryType)) {
                defaultGraphUris = q.defaultGraphUris;
                namedGraphUris   = q.namedGraphUris;
            }
        }
        // Build query dataset from specified graphs, if any.
        if ((defaultGraphUris != null) || (namedGraphUris != null)) {
            DatasetImpl dataset = new DatasetImpl();
            if (defaultGraphUris != null) {
                for (String g : defaultGraphUris) {
                    dataset.addDefaultGraph(new URIImpl(g));
                }
            }
            if (namedGraphUris != null) {
                for (String g : namedGraphUris) {
                    dataset.addNamedGraph(new URIImpl(g));
                }
            }
            compiledQuery.setDataset(dataset);
        }
    }

    /**
     * Prepends the
     * {@link #prefix(String, String) registered namespace prefixes} to
     * a SPARQL query.
     * @param  query   the SPARQL query.
     *
     * @return the query augmented with the namespace prefix
     *         declarations.
     */
    private String addPrefixes(String query) {
        StringBuilder b = new StringBuilder(1024);
        for (Map.Entry<String,URI> e : this.prefixes.entrySet()) {
            b.append("PREFIX ").append(e.getKey()).append(": <")
                               .append(e.getValue()).append(">\n");
        }
        return b.append(query).toString();
    }

    /**
     * Checks the specified URI for a
     * {@link #prefix(String, String) registered namespace prefix} and
     * replaces it the actual namespace.
     * @param  uri   the URI to check for prefixes.
     *
     * @return the URI with any known namespace prefix replaced.
     */
    private String resolvePrefixes(String uri) {
        int n = uri.indexOf(':');
        if (n != -1) {
            String prefix = uri.substring(0, n);
            URI ns = prefixes.get(prefix);
            if (ns != null) {
                uri = ns.toString() + uri;
            }
        }
        return uri;
    }

    /**
     * Closes the specified repository connection, in a fail-safe way.
     * @param  cnx   the connection to close.
     */
    private static void close(RepositoryConnection cnx) {
        if (cnx != null) {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    /**
     * An iterator on the results of a SELECT SPARQL query.
     */
    private final static class SelectResultIterator
                                        implements Iterator<Map<String,Value>> {
        private final RepositoryConnection cnx;
        private final TupleQueryResult result;

        public SelectResultIterator(RepositoryConnection cnx,
                                    TupleQueryResult result) {
            this.cnx = cnx;
            this.result = result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            boolean hasNext = false;
            try {
                hasNext = this.result.hasNext();
                if (! hasNext) {
                    this.finalize();
                }
            }
            catch (Exception e) {
                log.error("Unexpected error while browsing result", e);
            }
            return hasNext;
        }

        /** {@inheritDoc} */
        @Override
        public Map<String,Value> next() {
            try {
                Map<String,Value> bindings = new HashMap<String,Value>();
                for (Binding b : this.result.next()) {
                    bindings.put(b.getName(), b.getValue());
                }
                return bindings;
            }
            catch (Exception e) {
                throw new RuntimeException("Failed reading query results", e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        protected void finalize() {
            try { this.result.close(); } catch (Exception e) { /* Ignore... */ }
            close(this.cnx);
        }
    }

    /**
     * An iterator on a set of RDF statements, such as the results of
     * a CONSTRUCT or DESCRIBE SPARQL query.
     */
    private final static class StatementIterator implements Iterator<Statement>
    {
        private final RepositoryConnection cnx;
        private final GraphQueryResult result;

        public StatementIterator(RepositoryConnection cnx,
                                 GraphQueryResult result) {
            this.cnx = cnx;
            this.result = result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            boolean hasNext = false;
            try {
                hasNext = this.result.hasNext();
                if (! hasNext) {
                    this.finalize();
                }
            }
            catch (Exception e) {
                log.error("Unexpected error while browsing result", e);
            }
            return hasNext;
        }

        /** {@inheritDoc} */
        @Override
        public Statement next() {
            try {
                return this.result.next();
            }
            catch (Exception e) {
                throw new RuntimeException("Failed reading query results", e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        protected void finalize() {
            try { this.result.close(); } catch (Exception e) { /* Ignore... */ }
            close(this.cnx);
        }
    }

    /**
     * The results of a DESCRIBE query on a single URI.
     */
    public final class DescribeResult
    {
        private final String uri;
        private final Map<String,Collection<Value>> values =
                                        new HashMap<String,Collection<Value>>();

        private DescribeResult(String uri, GraphQueryResult result)
                                        throws QueryEvaluationException {
            this.uri = uri;
            try {
                for (; result.hasNext(); ) {
                    Statement s = result.next();
                    String p = s.getPredicate().toString();
                    Collection<Value> v = this.values.get(p);
                    if (v == null) {
                        v = new HashSet<Value>();
                        this.values.put(p, v);
                    }
                    v.add(s.getObject());
                }
            }
            finally {
                try { result.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }

        /**
         * Returns the URI of the RDF resource being described.
         * @return the URI of the RDF resource.
         */
        public String uri() {
            return this.uri;
        }
        /**
         * Returns the URI of the RDF resource being described, the
         * Java Bean way.
         * @return the URI of the RDF resource.
         */
        public String getUri() {
            return this.uri();
        }

        /**
         * Returns a single value for the specified RDF property.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return one of the values of the property or
         *         <code>null</code> if the property was not found.
         */
        public Value value(String predicate) {
            Collection<Value> v = this.values(predicate);
            return (! v.isEmpty())? v.iterator().next(): null;
        }
        /**
         * Returns a single value for the specified RDF property, the
         * Java Bean way.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return one of the values of the property or
         *         <code>null</code> if the property was not found.
         */
        public Value getValue(String predicate) {
            return this.value(predicate);
        }

        /**
         * Returns the values for the specified RDF property.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return the values of the property as a collection,
         *         empty if the property was not found.
         */
        public Collection<Value> values(String predicate) {
            Collection<Value> v = null;
            if (isSet(predicate)) {
                v = this.values.get(resolvePrefixes(predicate));
            }
            if (v == null) {
                v = Collections.emptySet();
            }
            return v;
        }
        /**
         * Returns the values for the specified RDF property, the
         * Java Bean way.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return the values of the property as a collection,
         *         empty if the property was not found.
         */
        public Collection<Value> getValues(String predicate) {
            return this.values(predicate);
        }
    }
}
