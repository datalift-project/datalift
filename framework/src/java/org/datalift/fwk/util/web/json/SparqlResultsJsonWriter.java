package org.datalift.fwk.util.web.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultWriterBase;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;

import static org.datalift.fwk.util.web.json.JsonWriter.ResourceType.*;


public class SparqlResultsJsonWriter extends QueryResultWriterBase
                                     implements TupleQueryResultWriter
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final JsonWriter w;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public SparqlResultsJsonWriter(OutputStream out) {
        this(out, null, null, null);
    }

    public SparqlResultsJsonWriter(OutputStream out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
        this.w = new JsonResultsWriter(out, urlPattern,
                                            defaultGraphUri, jsonCallback);
    }

    public SparqlResultsJsonWriter(Writer out) {
        this(out, null, null, null);
    }

    public SparqlResultsJsonWriter(Writer out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
        this.w = new JsonResultsWriter(out, urlPattern,
                                            defaultGraphUri, jsonCallback);
    }

    //-------------------------------------------------------------------------
    // TupleQueryResultWriter contract support
    //-------------------------------------------------------------------------

    @Override
    public final TupleQueryResultFormat getTupleQueryResultFormat() {
        return TupleQueryResultFormat.JSON;
    }

    //-------------------------------------------------------------------------
    // QueryResultWriter contract support
    //-------------------------------------------------------------------------

    @Override
    public TupleQueryResultFormat getQueryResultFormat() {
        return getTupleQueryResultFormat();
    }

    @Override
    public void handleNamespace(String prefix, String uri)
                                        throws QueryResultHandlerException {
        this.w.setPrefix(prefix, uri);
    }

    @Override
    public void startDocument() throws QueryResultHandlerException {
        // NOP. See #startQueryResult(List<String>)
    }

    /** {@inheritDoc} */
    @Override
    public void handleStylesheet(String stylesheetUrl)
                                        throws QueryResultHandlerException {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void startHeader() throws QueryResultHandlerException {
        // NOP. See #startQueryResult(List<String>)
    }

    /** {@inheritDoc} */
    @Override
    public void endHeader() throws QueryResultHandlerException {
        // NOP. See #startQueryResult(List<String>)
    }

    /** {@inheritDoc} */
    @Override
    public void handleBoolean(boolean value)
                                        throws QueryResultHandlerException {
        throw new QueryResultHandlerException(
                                        new UnsupportedOperationException());
    }

    /** {@inheritDoc} */
    @Override
    public void handleLinks(List<String> linkUrls)
                    throws QueryResultHandlerException {
        // TODO: Add support for JSON links.
    }

    /** {@inheritDoc} */
    @Override
    public void startQueryResult(List<String> columnHeaders)
                                    throws TupleQueryResultHandlerException {
        try {
            this.w.start(columnHeaders);
            this.w.openBraces();
            // Write header
            this.w.writeKey("head");
            this.w.openBraces();
            this.w.writeKeyValue("vars", columnHeaders);
            this.w.closeBraces();
            this.w.writeComma();
            // Write results
            this.w.writeKey("results");
            this.w.openBraces();
            this.w.writeKey("bindings");
            this.w.openArray();
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
            this.w.startSolution();     // start of new solution

            for (Iterator<Binding> i=bindingSet.iterator(); i.hasNext(); ) {
                Binding b = i.next();
                this.w.writeKeyValue(b.getName(), b.getValue(), Unknown);
                if (i.hasNext()) {
                    this.w.writeComma();
                }
            }
            this.w.endSolution();       // end solution
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endQueryResult() throws TupleQueryResultHandlerException {
        try {
            this.w.closeArray();          // bindings array
            this.w.closeBraces();         // results braces
            this.w.closeBraces();         // root braces
            this.w.end();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    private final static class JsonResultsWriter extends JsonWriter
    {
        public JsonResultsWriter(OutputStream out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
            super(out, urlPattern, defaultGraphUri, jsonCallback);
        }

        public JsonResultsWriter(Writer out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
            super(out, urlPattern, defaultGraphUri, jsonCallback);
        }

        @Override
        protected void writeValue(Value value, ResourceType type)
                                                            throws IOException {
            this.writeJsonValue(value, type);
        }
    }
}
