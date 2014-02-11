package org.datalift.owl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datalift.fwk.log.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;

import static org.openrdf.query.QueryLanguage.SPARQL;


public class OwlParser {
	
	private final static Logger log = Logger.getLogger();
	
	private final static AnnotationProperty RDFS_LABEL =
            new AnnotationProperty(RDFS.LABEL.stringValue(), "rdfs:label",
                                   "Human-readable version of a resource's name");
    private final static AnnotationProperty RDFS_COMMENT =
            new AnnotationProperty(RDFS.COMMENT.stringValue(), "rdfs:comment",
                                   "Human-readable description of a resource");
    private final static AnnotationProperty RDFS_SEEALSO =
            new AnnotationProperty(RDFS.SEEALSO.stringValue(), "rdfs:seeAlso",
                                   "Resource that might provide additional information about the subject resource");
    private final static AnnotationProperty RDFS_ISDEFINEDBY =
            new AnnotationProperty(RDFS.ISDEFINEDBY.stringValue(), "rdfs:isDefinedBy",
                                   "Resource defining the subject resource");
    
    private final static String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";
    private final static String RDFS_LITERAL = "http://www.w3.org/2000/01/rdf-schema#Literal";

    public OwlParser() {
        super();
    }

    public Ontology parse(File f, String baseUri) throws OpenRDFException {
        if (f == null) {
            throw new IllegalArgumentException("f");
        }
        if (! f.canRead()) {
            throw new IllegalArgumentException(
                                        new FileNotFoundException(f.getPath()));
        }
        Repository store = this.newRepository();
        try {
            // Load ontology into memory store.
            this.load(f, baseUri, store);
            // Build Java representation from RDF model.
            return this.newOntology(store);
        }
        finally {
            try { store.shutDown(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    public Ontology parse(InputStream in, String baseUri, RDFFormat format)
                                                    throws OpenRDFException {
        if (in == null) {
            throw new IllegalArgumentException("in");
        }
        if (format == null) {
            format = RDFFormat.RDFXML;
        }
        Repository store = this.newRepository();
        try {
            // Load ontology into memory store.
            this.load(in, format, baseUri, store);
            // Build Java representation from RDF model.
            return this.newOntology(store);
        }
        finally {
            try { store.shutDown(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    public Ontology parse(Reader in, String baseUri, RDFFormat format)
                                                    throws OpenRDFException {
        if (in == null) {
            throw new IllegalArgumentException("in");
        }
        if (format == null) {
            format = RDFFormat.RDFXML;
        }
        Repository store = this.newRepository();
        try {
            // Load ontology into memory store.
            this.load(in, format, baseUri, store);
            // Build Java representation from RDF model.
            return this.newOntology(store);
        }
        finally {
            try { store.shutDown(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    private Ontology newOntology(Repository store) throws OpenRDFException {
        Ontology o = null;

        RepositoryConnection cnx = store.getConnection();
        try {
            final Map<String,OwlClass> classes = new HashMap<String,OwlClass>();
            final Map<String,OwlProperty> properties =
                                            new HashMap<String,OwlProperty>();
            // Extract classes.
            String query = "PREFIX owl:  <" + OWL.NAMESPACE  + "> "   +
                           "PREFIX rdfs: <" + RDFS.NAMESPACE + "> "   +
                           "SELECT * WHERE {" +
                           "  ?uri a owl:Class ." +
                           "  OPTIONAL { ?uri rdfs:label   ?name . }"   +
                           "  OPTIONAL { ?uri rdfs:comment ?desc . } }";
            TupleQueryResult rs = cnx.prepareTupleQuery(SPARQL, query).evaluate();
            while (rs.hasNext()) {
                BindingSet bs = rs.next();
                String uri = v(bs, "uri");
                if (uri != null) {
                    classes.put(uri, new OwlClass(uri, v(bs, "name"),
                                                       v(bs, "desc")));
                }
            }
            rs.close();
            rs = null;
            // Load class hierarchy.
            query = "PREFIX rdf:  <" + RDF.NAMESPACE  + "> "   +
                    "PREFIX owl:  <" + OWL.NAMESPACE  + "> "   +
                    "PREFIX rdfs: <" + RDFS.NAMESPACE + "> "   +
                    "SELECT ?uri ?super ?parent WHERE {" +
                    "  ?uri a owl:Class ; rdfs:subClassOf ?super ." +
                    "  OPTIONAL {" +
                    "    ?super owl:unionOf ?x ." +
                    "    ?x rdf:rest*/rdf:first ?parent . } }";
            rs = cnx.prepareTupleQuery(SPARQL, query).evaluate();
            while (rs.hasNext()) {
                BindingSet bs = rs.next();
                OwlClass c = classes.get(v(bs, "uri"));
                if (c != null) {
                    c.parent(classes.get(v(bs, "super")));
                    c.parent(classes.get(v(bs, "parent")), true);
                }
            }
            rs.close();
            rs = null;
            // Load class exclusions.
            query = "PREFIX owl:  <" + OWL.NAMESPACE  + "> "   +
                    "SELECT ?uri ?disjoint WHERE {" +
                    "  ?uri a owl:Class ; owl:disjointWith ?disjoint . }";
            rs = cnx.prepareTupleQuery(SPARQL, query).evaluate();
            while (rs.hasNext()) {
                BindingSet bs = rs.next();
                OwlClass c = classes.get(v(bs, "uri"));
                if (c != null) {
                    c.disjoint(classes.get(v(bs, "disjoint")));
                }
            }
            rs.close();
            rs = null;
            // Extract properties.
            query = "PREFIX owl:  <" + OWL.NAMESPACE  + "> "   +
                    "PREFIX rdf:  <" + RDF.NAMESPACE  + "> "   +
                    "PREFIX rdfs: <" + RDFS.NAMESPACE + "> "   +
                    "SELECT * WHERE {" +
                    "  { ?uri a rdf:Property . }" +
                    "  UNION { ?uri a owl:ObjectProperty . }" +
                    "  UNION { ?uri a owl:DatatypeProperty . }" +
                    "  UNION { ?uri a owl:AnnotationProperty . }" +
                    "  ?uri a ?type ." +
                    "  OPTIONAL { ?uri rdfs:label   ?name .   }" +
                    "  OPTIONAL { ?uri rdfs:comment ?desc .   }" +
                    "  OPTIONAL { ?uri rdfs:domain  ?domain . }" +
                    "  OPTIONAL { ?uri rdfs:range   ?range .  } }";
            rs = cnx.prepareTupleQuery(SPARQL, query).evaluate();
            final List<String> hasClassRange = new ArrayList<String>();
            while (rs.hasNext()) {
                BindingSet bs = rs.next();
                String uri = v(bs, "uri");
                if (uri != null) {
                    OwlProperty p = null;
                    OwlProperty pOld = properties.get(uri);
                    String name = v(bs, "name");
                    String desc = v(bs, "desc");
                    OwlClass c = classes.get(v(bs, "domain"));
                    Value range = bs.getValue("range");
                    if (p == null) {
                        Value type = bs.getValue("type");
                        if (OWL.OBJECTPROPERTY.equals(type)) {
                            p = new ObjectProperty(uri, name, desc);
                        }
                        else if (OWL.DATATYPEPROPERTY.equals(type)) {
                            p = new DatatypeProperty(uri, name, desc);
                        }
                        else if (OWL.ANNOTATIONPROPERTY.equals(type)) {
                            p = new AnnotationProperty(uri, name, desc);
                        }
                        else {
                            // RDF Property. => Check range.
							if ((range != null)
									&& ( ! range.stringValue().startsWith(
											XMLSchema.NAMESPACE))
									&& ( ! range.stringValue().equalsIgnoreCase(
											OWL_THING))
									&& ( ! range.stringValue().equalsIgnoreCase(
											RDFS_LITERAL))) {
                                p = new ObjectProperty(uri, name, desc);
                            	hasClassRange.add(uri);
                            }
                        }
                        if (p == null) {
                            p = new DatatypeProperty(uri, name, desc);
                        }
                        boolean isDatatype = p instanceof DatatypeProperty;
                        if (pOld == null) {
                        		properties.put(uri, p);
                        }
                        else {
                        	if (hasClassRange.contains(uri)) {
                        		if (p instanceof ObjectProperty) {
                    				properties.put(uri, p);
                    			}
                        		
                        	}
                        	else {
                        		if (isDatatype) {
                        			properties.put(uri, p);
                        		}
                        	}
                        }
                    }
                    if (c != null) {
                        c.property(p);
                        p.domain(c);
                    }
                    if (range != null) {
                        String type = range.stringValue();
                        c = classes.get(type);
                        if (c != null) {
                            p.range(c);
                        }
                        else {
                            p.range(type);
                        }
                    }
                }
            }
            rs.close();
            rs = null;
            // Add OWL built-in annotation properties.
            properties.put(RDFS_LABEL.uri(),       RDFS_LABEL);
            properties.put(RDFS_COMMENT.uri(),     RDFS_COMMENT);
            properties.put(RDFS_SEEALSO.uri(),     RDFS_SEEALSO);
            properties.put(RDFS_ISDEFINEDBY.uri(), RDFS_ISDEFINEDBY);
            // Extract ontology attributes.
            query = "PREFIX owl: <" + OWL.NAMESPACE  + "> " +
                    "PREFIX dc:  <http://purl.org/dc/elements/1.1/> " +
                    "SELECT * WHERE {" +
                    "  OPTIONAL { ?uri a owl:Ontology . }" +
                    "  OPTIONAL { ?uri dc:title       ?title . }" +
                    "  OPTIONAL { ?uri dc:description ?desc  . } }";
            rs = cnx.prepareTupleQuery(SPARQL, query).evaluate();
            if (rs.hasNext()) {
                BindingSet bs = rs.next();
                o = new Ontology(v(bs, "uri"), v(bs, "title"), v(bs, "desc"),
                                 classes, properties);
            }
            else {
                throw new QueryResultParseException("No ontology found");
            }
        }
        finally {
            try { cnx.close();  } catch (Exception e) { /* Ignore... */ }
        }
        return o;
    }

    private String v(BindingSet bs, String name) {
        String s = null;
        Value v = bs.getValue(name);
        if (v instanceof Literal) {
            s = ((Literal)v).getLabel();
        }
        else if (v instanceof URI) {
            s = v.stringValue();
        }
        return s;
    }

    private Repository newRepository() throws OpenRDFException {
        Repository store = new SailRepository(new MemoryStore());
        store.initialize();
        return store;
    }

    private void load(File f, String baseUri, Repository store)
                                                    throws OpenRDFException {
        RepositoryConnection cnx = store.getConnection();
        try {
            if (baseUri == null) {
                baseUri = "";
            }
            // Load ontology into RDF store.
            cnx.add(f, baseUri, Rio.getParserFormatForFileName(
                                            f.getPath(), RDFFormat.RDFXML));
        }
        catch (IOException e) {
            throw new RDFParseException(e);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    private void load(InputStream in, RDFFormat format,
                      String baseUri, Repository store)
                                                    throws OpenRDFException {
        RepositoryConnection cnx = store.getConnection();
        try {
            if (baseUri == null) {
                baseUri = "";
            }
            // Load ontology into RDF store.
            cnx.add(in, baseUri, format);
        }
        catch (IOException e) {
            throw new RDFParseException(e);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    private void load(Reader in, RDFFormat format,
                      String baseUri, Repository store)
                                                    throws OpenRDFException {
        RepositoryConnection cnx = store.getConnection();
        try {
            if (baseUri == null) {
                baseUri = "";
            }
            // Load ontology into RDF store.
            cnx.add(in, baseUri, format);
        }
        catch (IOException e) {
            throw new RDFParseException(e);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }
}
