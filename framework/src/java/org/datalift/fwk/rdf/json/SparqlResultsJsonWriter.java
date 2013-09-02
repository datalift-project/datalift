package org.datalift.fwk.rdf.json;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;


/**
 * A {@link TupleQueryResultWriter} implementation that serializes
 * SPARQL query results in
 * <a href="http://www.w3.org/TR/sparql11-results-json/">SPARQL 1.1
 * Query Results JSON Format</a>.
 *
 * @author lbihanic
 */
public class SparqlResultsJsonWriter extends AbstractJsonWriter
                                     implements TupleQueryResultWriter
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Create a new SPARQL Query Results JSON serializer.
     * @param  out   the byte stream to write JSON text to.
     */
    public SparqlResultsJsonWriter(OutputStream out) {
        this(out, null);
    }

    /**
     * Create a new SPARQL Query Results JSON serializer.
     * @param  out            the byte stream to write JSON text to.
     * @param  jsonCallback   the JSONP callback function to wrap the
     *                        generated JSON object or <code>null</code>
     *                        to produce standard JSON.
     */
    public SparqlResultsJsonWriter(OutputStream out, String jsonCallback) {
        super(out, jsonCallback);
    }

    /**
     * Create a new SPARQL Query Results JSON serializer.
     * @param  out   the character stream to write JSON text to.
     */
    public SparqlResultsJsonWriter(Writer out) {
        this(out, null);
    }

    /**
     * Create a new SPARQL Query Results JSON serializer.
     * @param  out            the character stream to write JSON text to.
     * @param  jsonCallback   the JSONP callback function to wrap the
     *                        generated JSON object or <code>null</code>
     *                        to produce standard JSON.
     */
    public SparqlResultsJsonWriter(Writer out, String jsonCallback) {
        super(out, jsonCallback);
    }

    //-------------------------------------------------------------------------
    // AbstractJsonWriter contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation relies on
     * {@link #writeJsonValue(Value, ResourceType)} to output
     * RDF/JSON-compliant value descriptions.</p>
     */
    @Override
    protected void writeValue(Value value, ResourceType type)
                                                            throws IOException {
        this.writeJsonValue(value, type);
    }

    //-------------------------------------------------------------------------
    // TupleQueryResultWriter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final TupleQueryResultFormat getTupleQueryResultFormat() {
        return TupleQueryResultFormat.JSON;
    }

    /** {@inheritDoc} */
    @Override
    public void startQueryResult(List<String> bindingNames)
                                    throws TupleQueryResultHandlerException {
        try {
            this.start(bindingNames);
            this.openBraces();
            // Write header
            this.writeKey("head");
            this.openBraces();
            this.writeKeyValue("vars", bindingNames);
            this.closeBraces();
            this.writeComma();
            // Write results
            this.writeKey("results");
            this.openBraces();
            this.writeKey("bindings");
            this.openArray();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleSolution(BindingSet bindingSet)
                                    throws TupleQueryResultHandlerException {
        try {
            this.startSolution();       // start of new solution

            for (Iterator<Binding> i=bindingSet.iterator(); i.hasNext(); ) {
                Binding b = i.next();
                this.writeKeyValue(b.getName(), b.getValue(),
                                                ResourceType.Unknown);
                if (i.hasNext()) {
                    this.writeComma();
                }
            }
            this.endSolution();         // end solution
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endQueryResult() throws TupleQueryResultHandlerException {
        try {
            this.closeArray();          // bindings array
            this.closeBraces();         // results braces
            this.closeBraces();         // root braces
            this.end();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }
}
