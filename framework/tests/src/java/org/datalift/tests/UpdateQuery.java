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

package org.datalift.tests;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.NumericLiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;

import org.datalift.fwk.rdf.RdfNamespace;

import static org.datalift.fwk.util.StringUtils.*;


public abstract class UpdateQuery
{
    private final static URI RDF_TYPE = new URIImpl("rdf:a");

    private final static Map<String,String> tentativePrefixes =
                                                new HashMap<String,String>();

    static {
        for (RdfNamespace ns : RdfNamespace.values()) {
            tentativePrefixes.put(ns.uri, ns.prefix);
        }
    }

    private final String name;
    private final URI targetGraph;
    private final AtomicInteger prefixCount   = new AtomicInteger();
    private final AtomicInteger variableCount = new AtomicInteger();
    private final Map<String,String> prefix2Ns = new HashMap<String,String>();
    private final Map<String,String> ns2Prefix = new HashMap<String,String>();
    private final Collection<Statement> triples = new LinkedList<Statement>();
    private final WhereClauses whereClauses = new WhereClauses();
    private final Map<String,WhereClauses> namedWhereClauses =
                                            new HashMap<String,WhereClauses>();
    private final Collection<Binding> bindings = new LinkedList<Binding>();

    protected UpdateQuery(String queryName) {
        this(queryName, null);
    }

    protected UpdateQuery(String queryName, URI targetGraph) {
        this.name = queryName.toUpperCase();
        this.targetGraph = targetGraph;
    }

    public URI getTargetGraph() {
        return this.targetGraph;
    }

    public UpdateQuery prefix(String prefix, String ns) {
        this.prefix2Ns.put(prefix, ns);
        this.ns2Prefix.put(ns, prefix);
        return this;
    }

    public final UpdateQuery triple(Resource s, URI p, String v) {
        return this.triple(s, p, v, null);
    }
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
    public final UpdateQuery triple(Resource s, URI p, Value o) {
        return this.triple(s, p, o, null);
    }
    public UpdateQuery triple(Resource s, URI p, Value o, URI graph) {
        this.triples.add(this.statement(s, p, o, graph));
        return this;
    }

    public final UpdateQuery rdfType(Resource s, URI t) {
        return this.rdfType(s, t, null);
    }

    public UpdateQuery rdfType(Resource s, URI t, URI graph) {
        return this.triple(s, RDF_TYPE, t, graph);
    }

    public final UpdateQuery where(Resource s, URI p, String v) {
        return this.where(s, p, v, null);
    }
    public final UpdateQuery where(Resource s, URI p, String v, URI graph) {
        return this.where(s, p, this.literal(v), graph);
    }
    public final UpdateQuery where(Resource s, URI p, Value o) {
        return this.where(s, p, o, null);
    }
    public final UpdateQuery where(Resource s, URI p, Value o, URI graph) {
        return this.where(s, p, o, graph, (WhereClauses)null);
    }
    public final UpdateQuery where(Resource s, URI p, Value o, URI graph,
                                                               String group) {
        return this.where(s, p, o, graph, this.whereGroup(group));
    }
    public final UpdateQuery where(Resource s, URI p, Value o, URI graph,
                                   String groupKey, WhereType groupType) {
        WhereClauses w = null;
        if (isSet(groupKey)) {
            if (groupType == null) {
                groupType = WhereType.UNION;
            }
            w = this.whereGroup(groupKey, groupType, this.whereClauses);
        }
        return this.where(s, p, o, graph, w);
    }
    public UpdateQuery where(Resource s, URI p, Value o, URI graph,
                                                         WhereClauses group) {
        if (group == null) {
            group = this.whereClauses;
        }
        group.add(this.statement(s, p, o, graph));
        return this;
    }

