package org.datalift.tests;


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
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;

import org.datalift.fwk.rdf.RdfNamespace;

import static org.datalift.fwk.util.StringUtils.*;


public class ConstructQuery
{
    private final static URI RDF_TYPE = new URIImpl("rdf:a") {
            @Override
            public String stringValue() { return this.getLocalName(); }
        };

    private final static Map<String,String> tentativePrefixes =
                                                new HashMap<String,String>();

    static {
        for (RdfNamespace ns : RdfNamespace.values()) {
            tentativePrefixes.put(ns.uri, ns.prefix);
        }
    }

    private AtomicInteger prefixCount   = new AtomicInteger();
    private AtomicInteger variableCount = new AtomicInteger();
    private Map<String,String> prefix2Ns = new HashMap<String,String>();
    private Map<String,String> ns2Prefix = new HashMap<String,String>();
    private Collection<Statement> triples = new LinkedList<Statement>();
    private Collection<Statement> whereClauses = new LinkedList<Statement>();
    private Collection<Binding> bindings = new LinkedList<Binding>();

    public ConstructQuery addPrefix(String prefix, String ns) {
        this.prefix2Ns.put(prefix, ns);
        this.ns2Prefix.put(ns, prefix);
        return this;
    }

    public ConstructQuery addTriple(Resource s, URI p, String v) {
        return this.addTriple(s, p, this.literal(v));
    }
    public ConstructQuery addTriple(Resource s, URI p, SparqlExpression expr) {
        return this.addTriple(s, p, expr, null);
    }
    public ConstructQuery addTriple(Resource s, URI p,
                                    SparqlExpression expr, String var) {
        Variable v = this.variable(var);
        return this.addBinding(expr, v)
                   .addTriple(s, p, v);
    }
    public ConstructQuery addTriple(Resource s, URI p, Value o) {
        this.triples.add(new StatementImpl(s, p, o));
        return this;
    }

    public ConstructQuery rdfType(Resource s, URI t) {
        return this.addTriple(s, RDF_TYPE, t);
    }

    public ConstructQuery addWhereClause(Resource s, URI p, String v) {
        return this.addWhereClause(s, p, this.literal(v));
    }
    public ConstructQuery addWhereClause(Resource s, URI p, Value o) {
        this.whereClauses.add(new StatementImpl(s, p, o));
        return this;
    }

    public ConstructQuery addBinding(SparqlExpression expr, Variable v) {
        this.bindings.add(new Binding(expr, v));
        return this;
    }

