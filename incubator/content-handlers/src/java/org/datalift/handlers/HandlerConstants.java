package org.datalift.handlers;


import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Variant;

import static org.datalift.fwk.MediaTypes.*;


/**
 * Constants commons to the content type handlers.
 *
 * @author lbihanic
 */
public class HandlerConstants
{
    /**
     * The MIME types for CONSTRUCT and DESCRIBE query responses handled
     * by the default SPARQL endpoint.
     */
    public final static List<Variant> CONSTRUCT_DEFAULT_RESPONSE_TYPES =
            Arrays.asList(
                    new Variant(APPLICATION_RDF_XML_TYPE, null, null),
                    new Variant(APPLICATION_SPARQL_RESULT_JSON_TYPE, null, null),
                    new Variant(APPLICATION_JSON_TYPE, null, null),
                    new Variant(TEXT_TURTLE_TYPE, null, null),
                    new Variant(APPLICATION_TURTLE_TYPE, null, null),
                    new Variant(TEXT_N3_TYPE, null, null),
                    new Variant(TEXT_RDF_N3_TYPE, null, null),
                    new Variant(APPLICATION_N3_TYPE, null, null),
                    new Variant(APPLICATION_NTRIPLES_TYPE, null, null),
                    new Variant(APPLICATION_TRIG_TYPE, null, null),
                    new Variant(APPLICATION_TRIX_TYPE, null, null),
                    new Variant(TEXT_HTML_TYPE, null, null),
                    new Variant(APPLICATION_XHTML_XML_TYPE, null, null),
                    new Variant(APPLICATION_XML_TYPE, null, null),
                    new Variant(TEXT_XML_TYPE, null, null));

    /** Default constructor, private on purpose. */
    private HandlerConstants() {
        throw new UnsupportedOperationException();
    }
}