    public final WhereClauses whereGroup(String key) {
        return this.whereGroup(key, null, (WhereClauses)null);
    }
    public final WhereClauses whereGroup(WhereType type) {
        return this.whereGroup(null, type, (WhereClauses)null);
    }
    public final WhereClauses whereGroup(String key, WhereType type,
                                                     String parent) {
        return this.whereGroup(key, type, this.whereGroup(parent));
    }
    public WhereClauses whereGroup(String key, WhereType type,
                                               WhereClauses parent) {
        WhereClauses w = null;
        if (isSet(key)) {
            w = this.namedWhereClauses.get(key);
        }
        if ((w == null) && (type != null)) {
            if (! isSet(key)) {
                key = this.nextVariable("w");
            }
            w = new WhereClauses(type, key,
                                 (parent != null)? parent: this.whereClauses);
        }
        if (w == null) {
            w = this.whereClauses;
        }
        return w;
    }

    public UpdateQuery bind(SparqlExpression expr, Variable v) {
        this.bindings.add(new Binding(expr, v));
        return this;
    }

    public final String prefixFor(URI u) {
        return this.prefixFor(u.getNamespace());
    }

    public String prefixFor(String ns) {
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

    public URI uri(String ns, String name) {
        String uri = this.prefix2Ns.get(ns);
        if (uri != null) {
            ns = uri;
        }
        return new URIImpl(ns + name);
    }

    public final String nameFor(String ns, String name) {
        return this.nameFor(this.uri(ns, name));
    }

    public String nameFor(URI u) {
        return this.prefixFor(u) + '_' + u.getLocalName();
    }

    public Variable variable(String name) {
        if (! isSet(name)) {
            name = this.nextVariable("v");
        }
        return new VariableImpl(name);
    }

    public final Variable variable(URI u) {
        return this.variable(this.nameFor(u));
    }

    public Literal literal(String v) {
        return new LiteralImpl(v);
    }

    public Literal literal(int i) {
        return new NumericLiteralImpl(i);
    }

    public final BNode blankNode() {
        return this.blankNode((String)null);
    }

    public BNode blankNode(String name) {
        if (! isSet(name)) {
            name = this.nextVariable("b");
        }
        return new BNodeImpl(name);
    }

    public final BNode blankNode(URI u) {
        return this.blankNode(this.nameFor(u));
    }

    private String nextVariable(String prefix) {
        return prefix + this.variableCount.incrementAndGet();
    }

    public String toString() {
        StringBuilder b = new StringBuilder(1024);
        // Namespace prefix declarations
        for (Entry<String,String> e : this.prefix2Ns.entrySet()) {
            b.append("PREFIX ").append(e.getKey())
             .append(": <").append(e.getValue()).append(">\n");
        }
        b.append('\n');
        // With graph
        URI graph = this.getTargetGraph();
        if (graph != null) {
            b.append("WITH <").append(graph.toString()).append(">\n");
        }
        // Query type
        b.append(this.name).append(" {\n");
        // Triples, grouped by graph.
        b = this.append(this.triples, b).append("}\n");
        // WHERE clauses, grouped by graph.
        if (! this.whereClauses.isEmpty()) {
            b.append("WHERE {\n");
            b = this.append(this.whereClauses, b, 0);
        }
        // Local variable bindings
        for (Binding bnd : this.bindings) {
            b.append("\t").append(bnd).append('\n');
        }
        b.append('}');
        return b.toString();
    }

    private StringBuilder append(WhereClauses c, StringBuilder b, int level) {
        if (level > 0) {
            b.append('\t');
            if (c.type != WhereType.DEFAULT) {
                b.append(c.type).append(' ');
            }
            b.append("{\n");
        }
        this.append(c.clauses, b);
        for (WhereClauses w : c.children) {
            this.append(w, b, level + 1);
        }
        if (level > 0) {
            b.append("\t}\n");
        }
        return b;
    }

    private StringBuilder append(Collection<Statement> c, StringBuilder b) {
        List<Statement> stmts = new ArrayList<Statement>(c);
        Collections.sort(stmts, new Comparator<Statement>() {
                    @Override
                    public int compare(Statement t1, Statement t2) {
                        int n = 0;
                        // Sort statements by named group.
                        if (t1.getContext() != null) {
                            n = (t2.getContext() != null)?
                                    t1.getContext().stringValue().compareTo(
                                                t2.getContext().stringValue()):
                                    1;
                        }
                        else {
                            n = (t2.getContext() != null)? -1: 0;
                        }
                        // Then by subject name.
                        if (n == 0) {
                            n = t1.getSubject().stringValue().compareTo(
                                                t2.getSubject().stringValue());
                        }
                        return n;
                    }
                });
        Resource graph = null;
        boolean inGraph = false;
        Resource s = null;
        URI p = null;
        for (Statement t : stmts) {
            Resource g = t.getContext();
            if ((inGraph) && ((g == null) || (! g.equals(graph)))) {
                    // Close named graph scope.
                    b.append("\t}\n");
                    inGraph = false;
                    graph = null;
                    s = null;
                    p = null;
            }
            if ((! inGraph) && (g != null)) {
                    // Open named graph scope.
                    b.append("\tGRAPH <").append(g.toString()).append("> {\n");
                    inGraph = true;
                    graph = g;
            }

            if (t.getSubject().equals(s)) {
                b.setLength(b.length() - 2);
                if (t.getPredicate().equals(p)) {
                    b.append(",\n\t\t\t\t");
                }
                else {
                    p = t.getPredicate();
                    b.append(";\n\t\t\t").append(this.toString(p)).append(' ');
                }
            }
            else {
                s = t.getSubject();
                p = t.getPredicate();
                b.append("\t\t").append(this.toString(s)).append('\t')
                                .append(this.toString(p)).append(' ');
            }
            b.append(this.toString(t.getObject())).append(" .\n");
        }
        if (inGraph) {
            // Close named graph scope.
            b.append("\t}\n");
            inGraph = false;
        }
        return b;
    }

    private String toString(Value v) {
        if (v instanceof URI) {
            URI u = (URI)v;
            String nsUri = u.getNamespace();
            String prefix = this.ns2Prefix.get(nsUri);
            if ((prefix == null) && ("rdf:".equalsIgnoreCase(nsUri))) {
                nsUri = null;
            }
            return (prefix != null)? prefix + ':' + u.getLocalName():
                   (nsUri  != null)? "<" + v.toString() + '>': u.getLocalName();
        }
        else {
            return v.toString();
        }
    }

    protected Statement statement(Resource s, URI p, Value o, URI graph) {
        return ((graph != null)? new ContextStatementImpl(s, p, o, graph):
                                 new StatementImpl(s, p, o));
    }

    protected final UpdateQuery map(Resource node, Map<URI,String> values) {
        return this.map(null, node, null, values);
    }

    protected final UpdateQuery map(URI srcGraph, Resource node,
                                                  Map<URI,String> values) {
        return this.map(srcGraph, node, null, values);
    }

    protected final UpdateQuery map(Resource from, Resource to,
                                                   Map<URI,String> values) {
        return this.map(null, from, to, values);
    }

    protected UpdateQuery map(URI srcGraph, Resource from,
                              Resource to, Map<URI,String> values) {
        if (to == null) {
            // No target subject specified. => Assume the target subject URI
            // is the source one (the owning named graph may differ).
            to = from;
        }
        // Parse mapped values and generates triples.
        for (Entry<URI,String> e : values.entrySet()) {
            URI p = e.getKey();
            String v = e.getValue();

            Value o = this.mapValue(v);
            if (o instanceof SparqlExpression) {
                this.triple(to, p, (SparqlExpression)o, (URI)null);
            }
            else if (o instanceof URI) {
                // Node URI. => Use an intermediate SPARQL variable to create
                // the link between the triple to insert and the WHERE clause.
                URI u = (URI)o;
                Variable var = this.variable(u.getLocalName());
                this.where(from, u, var, srcGraph)
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
                    for (int i=0; i<p.length; i++) {
                        Value x = this.mapValue(p[i]);
                        args[i] = (x instanceof URI)?
                                    this.variable(((URI)x).getLocalName()): x;
                    }
                    v = SparqlFunction.newFunction(f, args);
                }
                else {
                    // Check for integer.
                    if (Character.isDigit(s.charAt(0))) {
                        try {
                            v = this.literal(Integer.parseInt(s));
                        }
                        catch (Exception e) { /* Ignore... */ }
                    }
                    if (v == null) {
                        // No match yet. => Assume URI.
                        int i = s.indexOf(':');
                        if (i != -1) {
                            v = this.uri(s.substring(0, i), s.substring(i + 1));
                        }
                        else {
                            v = this.uri("src", s);
                        }
                    }
                }
            }
        }
        return v;
    }


