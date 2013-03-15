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

package org.datalift.sparql.query;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.BooleanLiteralImpl;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.NumericLiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.sparql.query.WhereClauses.WhereType;
import org.datalift.sparql.query.functions.SparqlFunction;
import org.datalift.sparql.query.impl.VariableImpl;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * A simple grammar to help building SPARQL update (insert & delete) and
 * CONSTRUCT queries in Java.
 *
 * @author lbihanic
 */
public abstract class UpdateQuery
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** Well-known RDF namespace prefix mappings. */
    private final static Map<String,String> tentativePrefixes =
                                                new HashMap<String,String>();

    //-------------------------------------------------------------------------
    // Class initializer
    //-------------------------------------------------------------------------

    static {
        for (RdfNamespace ns : RdfNamespace.values()) {
            tentativePrefixes.put(ns.uri, ns.prefix);
        }
    }

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The query type (CONSTRUCT, INSET, DELETE...). */
    public final String type;
    /** The named graph the query will modify. */
    public final URI targetGraph;

    private final AtomicInteger prefixCount   = new AtomicInteger();
    private final AtomicInteger variableCount = new AtomicInteger();
    private final Map<String,String> prefix2Ns = new HashMap<String,String>();
    private final Map<String,String> ns2Prefix = new HashMap<String,String>();
    private final Collection<Statement> triples = new LinkedList<Statement>();
    private final WhereClauses whereClauses = new WhereClauses();
    private final Map<String,WhereClauses> namedWhereClauses =
                                            new HashMap<String,WhereClauses>();
    private final Collection<Binding> bindings = new LinkedList<Binding>();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new query.
     * @param  type   the query type (CONSTRUCT, INSERT, DELETE...).
     */
    protected UpdateQuery(String type) {
        this(type, null);
    }

    /**
     * Creates a new query.
     * @param  type          the query type (CONSTRUCT, INSERT,
     *                       DELETE...).
     * @param  targetGraph   the named graph the query will modify or
     *                       <code>null</code> to target the default
     *                       graph.
     */
    protected UpdateQuery(String type, URI targetGraph) {
        if (isBlank(type)) {
            throw new IllegalArgumentException("queryType");
        }
        this.type = type.toUpperCase();
        this.targetGraph = targetGraph;
    }

    //-------------------------------------------------------------------------
    // Triples construction
    //-------------------------------------------------------------------------

    /**
     * Shorthand method to append a triple template for string literal
     * value.
     * @param  s   the subject (URI or variable).
     * @param  p   the predicate.
     * @param  v   the value.
     *
     * @return this query object, for call chaining.
     * @see    #triple(Resource, URI, Value)
     */
    public final UpdateQuery triple(Resource s, URI p, String v) {
        return this.triple(s, p, this.literal(v));
    }

    /**
     * Shorthand method to append a triple template for string literal
     * value.
     * @param  s       the subject (URI or variable).
     * @param  p       the predicate.
     * @param  v       the value.
     * @param  graph   the named graph for the triple belongs to,
     *                 <code>null</code> to use the default graph.
     *
     * @return this query object, for call chaining.
     * @see    #triple(Resource, URI, Value, URI)
     */
    public final UpdateQuery triple(Resource s, URI p, String v, URI graph) {
        return this.triple(s, p, this.literal(v), graph);
    }

    public final UpdateQuery triple(Resource s, URI p, SparqlExpression expr) {
        return this.triple(s, p, expr, null, null);
    }
    public final UpdateQuery triple(Resource s, URI p,
                                    SparqlExpression expr, URI graph) {
        return this.triple(s, p, expr, null, graph);
    }
    public final UpdateQuery triple(Resource s, URI p,
                                    SparqlExpression expr, String var) {
        return this.triple(s, p, expr, var, null);
    }
    public UpdateQuery triple(Resource s, URI p,
                              SparqlExpression expr, String var, URI graph) {
        Variable v = this.variable(var);
        return this.bind(expr, v).triple(s, p, v, graph);
    }

    /**
     * Appends the specified template to the list of triples this query
     * generates (constructs, inserts or deletes).
     * @param  s   the subject (URI or variable).
     * @param  p   the predicate.
     * @param  o   the value.
     *
     * @return this query object, for call chaining.
     * @see    #triple(Resource, URI, Value, URI)
     */
    public final UpdateQuery triple(Resource s, URI p, Value o) {
        return this.triple(s, p, o, null);
    }

    /**
     * Appends the specified template to the list of triples this query
     * generates (constructs, inserts or deletes).
     * @param  s       the subject (URI or variable).
     * @param  p       the predicate.
     * @param  o       the value.
     * @param  graph   the named graph for the triple belongs to,
     *                 <code>null</code> to use the default graph.
     *
     * @return this query object, for call chaining.
     */
    public UpdateQuery triple(Resource s, URI p, Value o, URI graph) {
        this.triples.add(this.statement(s, p, o, graph));
        return this;
    }

    /**
     * Appends a triple template defining the {@link #RDF_TYPE RDF type}
     * of the specified subject into the list of triples this query
     * generates (constructs, inserts or deletes).
     * @param  s   the subject (URI or variable).
     * @param  t   the RDF type.
     *
     * @return this query object, for call chaining.
     * @see    #rdfType(Resource, URI, URI)
     */
    public final UpdateQuery rdfType(Resource s, URI t) {
        return this.rdfType(s, t, null);
    }

    /**
     * Appends a triple template defining the {@link #RDF_TYPE RDF type}
     * of the specified subject into the list of triples this query
     * generates (constructs, inserts or deletes).
     * @param  s       the subject (URI or variable).
     * @param  t       the RDF type.
     * @param  graph   the named graph for the triple belongs to,
     *                 <code>null</code> to use the default graph.
     *
     * @return this query object, for call chaining.
     * @see    #triple(Resource, URI, Value, URI)
     */
    public UpdateQuery rdfType(Resource s, URI t, URI graph) {
        return this.triple(s, RDF.TYPE, t, graph);
    }

    //-------------------------------------------------------------------------
    // Where clauses construction
    //-------------------------------------------------------------------------

    /**
     * Shorthand method to append a WHERE clause to the default clause
     * group for string literal value.
     * @param  s       the subject (URI or variable).
     * @param  p       the predicate.
     * @param  v       the value.
     *
     * @return this query object, for call chaining.
     * @see    #where(Resource, URI, Value, URI)
     */
    public final UpdateQuery where(Resource s, URI p, String v) {
        return this.where(s, p, v, null);
    }

    /**
     * Shorthand method to append a WHERE clause to the default clause
     * group for string literal value.
     * @param  s       the subject (URI or variable).
     * @param  p       the predicate.
     * @param  v       the value.
     * @param  graph   the named graph for the triple shall belong to
     *                 or <code>null</code> for any graph.
     *
     * @return this query object, for call chaining.
     * @see    #where(Resource, URI, Value, URI)
     */
    public final UpdateQuery where(Resource s, URI p, String v, URI graph) {
        return this.where(s, p, this.literal(v), graph);
    }

    /**
     * Appends the specified SPARQL WHERE clause to the default clause
     * group for this query.
     * @param  s   the subject (URI or variable).
     * @param  p   the predicate.
     * @param  o   the value.
     *
     * @return this query object, for call chaining.
     * @see    #where(Resource, URI, Value, URI)
     */
    public final UpdateQuery where(Resource s, URI p, Value o) {
        return this.where(s, p, o, null);
    }

    /**
     * Appends the specified SPARQL WHERE clause to the default clause
     * group for this query.
     * @param  s       the subject (URI or variable).
     * @param  p       the predicate.
     * @param  o       the value.
     * @param  graph   the named graph for the triple shall belong to
     *                 or <code>null</code> for any graph.
     *
     * @return this query object, for call chaining.
     * @see    #where(Resource, URI, Value, URI, String)
     */
    public final UpdateQuery where(Resource s, URI p, Value o, URI graph) {
        return this.where(s, p, o, graph, (WhereClauses)null);
    }

    /**
     * Appends the specified SPARQL WHERE clause to the specified clause
     * group for this query.
     * @param  s       the subject (URI or variable).
     * @param  p       the predicate.
     * @param  o       the value.
     * @param  graph   the named graph for the triple shall belong to
     *                 or <code>null</code> for any graph.
     * @param  group   the clause group name.
     *
     * @return this query object, for call chaining.
     * @see    #where(Resource, URI, Value, URI, WhereClauses)
     */
    public final UpdateQuery where(Resource s, URI p, Value o, URI graph,
                                                               String group) {
        return this.where(s, p, o, graph, this.whereGroup(group));
    }

    /**
     * Appends the specified SPARQL WHERE clause to the specified clause
     * group for this query, creating the group if it does not exist and
     * a group type is specified.
     * @param  s           the subject (URI or variable).
     * @param  p           the predicate.
     * @param  o           the value.
     * @param  graph       the named graph for the triple shall belong
     *                     to or <code>null</code> for any graph.
     * @param  groupKey    the clause group name.
     * @param  groupType   the type of the clause group to create or
     *                     <code>null</code> to prevent on-the-fly
     *                     group creation.
     *
     * @return this query object, for call chaining.
     * @see    #where(Resource, URI, Value, URI, WhereClauses)
     */
    public final UpdateQuery where(Resource s, URI p, Value o, URI graph,
                                   String groupKey, WhereType groupType) {
        return this.where(s, p, o, graph, (groupType != null)?
                    // Allows on-the-fly group creation.
                    this.whereGroup(groupKey, groupType, this.whereClauses):
                    this.whereGroup(groupKey));
    }

    /**
     * Appends the specified SPARQL WHERE clause to the specified clause
     * group for this query.
     * @param  s       the subject (URI or variable).
     * @param  p       the predicate.
     * @param  o       the value.
     * @param  graph   the named graph for the triple shall belong to
     *                 or <code>null</code> for any graph.
     * @param  group   the clause group or <code>null</code> to use the
     *                 default clause group.
     *
     * @return this query object, for call chaining.
     * @see    #where(Resource, URI, Value, URI, WhereClauses)
     */
    public UpdateQuery where(Resource s, URI p, Value o, URI graph,
                                                         WhereClauses group) {
        if (group == null) {
            group = this.whereClauses;
        }
        group.add(this.statement(s, p, o, graph));
        return this;
    }

    /**
     * Retrieves the specified WHERE clause group.
     * @param  key   the name of the WHERE clause group or
     *               <code>null</code> for the default group.
     *
     * @return the WHERE clause group associated to <code>key</code>
     *         for this query.
     * @throws IllegalArgumentException if no binding exists for
     *         <code>key</code>.
     */
    public final WhereClauses whereGroup(String key) {
        return this.whereGroup(key, null, (WhereClauses)null);
    }

    /**
     * Creates a new WHERE clause group of the specified type,
     * dynamically generating the group name.  The new clause group
     * is a child of the default group.
     * @param  type   the clause group type.
     *
     * @return the created WHERE clause group.
     * @see    #whereGroup(String, WhereType, WhereClauses)
     */
    public final WhereClauses whereGroup(WhereType type) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        return this.whereGroup(null, type, (WhereClauses)null);
    }

    /**
     * Creates a new WHERE clause group.
     * @param  key      the clause group name.
     * @param  type     the clause group type.
     * @param  parent   the name of parent clause group or
     *                  <code>null</code> if the new group is a child
     *                  of the default group.
     *
     * @return the created WHERE clause group.
     * @see    #whereGroup(String, WhereType, WhereClauses)
     */
    public final WhereClauses whereGroup(String key, WhereType type,
                                                     String parent) {
        return this.whereGroup(key, type, this.whereGroup(parent));
    }

    /**
     * Creates a new WHERE clause group.
     * @param  key      the clause group name.
     * @param  type     the clause group type.
     * @param  parent   the parent clause group or <code>null</code> if
     *                  the new group is a child of the default group.
     *
     * @return the created WHERE clause group.
     */
    public WhereClauses whereGroup(String key, WhereType type,
                                               WhereClauses parent) {
        WhereClauses w = null;
        boolean hasKey = isSet(key);
        if (hasKey) {
            // Retrieve group from query registry.
            w = this.namedWhereClauses.get(key);
        }
        else if (type == null) {
            // No name and no data to create a new group. => Return default.
            w = this.whereClauses;
        }
        if ((w == null) && (type != null)) {
            // Not found. => Create and register new group.
            if (! hasKey) {
                // Generate group name dynamically.
                key = this.nextVariable("w");
            }
            w = new WhereClauses(type, key,
                                 (parent != null)? parent: this.whereClauses);
            this.namedWhereClauses.put(key, w);
        }
        if (w == null) {
            throw new IllegalArgumentException(key);
        }
        return w;
    }

    //-------------------------------------------------------------------------
    // Variable binding
    //-------------------------------------------------------------------------

    /**
     * Bind the specified SPARQL expression to the specified variable.
     * @param  expr   the SPARQL expression.
     * @param  v      the variable to bind the expression to.
     *
     * @return this query object, for call chaining.
     */
    public UpdateQuery bind(SparqlExpression expr, Variable v) {
        this.bindings.add(new Binding(expr, v));
        return this;
    }

    //-------------------------------------------------------------------------
    // Namespace prefix mapping
    //-------------------------------------------------------------------------

    /**
     * Add a namespace prefix mapping.
     * @param  prefix   the prefix.
     * @param  ns       the namespace URI to bind to the prefix.
     *
     * @return this query object, for call chaining.
     */
    public UpdateQuery prefix(String prefix, String ns) {
        this.prefix2Ns.put(prefix, ns);
        this.ns2Prefix.put(ns, prefix);
        return this;
    }

    /**
     * Return the prefix associated to the specified namespace for
     * this query.  If no mapping exists, this method also checks for
     * {@link RdfNamespace well-known prefix mappings} and automatically
     * registers positive matches.
     * @param  ns   the namespace URI.
     *
     * @return the prefix associated to the namespace or
     *         <code>null</code> if none was defined.
     */
    public String prefixFor(String ns) {
        if (ns.endsWith(":")) {
            return ns.substring(0, ns.length() - 1);
        }
        String prefix = this.ns2Prefix.get(ns);
        if (prefix == null) {
            prefix = tentativePrefixes.get(ns);
            if (prefix == null) {
                prefix = "p" + this.prefixCount.incrementAndGet();
            }
            this.prefix(prefix, ns);
        }
        return prefix;
    }

    //-------------------------------------------------------------------------
    // Factory methods
    //-------------------------------------------------------------------------

    /**
     * Creates a URI object for the specified URI string.
     * @param  uri   the URI as a string.
     *
     * @return a URI object.
     */
    public URI uri(String uri) {
        return this.uri(null, uri);
    }

    /**
     * Creates a URI for the specified namespace and local name.
     * @param  ns     the namespace URI or prefix.
     * @param  name   the local name.
     *
     * @return a URI object.
     */
    public URI uri(String ns, String name) {
        String uri = this.prefix2Ns.get(ns);
        if (uri != null) {
            ns = uri;
        }
        return (ns != null)? new URIImpl(ns + name): new URIImpl(name);
    }

    /**
     * Creates a new variable with a generated (unique) name.
     * @return a new variable.
     */
    public Variable variable() {
        return this.variable((String)null);
    }

    /**
     * Creates a new variable, the name of which is derived from the
     * specified URI.
     * @param  u   the URI to build the variable name from.
     *
     * @return a new variable.
     */
    public final Variable variable(URI u) {
        return this.variable(this.nameFor(u));
    }

    /**
     * Creates a new variable with the specified name or generating one
     * if none is specified.
     * @param  name   the (optional) variable name.
     *
     * @return a new variable.
     */
    public Variable variable(String name) {
        if (! isSet(name)) {
            name = this.nextVariable("v");
        }
        return new VariableImpl(name);
    }

    /**
     * Creates a string literal.
     * @param  v   the string literal value.
     *
     * @return a literal holding the specified string value.
     */
    public Literal literal(String v) {
        return new LiteralImpl(v);
    }

    /**
     * Creates a boolean literal.
     * @param  b   the boolean literal value.
     *
     * @return a literal holding the specified boolean value.
     */
    public Literal literal(boolean b) {
        return new BooleanLiteralImpl(b);
    }

    /**
     * Creates a numeric literal.
     * @param  l   the long integer literal value.
     *
     * @return a literal holding the specified integer value.
     */
    public Literal literal(long l) {
        return new NumericLiteralImpl(l);
    }

    /**
     * Creates a numeric literal.
     * @param  v   the floating point literal value.
     *
     * @return a literal holding the specified floating point value.
     */
    public Literal literal(double d) {
        return new NumericLiteralImpl(d);
    }

    public final BNode blankNode() {
        return this.blankNode((String)null);
    }

    public final BNode blankNode(URI u) {
        return this.blankNode(this.nameFor(u));
    }

    public BNode blankNode(String name) {
        if (! isSet(name)) {
            name = this.nextVariable("b");
        }
        return new BNodeImpl(name);
    }

    protected Statement statement(Resource s, URI p, Value o, URI graph) {
        return ((graph != null)? new ContextStatementImpl(s, p, o, graph):
                                 new StatementImpl(s, p, o));
    }

    protected final String nextVariable(String prefix) {
        return prefix + this.variableCount.incrementAndGet();
    }

    /**
     * Generates a SPARQL variable name for the specified URI.
     * @param  u   the URI to generate a name for.
     *
     * @return a valid SPARQL variable name.
     */
    protected final String nameFor(URI u) {
        if (u == null) {
            throw new IllegalArgumentException("u");
        }
        return this.prefixFor(u.getNamespace()) + '_' + u.getLocalName();
    }

    //-------------------------------------------------------------------------
    // Query SPARQL serialization
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    public final String toString() {
        StringBuilder b = new StringBuilder(1024);
        // Namespace prefix declarations
        for (Entry<String,String> e : this.prefix2Ns.entrySet()) {
            b.append("PREFIX ").append(e.getKey())
             .append(": <").append(e.getValue()).append(">\n");
        }
        b.append('\n');
        // With graph
        if (this.targetGraph != null) {
            b.append("WITH <").append(this.targetGraph.toString())
                              .append(">\n");
        }
        // Query type
        b.append(this.type).append(" {\n");
        // Triples, grouped by graph.
        b = this.append(this.triples, b).append("}\n");
        // WHERE clauses, grouped by graph.
        if (! this.whereClauses.isEmpty()) {
            b.append("WHERE {\n");
            b = this.append(this.whereClauses, b, 0);
            // Local variable bindings
            for (Binding bnd : this.bindings) {
                b.append("\t").append(bnd).append('\n');
            }
            b.append('}');
        }
        return b.toString();
    }

    private StringBuilder append(WhereClauses c, StringBuilder b, int level) {
        if (c.isEmpty()) {
            // No statements and no child where clauses. => Return right away.
            return b;
        }
        if ((level > 0) && (c.type != WhereType.UNION)) {
            b.append('\t');
            if (c.type != WhereType.DEFAULT) {
                b.append(c.type).append(' ');
            }
            b.append("{\n");
        }
        this.append(c.clauses, b);
        boolean first = true;
        for (WhereClauses w : c.children) {
            if ((c.type == WhereType.UNION) && (! first)) {
                b.append('\t').append(c.type).append('\n');
            }
            first = false;
            this.append(w, b, level + 1);
        }
        if (level > 0) {
            b.append("\t}\n");
        }
        return b;
    }

    private StringBuilder append(Collection<Statement> c, StringBuilder b) {
        if (c.isEmpty()) {
            // Collection is empty. => Return right away.
            return b;
        }
        // Group statement using named graph, subject and predicate.
        List<Statement> stmts = new ArrayList<Statement>(c);
        Collections.sort(stmts, new Comparator<Statement>() {
                    @Override
                    public int compare(Statement t1, Statement t2) {
                        int n = 0;
                        // Sort statements by named graph first.
                        if (t1.getContext() != null) {
                            n = (t2.getContext() != null)?
                                    t1.getContext().stringValue().compareTo(
                                                t2.getContext().stringValue()):
                                    1;
                        }
                        else {
                            n = (t2.getContext() != null)? -1: 0;
                        }
                        // Then by subject.
                        if (n == 0) {
                            n = t1.getSubject().stringValue().compareTo(
                                                t2.getSubject().stringValue());
                        }
                        // And finally by predicate.
                        if (n == 0) {
                            n = t1.getPredicate().stringValue().compareTo(
                                                t2.getPredicate().stringValue());
                        }
                        return n;
                    }
                });
        Resource graph = null;
        boolean inGraph = false;
        Resource s = null;
        URI p = null;
        for (Statement t : stmts) {
            // Check for new named graph scope.
            Resource g = t.getContext();
            if ((inGraph) && ((g == null) || (! g.equals(graph)))) {
                // Close previous named graph scope.
                b.append("\t}\n");
                inGraph = false;
                graph = null;
                s = null;
                p = null;
            }
            if ((! inGraph) && (g != null)) {
                // Open new named graph scope.
                b.append("\tGRAPH <").append(g.toString()).append("> {\n");
                inGraph = true;
                graph = g;
            }
            // Optimize request look (!) by not duplicating repeating
            // subjects and predicates
            if (t.getSubject().equals(s)) {
                // Same subject. => Remove triple terminator (" .").
                b.setLength(b.length() - 2);
                if (t.getPredicate().equals(p)) {
                    // Same predicate. => Object list
                    b.append(",\n\t\t\t\t");
                }
                else {
                    // New predicate.
                    p = t.getPredicate();
                    b.append(";\n\t\t\t").append(this.toString(p)).append(' ');
                }
            }
            else {
                // New subject. => Reset both subject and predicate.
                s = t.getSubject();
                p = t.getPredicate();
                b.append("\t\t").append(this.toString(s)).append('\t')
                                .append(this.toString(p)).append(' ');
            }
            // Append value and triple terminator.
            b.append(this.toString(t.getObject())).append(" .\n");
        } // End loop.
        // Close current name graph scope, if any.
        if (inGraph) {
            b.append("\t}\n");
            inGraph = false;
        }
        return b;
    }

    private String toString(Value v) {
        String s = null;
        if (v instanceof URI) {
            URI u = (URI)v;
            if (u == RDF.TYPE) {
                s = "a";
            }
            else {
                String nsUri  = u.getNamespace();
                String prefix = this.ns2Prefix.get(nsUri);
                if ((prefix == null) && (nsUri.endsWith(":"))) {
                    // Check for prefixed URI.
                    String p = nsUri.substring(0, nsUri.length() - 1);
                    if (this.prefix2Ns.get(p) != null) {
                        prefix = p;
                    }
                    // Else: Unknown prefix.
                }
                s = (prefix != null)? prefix + ':' + u.getLocalName():
                                      "<" + v.toString() + '>';
            }
        }
        else {
            s = v.toString();
        }
        return s;
    }

    //-------------------------------------------------------------------------
    // ???
    //-------------------------------------------------------------------------

    public final UpdateQuery map(Resource node, Map<URI,String> values) {
        return this.map(null, node, null, values);
    }

    public final UpdateQuery map(URI srcGraph, Resource node,
                                               Map<URI,String> values) {
        return this.map(srcGraph, node, null, values);
    }

    public final UpdateQuery map(Resource from, Resource to,
                                                Map<URI,String> values) {
        return this.map(null, from, to, values);
    }

    public UpdateQuery map(URI srcGraph, Resource from,
                                         Resource to, Map<URI,String> values) {
        return this.map(srcGraph, from, to, values, false);
    }

    public UpdateQuery map(URI srcGraph, Resource from,
                       Resource to, Map<URI,String> values, boolean optional) {
        if (to == null) {
            // No target subject specified. => Assume the target subject URI
            // is the source one (the owning named graph may differ).
            to = from;
        }
        WhereType opt = (optional)? WhereType.OPTIONAL: null;
        // Parse mapped values and generates triples.
        for (Entry<URI,String> e : values.entrySet()) {
            URI p = e.getKey();
            String v = e.getValue();

            Value o = this.mapValue(v);
            if (o instanceof SparqlExpression) {
                if (o instanceof FunctionWrapper) {
                    FunctionWrapper w = (FunctionWrapper)o;
                    for (Entry<URI,Variable> e2 : w.vars.entrySet()) {
                        this.where(from, e2.getKey(),
                                         e2.getValue(), srcGraph, null, opt);
                    }
                }
                this.triple(to, p, (SparqlExpression)o, (URI)null);
            }
            else if (o instanceof URI) {
                // Node URI. => Use an intermediate SPARQL variable to create
                // the link between the triple to insert and the WHERE clause.
                URI u = (URI)o;
                Variable var = this.mapVariable(u);
                this.where(from, u, var, srcGraph, null, opt)
                    .triple(to, p, var, null);
            }
            else if (o instanceof Literal) {
                // Literal.
                this.triple(to, p, (Literal)o, null);
            }
            else {
                throw new IllegalArgumentException(p.toString() + " -> " + v);
            }
        }
        return this;
    }

    private final static Pattern fctPattern =
                                        Pattern.compile("(\\w+?)\\((.*?)\\)");

    protected Value mapValue(String s) {
        Value v = null;
        if (! isBlank(s)) {
            s = s.trim();
            if (s.charAt(0) == '"') {
                // Quoted string
                v = this.literal(s.substring(1, s.length() - 1));
            }
            else {
                Matcher m = fctPattern.matcher(s);
                if (m.matches()) {
                    // Function
                    String   f = m.group(1);
                    String[] p = m.group(2).split("\\s*,\\s*");

                    Value[] args = new Value[p.length];
                    Map<URI,Variable> vars = new HashMap<URI,Variable>();
                    for (int i=0; i<p.length; i++) {
                        Value x = this.mapValue(p[i]);
                        if (x instanceof URI) {
                            URI u = (URI)x;
                            Variable y = this.mapVariable(u);
                            vars.put(u, y);
                            x = y;
                        }
                        args[i] = x;
                    }
                    v = new FunctionWrapper(
                                    SparqlFunction.newFunction(f, args), vars);
                }
                else {
                    // Check for numbers.
                    if ((Character.isDigit(s.charAt(0))) ||
                        ((s.length() > 1) && (s.charAt(0) == '-')
                                          && (Character.isDigit(s.charAt(1))))) {
                        try {
                            if ((s.indexOf('.') != -1) || (s.indexOf(',') != -1)) {
                                // Double
                                v = this.literal(Double.parseDouble(
                                                        s.replace(',', '.')));
                            }
                            else {
                                // Integer
                                v = this.literal(Long.parseLong(s));
                            }
                        }
                        catch (Exception e) { /* Ignore... */ }
                    }
                    if (v == null) {
                        // No match yet. => Assume URI.
                        URI u = this.uri(s);
                        this.prefixFor(u.getNamespace());
                        v = u;
                    }
                }
            }
        }
        return v;
    }

    private Map<Variable,URI> mappedVariables = new HashMap<Variable,URI>();

    private Variable mapVariable(URI u) {
        Variable var = this.variable(u.getLocalName());
        URI x = this.mappedVariables.get(var);
        if ((x != null) && (! u.equals(x))) {
            // Same name but different URIs. => Create new variable.
            var = this.variable();
        }
        // Register variable.
        this.mappedVariables.put(var, u);
        return var;
    }

    /**
     * SPARQL <a href="http://www.w3.org/TR/sparql11-query/#bind">variable
     * assignment</a>.
     */
    private final static class Binding extends GraphPattern
    {
        public final Value expr;
        public final Variable v;

        public Binding(Value expr, Variable v) {
            super();
            this.expr = expr;
            this.v = v;
        }

        public String stringValue() {
            return "BIND(" + this.expr.stringValue()
                           + " AS " + this.v.stringValue() + ')';
        }
    }

    protected final static class Filter extends GraphPattern
    {
        public final Value expr;

        public Filter(Value expr) {
            this(expr, null);
        }

        public Filter(Value expr, URI graph) {
            super(graph);
            this.expr = expr;
        }

        public String stringValue() {
            return "FILTER(" + this.expr.stringValue() + ')';
        }
    }

    private final static class FunctionWrapper implements SparqlExpression
    {
        public final SparqlFunction fct;
        public final Map<URI,Variable> vars = new HashMap<URI,Variable>();

        public FunctionWrapper(SparqlFunction fct, Map<URI,Variable> vars) {
            this.fct = fct;
            this.vars.putAll(vars);
        }

        @Override
        public String stringValue() {
            return this.fct.stringValue();
        }
    }
}
