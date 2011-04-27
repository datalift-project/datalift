package org.datalift.fwk.rdf;

import java.util.Map;

import org.openrdf.query.Dataset;


/**
 * A wrapper for exceptions thrown by RDF result handlers.
 *
 * @author lbihanic
 */
public class RdfException extends Exception
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final String query;
    private Map<String,Object> bindings;
    private Dataset dataset;
    private String baseUri;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructs a new exception with the specified cause but no
     * detail message.  The detail message of this exception will
     * be the detail message of the cause.
     * @param  query   the query.
     * @param  cause   the cause.
     */
    public RdfException(String query, Throwable cause) {
        super(cause);
        this.query = query;
    }

    //-------------------------------------------------------------------------
    // RdfException contract definition
    //-------------------------------------------------------------------------

    public String getQuery() {
        return query;
    }

    public void setBindings(Map<String,Object> bindings) {
        this.bindings = bindings;
    }

    public Map<String,Object> getBindings() {
        return this.bindings;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getBaseUri() {
        return this.baseUri;
    }

    public final RdfException populate(Map<String,Object> bindings,
                                       Dataset dataset, String baseUri) {
        this.setBindings(bindings);
        this.setDataset(dataset);
        this.setBaseUri(baseUri);
        return this;
    }
}
