package org.datalift.tests;


import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import org.datalift.fwk.util.StringUtils;


public class ConstructQuery
{
    private AtomicInteger prefixCount   = new AtomicInteger();
    private AtomicInteger variableCount = new AtomicInteger();
    private ConcurrentMap<String,String> prefix2Ns = new ConcurrentHashMap<String,String>();
    private ConcurrentMap<String,String> ns2Prefix = new ConcurrentHashMap<String,String>();
    private Collection<TripleStatement> triples = new LinkedList<TripleStatement>();
    private Collection<TripleStatement> whereClauses = new LinkedList<TripleStatement>();
    private Collection<Binding> bindings = new LinkedList<Binding>();

    public ConstructQuery addPrefix(String prefix, String ns) {
        this.prefix2Ns.put(prefix, ns);
        this.ns2Prefix.put(ns, prefix);
        return this;
    }

    public ConstructQuery addTriple(Resource s, URI p, String v) {
        return this.addTriple(new TripleStatement(s, p, this.literal(v)));
    }
    public ConstructQuery addTriple(Resource s, URI p, Value o) {
        return this.addTriple(new TripleStatement(s, p, o));
    }
    public ConstructQuery addTriple(TripleStatement t) {
        this.triples.add(t);
        return this;
    }

    public ConstructQuery addWhereClause(Resource s, URI p, Value o) {
        return this.addWhereClause(new TripleStatement(s, p, o));
    }
    public ConstructQuery addWhereClause(TripleStatement t) {
        this.whereClauses.add(t);
        return this;
    }

    public ConstructQuery addBinding(SparqlExpression expr, Variable v) {
        return this.addBinding(new Binding(expr, v));
    }
    public ConstructQuery addBinding(Binding b) {
        this.bindings.add(b);
        return this;
    }

    public String prefixFor(URI u) {
        String ns = u.getNamespace();
        String prefix = this.ns2Prefix.get(ns);
        if (prefix == null) {
            prefix = "p" + this.prefixCount.incrementAndGet();
            prefix = this.ns2Prefix.putIfAbsent(ns, prefix);
        }
        return prefix;
    }

    public URI uri(String uri) {
/*
        String[] elts = uri.split(":");
        String nsUri = (elts.length == 2)? this.prefix2Ns.get(elts[0]): null;
        return (nsUri != null)? new PrefixedUri(elts[0], nsUri, elts[1]):
                                new URIImpl(uri);
 */
        return new URIImpl(uri);
    }

    public URI uri(String ns, String name) {
        String uri = this.prefix2Ns.get(ns);
        return (uri != null)? new PrefixedUri(ns, uri, name):
                              new URIImpl(ns + name);
    }

    public String nameFor(String uri) {
        return this.nameFor(this.uri(uri));
    }

    public String nameFor(String ns, String name) {
        String uri = this.prefix2Ns.get(ns);
        if (uri == null) {
            uri = ns;
        }
        return this.nameFor(this.uri(uri, name));
    }

    public String nameFor(URI u) {
        String name = u.getLocalName();
        if (! StringUtils.isSet(name)) {
            name = "v" + this.variableCount.incrementAndGet();
        }
        return this.prefixFor(u) + '_' + name;
    }

    public Variable variable(String name) {
        return new Variable(name);
    }

    public Variable variable(URI u) {
        return this.variable(this.nameFor(u));
    }

    public Literal literal(String v) {
        return new LiteralImpl(v);
    }

    public BNode blankNode(String name) {
        return new BlankNode(name);
    }

    public BNode blankNode(URI u) {
        return this.blankNode(nameFor(u));
    }

    public String toString() {
        StringBuilder b = new StringBuilder(1024);
        // Namespace prefix declarations.
        for (Entry<String,String> e : this.prefix2Ns.entrySet()) {
            b.append("PREFIX ").append(e.getKey())
             .append(": <").append(e.getValue()).append(">\n");
        }
        // CONSTRUCT statements.
        b.append("\nCONSTRUCT {\n");
        b = this.append(this.triples, b);
        // WHERE clauses.
        b.append("}\nWHERE {\n");
        b = this.append(this.whereClauses, b);
        // Local variable bindings.
        for (Binding bnd : this.bindings) {
            b.append('\t').append(bnd).append('\n');
        }
        b.append('}');
        return b.toString();
    }

