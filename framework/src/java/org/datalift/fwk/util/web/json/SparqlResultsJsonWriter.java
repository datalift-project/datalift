package org.datalift.fwk.util.web.json;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;


/**
 * A {@link TupleQueryResultWriter} implementation that serializes
 * SPARQL query results into a JSON syntax compliant with the W3C
 * proposal for
 * <a href="http://www.w3.org/TR/rdf-sparql-json-res/">Serializing
 * SPARQL Query Results in JSON</a>.
 *
 * @author lbihanic
 */
public class SparqlResultsJsonWriter extends AbstractJsonWriter
                                     implements TupleQueryResultWriter
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public SparqlResultsJsonWriter(OutputStream out) {
        this(out, null, null, null, null);
    }

    public SparqlResultsJsonWriter(OutputStream out, String urlPattern,
                                String defaultGraphUri, Dataset dataset,
                                                        String jsonCallback) {
        super(out, urlPattern, defaultGraphUri, dataset, jsonCallback);
    }

    public SparqlResultsJsonWriter(Writer out) {
        this(out, null, null, null, null);
    }

    public SparqlResultsJsonWriter(Writer out, String urlPattern,
                                String defaultGraphUri, Dataset dataset,
                                                        String jsonCallback) {
        super(out, urlPattern, defaultGraphUri, dataset, jsonCallback);
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
    public void startQueryResult(List<String> columnHeaders)
                                    throws TupleQueryResultHandlerException {
        try {
            this.start(columnHeaders);
            this.openBraces();
            // Write header
            this.writeKey("head");
            this.openBraces();
            this.writeKeyValue("vars", columnHeaders);
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

    @Override
    protected void writeValue(Value value, ResourceType type)
                                                            throws IOException {
        this.writeJsonValue(value, type);
    }
}
