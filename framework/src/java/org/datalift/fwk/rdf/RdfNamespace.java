package org.datalift.fwk.rdf;


public enum RdfNamespace
{
    //-------------------------------------------------------------------------
    // Values
    //-------------------------------------------------------------------------

    DataLift    ("datalift",    "http://www.datalift.org/core#"),
    DC_Elements ("dc",          "http://purl.org/dc/elements/1.1/"),
    DC_Terms    ("dcterms",     "http://purl.org/dc/terms/"),
    FOAF        ("foaf",        "http://xmlns.com/foaf/0.1/"),
    RDF         ("rdf",         "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    RDFS        ("rdfs",        "http://www.w3.org/2000/01/rdf-schema#"),
    OWL         ("owl",         "http://www.w3.org/2002/07/owl#"),
    VOID        ("void",        "http://rdfs.org/ns/void#"),
    DOAP        ("doap",        "http://usefulinc.com/ns/doap#");

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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "PREFIX " + this.prefix + ": <" + this.uri + '>';
    }
}
