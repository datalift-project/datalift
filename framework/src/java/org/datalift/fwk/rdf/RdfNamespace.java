package org.datalift.fwk.rdf;


/**
 * A set of predefined, well-known RDF namespaces.
 *
 * @author lbihanic
 */
public enum RdfNamespace
{
    //-------------------------------------------------------------------------
    // Values
    //-------------------------------------------------------------------------

    /** datalift: &lt;http://www.datalift.org/core#&gt; */
    DataLift    ("datalift",    "http://www.datalift.org/core#"),
    /** dc: &lt;http://purl.org/dc/elements/1.1/&gt; */
    DC_Elements ("dc",          "http://purl.org/dc/elements/1.1/"),
    /** dcterms: &lt;http://purl.org/dc/terms/&gt; */
    DC_Terms    ("dcterms",     "http://purl.org/dc/terms/"),
    /** foaf: &lt;http://xmlns.com/foaf/0.1/&gt; */
    FOAF        ("foaf",        "http://xmlns.com/foaf/0.1/"),
    /** rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; */
    RDF         ("rdf",         "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    /** rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt; */
    RDFS        ("rdfs",        "http://www.w3.org/2000/01/rdf-schema#"),
    /** owl: &lt;http://www.w3.org/2002/07/owl#&gt; */
    OWL         ("owl",         "http://www.w3.org/2002/07/owl#"),
    /** void: &lt;http://rdfs.org/ns/void#&gt; */
    VOID        ("void",        "http://rdfs.org/ns/void#"),
    /** doap: &lt;http://usefulinc.com/ns/doap#&gt; */
    DOAP        ("doap",        "http://usefulinc.com/ns/doap#"),
    /** vdpp: &lt;http://data.lirmm.fr/ontologies/vdpp#&gt; */
    VDPP        ("vdpp",        "http://data.lirmm.fr/ontologies/vdpp#"),
    /** prv: &lt;http://purl.org/net/provenance/ns#&gt; */
    PRV         ("prv",         "http://purl.org/net/provenance/ns#");

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The namespace prefix. */
    public final String prefix;
    /** The namespace URI. */
    public final String uri;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new RdfNamespace instance.
     * @param  prefix   the prefix.
     * @param  uri      the namespace URI.
     */
    RdfNamespace(String prefix, String uri) {
        if ((prefix == null) || (prefix.length() == 0)) {
            throw new IllegalArgumentException("prefix");
        }
        if ((uri == null) || (uri.length() == 0)) {
            throw new IllegalArgumentException("uri");
        }
        this.prefix = prefix;
        this.uri    = uri;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /**
     * Return the string representation of this namespace value in
     * SPARQL syntax: <code>PREFIX prefix: &lt;uri&gt;</code>
     */
    @Override
    public String toString() {
        return "PREFIX " + this.prefix + ": <" + this.uri + '>';
    }
}