    private static enum WhereType {
        DEFAULT         (""),
        OPTIONAL        ("OPTIONAL"),
        UNION           ("UNION"),
        MINUS           ("MINIUS");

        public final String label;

        WhereType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    final class WhereClauses implements Iterable<Statement>
    {
        // public final String name;
        public final WhereType type;
        public final Collection<Statement> clauses = new LinkedList<Statement>();
        public Collection<WhereClauses> children = new LinkedList<WhereClauses>();

        public WhereClauses() {
            this(WhereType.DEFAULT, null, null);
        }

        public WhereClauses(String name, WhereClauses parent) {
            this(WhereType.DEFAULT, name, parent);
        }

        public WhereClauses(WhereType type, String name, WhereClauses parent) {
            if (type == null) {
                throw new IllegalArgumentException("type");
            }
            // this.name = name;
            this.type = type;
            if (parent != null) {
                parent.add(this);
            }
            if (isSet(name)) {
                namedWhereClauses.put(name, this);
            }
        }

        public WhereClauses add(Statement s) {
            this.clauses.add(s);
            return this;
        }

        public boolean isEmpty() {
            return this.clauses.isEmpty();
        }

        private WhereClauses add(WhereClauses child) {
            this.children.add(child);
            return this;
        }

        @Override
        public Iterator<Statement> iterator() {
            return this.clauses.iterator();
        }
    }