    private StringBuilder append(Collection<TripleStatement> c, StringBuilder b) {
        Collection<TripleStatement> stmts = new TreeSet<TripleStatement>(
                new Comparator<TripleStatement>() {
                    @Override
                    public int compare(TripleStatement t1, TripleStatement t2) {
                        int n = t1.s.stringValue().compareTo(t2.s.stringValue());
                        if (n == 0) {
                            n = t1.p.stringValue().compareTo(t2.p.stringValue());
                        }
                        if (n == 0) {
                            n = t1.o.stringValue().compareTo(t2.o.stringValue());
                        }
                        return n;
                    }
                });
        stmts.addAll(c);
        Resource s = null;
        URI p = null;
        for (TripleStatement t : stmts) {
            if (t.s.equals(s)) {
                b.setLength(b.length() - 2);
                if (t.p.equals(p)) {
                    b.append(",\n\t\t\t");
                }
                else {
                    b.append(";\n\t\t").append(this.toString(t.p)).append(' ');
                    p = t.p;
                }
            }
            else {
                b.append("\t").append(this.toString(t.s)).append(' ')
                              .append(this.toString(t.p)).append(' ');
                s = t.s;
                p = t.p;
            }
            b.append(this.toString(t.o)).append(" .\n");
        }
        return b;
    }
    private String toString(Value v) {
        return ((v instanceof URI) && (! (v instanceof PrefixedUri)))?
                                        '<' + v.toString() + '>': v.toString();
    }

    public static void main(String[] args) throws Exception {
        final String VCARD = "v";
        final String VCARD_NS = "http://www.w3.org/2006/vcard/ns#";
        final String SRC = "src";

        ConstructQuery c = new ConstructQuery();
        c.addPrefix(VCARD, VCARD_NS)
         .addPrefix(SRC, "http://localhost:9091/datalift/project/kiosque/source/kiosques-ouverts-aaa-paris-csv-rdf-1/");

        URI work    = c.uri(VCARD, "Work");
        URI arrdt   = c.uri(SRC, "arrdt");
        URI adresse = c.uri(SRC, "adresse");

        Variable s = c.variable("s");
        Variable vAddr = c.variable(adresse);
        Variable vZip  = c.variable(arrdt);
        Variable vFullName = c.variable("fn");
        BNode b = c.blankNode(work);

        c.addTriple(b, RDF.TYPE, work)
         .addWhereClause(s, adresse, vAddr)
         .addTriple(b, c.uri(VCARD, "street-address"), vAddr)
         .addWhereClause(s, arrdt, vZip)
         .addTriple(b, c.uri(VCARD, "postal-code"), vZip)
         .addTriple(b, c.uri(VCARD, "locality"), "Paris")
         .addTriple(b, c.uri(VCARD, "country-name"), "France")
         .addTriple(s, RDF.TYPE, c.uri(VCARD, "VCard"))
         .addTriple(s, c.uri(VCARD, "fn"), vFullName)
         .addBinding(new Concat(c.literal("Kiosque "), vAddr), vFullName)
         .addTriple(s, c.uri(VCARD, "adr"), b);

        System.out.println(c);
    }
}


class TripleStatement
{
    public final Resource s;
    public final URI p;
    public final Value o;

    public TripleStatement(Resource s, URI p, Value o) {
        this.s = s;
        this.p = p;
        this.o = o;
    }
}

final class Variable implements Resource
{
    public final String name;

    public Variable(String name) {
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
}

final class PrefixedUri implements URI
{
    public final String prefix;
    public final String uri;
    public final String name;

    public PrefixedUri(String prefix, String uri, String name) {
        this.prefix = prefix;
        this.uri    = uri;
        this.name   = name;
    }
    
    @Override public String getLocalName() { return this.name; }
    @Override public String getNamespace() { return this.uri;  }

    @Override
    public String stringValue() {
        return this.prefix + ":" + this.name;
    }
    @Override
    public String toString() {
        return this.stringValue();
    }
}

final class BlankNode extends BNodeImpl
{
    public BlankNode(String id) {
        super(id);
    }
}

class Binding
{
    public final SparqlExpression expr;
    public final Variable v;

    public Binding(SparqlExpression expr, Variable v) {
        this.expr = expr;
        this.v = v;
    }

    @Override
    public String toString() {
        return "BIND(" + this.expr + " AS " + this.v + ')';
    }
}

interface SparqlExpression {
    // Marker interface
}

final class Concat implements SparqlExpression {
    private final List<Value> l;

    public Concat(Value... v) {
        this.l = Arrays.asList(v);
    }

    @Override
    public String toString() {
        return "concat(" + StringUtils.join(this.l, ", ") + ')';
    }
}
