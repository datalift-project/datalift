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

package org.datalift.fwk;


import javax.ws.rs.core.MediaType;


/**
 * An extension to JAX-RS {@link MediaType standard media types} with
 * RDF-related media types. Instances are immutable.
 *
 * @author lbihanic
 */
public class MediaTypes extends MediaType
{
    private final static String APPL_TYPE = APPLICATION_XML_TYPE.getType();
    private final static String TEXT_TYPE = TEXT_PLAIN_TYPE.getType();

    /** "application/rdf+xml" */
    public final static MediaType APPLICATION_RDF_XML_TYPE =
                                    new MediaType(APPL_TYPE, "rdf+xml");
    /** "application/rdf+xml" */
    public final static String APPLICATION_RDF_XML = "application/rdf+xml";

    /** "text/turtle" */
    public final static MediaType TEXT_TURTLE_TYPE =
                                    new MediaType(TEXT_TYPE, "turtle");
    /** "text/turtle" */
    public final static String TEXT_TURTLE = "text/turtle";
    /** "application/x-turtle" */
    public final static MediaType APPLICATION_TURTLE_TYPE =
                                    new MediaType(APPL_TYPE, "x-turtle");
    /** "application/x-turtle" */
    public final static String APPLICATION_TURTLE = "application/x-turtle";

    /** "text/n3" */
    public final static MediaType TEXT_N3_TYPE = new MediaType(TEXT_TYPE, "n3");
    /** "text/n3" */
    public final static String TEXT_N3 = "text/n3";
    /** "text/rdf+n3" */
    public final static MediaType TEXT_RDF_N3_TYPE =
                                    new MediaType(TEXT_TYPE, "rdf+n3");
    /** "text/rdf+n3" */
    public final static String TEXT_RDF_N3 = "text/rdf+n3";
    /** "application/n3" */
    public final static MediaType APPLICATION_N3_TYPE =
                                    new MediaType(APPL_TYPE, "n3");
    /** "application/n3" */
    public final static String APPLICATION_N3 = "application/n3";

    /** "application/n-triples" */
    public final static MediaType APPLICATION_NTRIPLES_TYPE =
                                    new MediaType(APPL_TYPE, "n-triples");
    /** "application/n-triples" */
    public final static String APPLICATION_NTRIPLES = "application/n-triples";

    /** "application/sparql-query" */
    public final static MediaType APPLICATION_SPARQL_QUERY_TYPE =
                                    new MediaType(APPL_TYPE, "sparql-query");
    /** "application/sparql-query" */
    public final static String APPLICATION_SPARQL_QUERY =
                                                    "application/sparql-query";
    /** "application/sparql-results+xml" */
    public final static MediaType APPLICATION_SPARQL_RESULT_XML_TYPE =
                            new MediaType(APPL_TYPE, "sparql-results+xml");
    /** "application/sparql-results+xml" */
    public final static String APPLICATION_SPARQL_RESULT_XML =
                                            "application/sparql-results+xml";
    /** "application/sparql-results+json" */
    public final static MediaType APPLICATION_SPARQL_RESULT_JSON_TYPE =
                            new MediaType(APPL_TYPE, "sparql-results+json");
    /** "application/sparql-results+json" */
    public final static String APPLICATION_SPARQL_RESULT_JSON =
                                            "application/sparql-results+json";    
    /** "application/trix" */
    public final static MediaType APPLICATION_TRIX_TYPE = new MediaType(APPL_TYPE, "trix");
    /** "application/trix" */
    public final static String APPLICATION_TRIX = "application/trix";

    /** "application/x-trig" */
    public final static MediaType APPLICATION_TRIG_TYPE = new MediaType(APPL_TYPE, "x-trig");
    /** "application/x-trig" */
    public final static String APPLICATION_TRIG = "application/x-trig";
}
