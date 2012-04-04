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
    /** dct: &lt;http://purl.org/dc/terms/&gt; */
    DCTerms     ("dct",         "http://purl.org/dc/terms/"),
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
    PRV         ("prv",         "http://purl.org/net/provenance/ns#"),
    /** cnt: &lt;http://www.w3.org/2011/content#&gt; */
    CNT         ("cnt",         "http://www.w3.org/2011/content#");

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