    final static class VariableImpl implements Variable
    {
        public final String name;

        public VariableImpl(String name) {
            if (! isSet(name)) {
                throw new IllegalArgumentException("name");
            }
            if (name.charAt(0) == '?') {
                name = name.substring(1);
            }
            this.name = name;
        }

        @Override
        public String stringValue()  {
            return "?" + this.name;
        }

        @Override
        public String toString() {
            return this.stringValue();
        }

        @Override
        public int hashCode() {
            return this.stringValue().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof VariableImpl)?
                this.stringValue().equals(((VariableImpl)o).stringValue()): false;
        }
    }

    final static URI EMPTY_URI = new URI() {
        @Override
        public String getLocalName() {
            return "";
        }

        @Override
        public String getNamespace() {
            return "";
        }

        @Override
        public String stringValue() {
            return "";
        }

        @Override
        public String toString() {
            return this.stringValue();
        }
    };

    abstract static class GraphPattern implements Statement
    {
        public final URI graph;

        protected GraphPattern(URI graph) {
            this.graph = graph;
        }

        abstract String stringValue();

        @Override
        public final String toString() {
            return this.stringValue();
        }

        @Override
        public final Resource getContext() {
            return this.graph;
        }

        @Override
        public final Resource getSubject() {
            return EMPTY_URI;
        }

        @Override
        public final URI getPredicate() {
            return EMPTY_URI;
        }

        @Override
        public final Value getObject() {
            final String v = this.stringValue();
            return new Value() {
                    @Override public String stringValue() { return v; }
                    @Override public String toString()    { return v; }
            };
        }
    }

    final static class Binding extends GraphPattern
    {
        public final Value expr;
        public final Variable v;

        public Binding(Value expr, Variable v) {
            this(expr, v, null);
        }

        public Binding(Value expr, Variable v, URI graph) {
            super(graph);
            this.expr = expr;
            this.v = v;
        }

        public String stringValue() {
            return "BIND(" + this.expr.stringValue()
                           + " AS " + this.v.stringValue() + ')';
        }
    
    }

    final static class Filter extends GraphPattern
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
}