    public String prefixFor(URI u) {
        String ns = u.getNamespace();
        String prefix = this.ns2Prefix.get(ns);
        if (prefix == null) {
            prefix = tentativePrefixes.get(ns);
            if (prefix == null) {
                prefix = "p" + this.prefixCount.incrementAndGet();
            }
            this.addPrefix(prefix, ns);
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

    public String nameFor(String ns, String name) {
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

    public Variable variable(URI u) {
        return this.variable(this.nameFor(u));
    }

    public Literal literal(String v) {
        return new LiteralImpl(v);
    }

    public BNode blankNode() {
        return this.blankNode((String)null);
    }

    public BNode blankNode(String name) {
        if (! isSet(name)) {
            name = this.nextVariable("b");
        }
        return new BNodeImpl(name);
    }

    public BNode blankNode(URI u) {
        return this.blankNode(this.nameFor(u));
    }

    private String nextVariable(String prefix) {
        return prefix + this.variableCount.incrementAndGet();
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

    private StringBuilder append(Collection<Statement> c, StringBuilder b) {
        List<Statement> stmts = new ArrayList<Statement>(c);
        Collections.sort(stmts, new Comparator<Statement>() {
                    @Override
                    public int compare(Statement t1, Statement t2) {
                        // Sort statements by subject name
                        int n = t1.getSubject().stringValue().compareTo(t2.getSubject().stringValue());
//                        if (n == 0) {
//                            n = t1.p.stringValue().compareTo(t2.p.stringValue());
//                        }
                        return n;
                    }
                });
        Resource s = null;
        URI p = null;
        for (Statement t : stmts) {
            if (t.getSubject().equals(s)) {
                b.setLength(b.length() - 2);
                if (t.getPredicate().equals(p)) {
                    b.append(",\n\t\t\t");
                }
                else {
                    p = t.getPredicate();
                    b.append(";\n\t\t").append(this.toString(p)).append(' ');
                }
            }
            else {
                s = t.getSubject();
                p = t.getPredicate();
                b.append("\t").append(this.toString(s)).append(' ')
                              .append(this.toString(p)).append(' ');
            }
            b.append(this.toString(t.getObject())).append(" .\n");
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

    private final static Pattern fctPattern = Pattern.compile("(\\w+?)\\((.*?)\\)");

    private static Value mapValue(String s, ConstructQuery c) {
        Value v = null;
        if (! isBlank(s)) {
            s = s.trim();
            if (s.charAt(0) == '"') {
                // Constant.
                v = c.literal(s.substring(1, s.length() - 1).trim());
            }
            else {
                Matcher m = fctPattern.matcher(s);
                if (m.matches()) {
                    // Function
                    String   f = m.group(1);
                    String[] p = m.group(2).split("\\s*,\\s*");
    
                    Value[] args = new Value[p.length];
                    for (int i=0; i<p.length; i++) {
                        Value x = mapValue(p[i], c);
                        args[i] = (x instanceof URI)?
                                        c.variable(((URI)x).getLocalName()): x;
                    }
                    v = SparqlFunction.newFunction(f, args);
                }
                else {
                    // Predicate match.
                    v = c.uri("src", s);
                }
            }
        }
        return v;
    }

    public static String vCardMapping(String srcNs, String addrType,
                                      Map<String,String> mapping) {
        final String VCARD = "http://www.w3.org/2006/vcard/ns#";

        ConstructQuery c = new ConstructQuery();
        c.addPrefix("v", VCARD)
         .addPrefix("src", srcNs);

        Variable node = c.variable("s");
        Resource addr = c.blankNode();

        c.rdfType(node, c.uri(VCARD, "VCard"))
         .rdfType(addr, c.uri(VCARD, addrType))
         .addTriple(node, c.uri(VCARD, "adr"), addr);

        for (Entry<String,String> e : mapping.entrySet()) {
            String attr = e.getKey();
            URI p       = c.uri(VCARD, attr);
            Resource r  = ("fn".equals(attr))? node: addr;

            String v = e.getValue();
            Value  o = mapValue(v, c);
            if (o instanceof SparqlExpression) {
                c.addTriple(r, p, (SparqlExpression)o);
            }
            else if (o instanceof URI) {
                // Predicate.
                URI u = (URI)o;
                Variable var = c.variable(u.getLocalName());
                c.addWhereClause(node, u, var)
                 .addTriple(r, p, var);

            }
            else if (o instanceof Literal) {
                // Literal.
                c.addTriple(r, p, (Literal)o);
            }
        }
        return c.toString();
    }

    public static void main(String[] args) throws Exception {
        final String SRC = "http://localhost:9091/datalift/project/kiosque/source/kiosques-ouverts-aaa-paris-csv-rdf-1/";

        Map<String,String> m = new HashMap<String,String>();
        m.put("street-address", "adresse");
        m.put("fn", "concat(\"Kiosque \",adresse)");
        m.put("country-name", "\"France\"");
        m.put("locality", "\"Paris\"");
        m.put("postal-code", "arrdt");
        System.out.println(vCardMapping(SRC, "Work", m));

/*
        // Old code:
        final String VCARD = "http://www.w3.org/2006/vcard/ns#";

        ConstructQuery c = new ConstructQuery();
        c.addPrefix("v", VCARD)
         .addPrefix("src", SRC);

        URI arrdt   = c.uri(SRC, "arrdt");
        URI adresse = c.uri(SRC, "adresse");

        Variable s = c.variable("s");
        Variable vAddr = c.variable(adresse);
        Variable vZip  = c.variable(arrdt);
        Resource b = c.blankNode();

        c.rdfType(b, c.uri(VCARD, "Work"))
         .addWhereClause(s, adresse, vAddr)
         .addTriple(b, c.uri(VCARD, "street-address"), vAddr)
         .addWhereClause(s, arrdt, vZip)
         .addTriple(b, c.uri(VCARD, "postal-code"), vZip)
         .addTriple(b, c.uri(VCARD, "locality"), "Paris")
         .addTriple(b, c.uri(VCARD, "country-name"), "France")
         .rdfType(s, c.uri(VCARD, "VCard"))
         .addTriple(s, c.uri(VCARD, "fn"),
                       new Concat(c.literal("Kiosque "), vAddr))
         .addTriple(s, c.uri(VCARD, "adr"), b);
*/
    }
}


final class VariableImpl implements Variable
{
    public final String name;

    public VariableImpl(String name) {
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

class Binding
{
    public final Value expr;
    public final Variable v;

    public Binding(Value expr, Variable v) {
        this.expr = expr;
        this.v = v;
    }

    public String stringValue() {
        return "BIND(" + this.expr.stringValue()
                       + " AS " + this.v.stringValue() + ')';
    }

    @Override
    public String toString() {
        return this.stringValue();
    }
}
