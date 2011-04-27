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
    public final static String APPLICATION_RDF_XML =
                                    APPLICATION_RDF_XML_TYPE.toString();

    /** "text/turtle" */
    public final static MediaType TEXT_TURTLE_TYPE =
                                    new MediaType(TEXT_TYPE, "turtle");
    /** "text/turtle" */
    public final static String TEXT_TURTLE = TEXT_TURTLE_TYPE.toString();
    /** "application/x-turtle" */
    public final static MediaType APPLICATION_TURTLE_TYPE =
                                    new MediaType(APPL_TYPE, "x-turtle");
    /** "application/x-turtle" */
    public final static String cTURTLE =
                                    APPLICATION_TURTLE_TYPE.toString();

    /** "text/n3" */
    public final static MediaType TEXT_N3_TYPE = new MediaType(TEXT_TYPE, "n3");
    /** "text/n3" */
    public final static String TEXT_N3 = TEXT_N3_TYPE.toString();
    /** "text/rdf+n3" */
    public final static MediaType TEXT_RDF_N3_TYPE =
                                    new MediaType(TEXT_TYPE, "rdf+n3");
    /** "text/rdf+n3" */
    public final static String TEXT_RDF_N3 = TEXT_RDF_N3_TYPE.toString();
    /** "application/n3" */
    public final static MediaType APPLICATION_N3_TYPE =
                                    new MediaType(APPL_TYPE, "n3");
    /** "application/n3" */
    public final static String APPLICATION_N3 = APPLICATION_N3_TYPE.toString();

    /** "application/sparql-query" */
    public final static MediaType APPLICATION_SPARQL_QUERY_TYPE =
                                    new MediaType(APPL_TYPE, "sparql-query");
    /** "application/sparql-query" */
    public final static String APPLICATION_SPARQL_QUERY =
                                    APPLICATION_SPARQL_QUERY_TYPE.toString();
    /** "application/sparql-results+xml" */
    public final static MediaType APPLICATION_SPARQL_RESULT_TYPE =
                            new MediaType(APPL_TYPE, "sparql-results+xml");
    /** "application/sparql-results+xml" */
    public final static String APPLICATION_SPARQL_RESULT =
                                    APPLICATION_SPARQL_RESULT_TYPE.toString();
}
