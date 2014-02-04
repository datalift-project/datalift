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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.velocity.tools.config.DefaultKey;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Binding;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryConnection;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.core.rdf.QueryException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.RdfQueryException;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.AccessController;
import org.datalift.fwk.sparql.AccessController.ControlledQuery;

import static org.datalift.fwk.util.PrimitiveUtils.wrap;
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
    private final Map<String,Value> bindings = new HashMap<String,Value>();

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
        if (! isSet(uri)) {
            throw new IllegalArgumentException("uri");
        }
        this.prefix(prefix, URI.create(uri));
    }
    /**
     * Registers a namespace prefix
     * @param  prefix   the prefix.
     * @param  uri      the namespace URI.
     *
     * @throws IllegalArgumentException if <code>prefix</code> is
     *         <code>null</code> or <code>uri</code> is not a valid URI.
     */
    public void prefix(String prefix, URI uri) {
        try {
            if (! isSet(prefix)) {
                throw new IllegalArgumentException("prefix");
            }
            if (uri == null) {
                throw new IllegalArgumentException("uri");
            }
            this.prefixes.put(prefix, uri);
        }
        catch (RuntimeException e) {
            log.error("Failed to register prefix: \"{}\" -> \"{}\"", e,
                      prefix, uri);
            throw e;
        }
    }

    /**
     * Binds the specified value to the specified SPARQL query variable.
     * If the value is a native Java object (URI, URL, Integer, Boolean,
     * Byte...) it is first {@link RdfUtils#mapBinding(Object) converted}
     * into an RDF {@link Value} object. 
     * @param  name    the name of the SPARQL query variable.
     * @param  value   the value to associate to variable
     *                 <code>name</code>.
     *
     * @throws UnsupportedOperationException if no valid mapping is
     *         defined for the object Java type.
     */
    public void bind(String name, Object value) {
        this.bindings.put(name, RdfUtils.mapBinding(value));
    }

    /**
     * Binds the specified URI to the specified SPARQL query variable.
     * @param  name   the name of the SPARQL query variable.
     * @param  uri    the URI to associate to variable
     *                <code>name</code>.
     *
     * @see    #bind(String, Object)
     */
    public void bindUri(String name, String uri) {
        this.bindings.put(name, new URIImpl(this.resolvePrefixes(uri, false)));
    }

    /**
     * Registers the specified variable bindings.
     * @param  bindings   the bindings, associating values to
     *                    SPARQL query named variables.
     *
     * @see    #bind(String, Object)
     */
    public void bind(Map<String,Object> bindings) {
        for (Map.Entry<String,Object> e : bindings.entrySet()) {
            this.bind(e.getKey(), e.getValue());
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
            ControlledQuery ctrl = this.checkAccessControls(query, repository);
            BooleanQuery q = cnx.prepareBooleanQuery(SPARQL, ctrl.query);
            this.setGraphConstraints(q, ctrl);
            this.setBindings(q);
            boolean result = q.evaluate();
            if (log.isDebugEnabled()) {
                if (this.bindings.isEmpty()) {
                    log.debug("\"{}\" on RDF store: {} -> {}",
                              query, repository, wrap(result));
                }
                else {
                    log.debug("\"{}\" ({}) on RDF store: {} -> {}",
                              query, this.bindings, repository, wrap(result));
                }
            }
            return result;
        }
        catch (OpenRDFException e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            throw new QueryException(query, e);
        }
        finally {
            Repository.closeQuietly(cnx);
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
            ControlledQuery ctrl = this.checkAccessControls(query, repository);
            TupleQuery q = cnx.prepareTupleQuery(SPARQL, ctrl.query);
            this.setGraphConstraints(q, ctrl);
            this.setBindings(q);
            if (log.isDebugEnabled()) {
                if (this.bindings.isEmpty()) {
                    log.debug("Executing \"{}\" on RDF store: {}...",
                                            query, repository);
                }
                else {
                    log.debug("Executing \"{}\" ({}) on RDF store: {}\"...",
                                            query, this.bindings, repository);
                }
            }
            return new SelectResultIterator(cnx, q.evaluate());
        }
        catch (Exception e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            Repository.closeQuietly(cnx);
            throw new QueryException(query, e);
        }
        // Do not close connection until results have been read:
        // SelectResultIterator takes care of it.
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
            ControlledQuery ctrl = this.checkAccessControls(query, repository);
            GraphQuery q = cnx.prepareGraphQuery(SPARQL, ctrl.query);
            this.setGraphConstraints(q, ctrl);
            this.setBindings(q);
            if (log.isDebugEnabled()) {
                if (this.bindings.isEmpty()) {
                    log.debug("Executing \"{}\" on RDF store: {}...",
                                            query, repository);
                }
                else {
                    log.debug("Executing \"{}\" ({}) on RDF store: {}...",
                                            query, this.bindings, repository);
                }
            }
            return new StatementIterator(cnx, q.evaluate());
        }
        catch (Exception e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            Repository.closeQuietly(cnx);
            throw new QueryException(query, e);
        }
        // Do not close connection until results have been read:
        // StatementIterator takes care of it.
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
    public DescribeResult describe(Object uri) {
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
    public DescribeResult describe(String repository, Object uri) {
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
    private DescribeResult describe(Repository repository, Object uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }
        String u = uri.toString();
        String query = "DESCRIBE <" + this.resolvePrefixes(u, false) + '>';
        RepositoryConnection cnx = null;
        try {
            cnx = repository.newConnection();
            query = this.addPrefixes(query);
            ControlledQuery ctrl = this.checkAccessControls(query, repository);
            GraphQuery q = cnx.prepareGraphQuery(SPARQL, ctrl.query);
            this.setGraphConstraints(q, ctrl);
            this.setBindings(q);
            DescribeResult result = new DescribeResult(u, q.evaluate());
            if (log.isDebugEnabled()) {
                if (this.bindings.isEmpty()) {
                    log.debug("\"{}\" on RDF store: {} -> {} triples",
                                    query, repository, wrap(result.size()));
                }
                else {
                    log.debug("\"{}\" ({}) on RDF store: {} -> {} triples",
                                    query, this.bindings,
                                    repository, wrap(result.size()));
                }
            }
            return result;
        }
        catch (Exception e) {
            log.error("Error executing SPARQL query \"{}\"", e, query);
            throw new QueryException(query, e);
        }
        finally {
            Repository.closeQuietly(cnx);
        }
    }

    /**
     * Sets the initial variable bindings for a query.
     * @param  query   the SPARQL query.
     */
    private void setBindings(Query query) {
        if (! this.bindings.isEmpty()) {
            for (Entry<String,Value> e : this.bindings.entrySet()) {
                query.setBinding(e.getKey(), e.getValue());
            }
        }
    }

    private ControlledQuery checkAccessControls(String query,
                                                Repository repository) {
        ControlledQuery q = null;
        // Enforce access control policies, if any.
        if (this.accessController != null) {
            q = this.accessController.checkQuery(query, repository, null, null);
            // Override accessed graphs for ASK queries for which a
            // Sesame bug leads to "false" results whenever a DataSet is set.
            if ("ASK".equals(q.queryType)) {
                q.defaultGraphUris.clear();
                q.namedGraphUris.clear();
            }
        }
        else {
            q = new ControlledQuery(query, null, null, null, null);
        }
        // Return modified query, enriched with restrictions.
        return q;
    }

    /**
     * Builds the query dataset from the default and named graph URIs
     * defined for the query, according to the access control policy.
     * @param  query   the query with the access control information.
     * @return a {@link Dataset dataset} with the accessible graphs or
     *         <code>null</code> if no scope limitation is requested.
     */
    private Query setGraphConstraints(Query query, ControlledQuery ctrl) {
        if (! (ctrl.defaultGraphUris.isEmpty()
                                        && ctrl.namedGraphUris.isEmpty())) {
            DatasetImpl dataset = new DatasetImpl();
            for (String g : ctrl.defaultGraphUris) {
                dataset.addDefaultGraph(new URIImpl(g));
            }
            for (String g : ctrl.namedGraphUris) {
                dataset.addNamedGraph(new URIImpl(g));
            }
            query.setDataset(dataset);
        }
        return query;
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
     * {@link #prefix(String, String) registered} or
     * {@link RdfNamespace well-known} namespace prefix and
     * replaces it the actual namespace.
     * <p>
     * Note: {@link RdfNamespace Well-known prefixes} resolution do not
     * apply to the SPARQL query text. Hence all namespace prefixes used
     * in the query need to be properly registered. This method only
     * helps when accessing predicates in SPARQL responses, such as the
     * result of a DESCRIBE query.</p>
     * @param  uri         the URI to check for prefixes.
     * @param  predicate   whether the specified URI is a RDF predicate.
     *
     * @return the URI with any known namespace prefix replaced.
     */
    private String resolvePrefixes(String uri, boolean predicate) {
        if (predicate && "a".equals(uri)) {
            uri = RDF.TYPE.toString();
        }
        else {
            int n = uri.indexOf(':');
            if (n != -1) {
                String prefix = uri.substring(0, n);
                URI ns = prefixes.get(prefix);
                if (ns != null) {
                    uri = ns.toString() + uri.substring(n + 1);
                }
                else {
                    // Check for well-known prefix.
                    RdfNamespace rdfNs = RdfNamespace.findByPrefix(prefix);
                    if (rdfNs != null) {
                        uri = rdfNs.uri + uri.substring(n + 1);
                    }
                }
            }
        }
        return uri;
    }

    /**
     * Closes the specified query result iterator, in a fail-safe way.
     * @param  r   the query result to close.
     */
    private static <T> void closeQuietly(QueryResult<T> r) {
        if (r != null) {
            try { r.close(); } catch (Exception e) { /* Ignore... */ }
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
            closeQuietly(this.result);
            Repository.closeQuietly(this.cnx);
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

        /**
         * Creates a statement iterator, reading triples from the
         * specified query result.
         * @param  cnx      the connection to the RDF store, to be
         *                  closed once the results have been read.
         * @param  result   the query results to read triples from.
         */
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
            closeQuietly(this.result);
            Repository.closeQuietly(this.cnx);
        }
    }

    /**
     * The results of a DESCRIBE query on a single URI.
     * <p>
     * If the DESCRIBE results include triples for other subjects (e.g.
     * triples the value of which is the described URI),
     * {@link DescribeResult#otherSubjects()} and
     * {@link DescribeResult#resultsFor(Object)} allow to access the
     * {@link DescribeResult} objects for these subjects.</p>
     */
    public final class DescribeResult extends HashMap<String,Collection<Value>>
    {
        private final String uri;
        private final Map<String, DescribeResult> otherSubjects;

        private DescribeResult(String uri,
                               Map<String, DescribeResult> otherSubjects) {
            if (! isSet(uri)) {
                throw new IllegalArgumentException("uri");
            }
            if (otherSubjects == null) {
                throw new IllegalArgumentException("otherSubjects");
            }
            this.uri = uri;
            this.otherSubjects = otherSubjects;
        }

        private DescribeResult(String uri, GraphQueryResult result)
                                        throws QueryEvaluationException {
            this(uri, new HashMap<String,SparqlTool.DescribeResult>());
            try {
                URIImpl u = new URIImpl(uri);
                for (; result.hasNext(); ) {
                    Statement s = result.next();
                    DescribeResult m = this;
                    Resource r = s.getSubject();
                    if (! u.equals(r)) {
                        String subject = r.toString();
                        m = this.otherSubjects.get(subject);
                        if (m == null) {
                            m = new DescribeResult(subject, this.otherSubjects);
                            this.otherSubjects.put(subject, m);
                        }
                    }
                    String p = s.getPredicate().toString();
                    Collection<Value> v = m.get(p);
                    if (v == null) {
                        v = new LinkedList<Value>();
                        m.put(p, v);
                    }
                    v.add(s.getObject());
                }
            }
            finally {
                closeQuietly(result);
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
         * Returns the URIs of the predicates that describe the
         * RDF resource.
         * @return the URIs of the predicates for the RDF resource.
         */
        public Collection<String> predicates() {
            return this.keySet();
        }

        /**
         * Returns whether the DESCRIBE query results include triples
         * related to other RDF resources.
         * @return <code>true</code> if the query results include
         *         triples related to other RDF resource;
         *         <code>false</code> otherwise.
         */
        public boolean hasOtherSubjects() {
            return (! this.otherSubjects.keySet().isEmpty());
        }

        /**
         * Returns the subject URIs of the triples present in the query
         * results and not pertaining the main RDF resource.
         * @return the subject URIs different from the RDF resource
         *         being described.
         */
        public Collection<String> otherSubjects() {
            return this.otherSubjects.keySet();
        }

        /**
         * Return the triples for the specified URI.
         * @param  uri   a subject URI, different from the URI of the
         *               resource being described.
         * @return the triples for the specified URI as a
         *         {@link DescribeResult} object or <code>null</code>
         *         if no triples for the specified URI were returned.
         */
        public DescribeResult resultsFor(Object uri) {
            return (uri == null)? this: this.otherSubjects.get(uri.toString());
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
            Collection<Value> v = this.get(predicate);
            return ((v == null) || (v.isEmpty()))? null: v.iterator().next();
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
         * Returns a single string value for the specified RDF property.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return the string value of one the property, empty
         *         if the property was not found.
         */
        public String valueOf(String predicate) {
            Value v = this.value(predicate);
            return (v != null)? v.stringValue(): "";
        }
        /**
         * Returns a single string value for the specified RDF property,
         * the Java Bean way.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return the string value of one the property, empty
         *         if the property was not found.
         */
        public String getValueOf(String predicate) {
            return this.valueOf(predicate);
        }

        /** {@inheritDoc} */
        @Override
        public Collection<Value> get(Object key) {
            Collection<Value> v = null;
            if (key != null) {
                v = super.get(resolvePrefixes(key.toString(), true));
            }
            return v;
        }

        /**
         * Returns the string values for the specified RDF property.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return the values of the property as a collection of
         *         strings, empty if the property was not found.
         */
        public Collection<String> valuesOf(String predicate) {
            Collection<String> s = null;
            Collection<Value> r = this.get(predicate);
            if ((r == null) || (r.isEmpty())) {
                s = Collections.emptySet();
            }
            else {
                s = new LinkedList<String>();
                for (Value v : r) {
                    s.add(v.stringValue());
                }
            }
            return s;
        }
        /**
         * Returns the string values for the specified RDF property,
         * the Java Bean way.
         * @param  predicate   the URI of the RDF property, possibly
         *                     using a declared namespace prefix.
         *
         * @return the values of the property as a collection of
         *         strings, empty if the property was not found.
         */
        public Collection<String> getValuesOf(String predicate) {
            return this.valuesOf(predicate);
        }
    }
}
