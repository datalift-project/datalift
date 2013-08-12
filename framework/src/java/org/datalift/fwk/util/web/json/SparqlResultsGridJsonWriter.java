package org.datalift.fwk.util.web.json;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultWriterBase;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;

import static org.datalift.fwk.util.web.json.JsonWriter.ResourceType.Unknown;


public class SparqlResultsGridJsonWriter extends QueryResultWriterBase
                                         implements TupleQueryResultWriter
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final JsonWriter w;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public SparqlResultsGridJsonWriter(OutputStream out) {
        this(out, null, null, null);
    }

    public SparqlResultsGridJsonWriter(OutputStream out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
        this.w = new JsonWriter(out, urlPattern, defaultGraphUri, jsonCallback);
    }

    public SparqlResultsGridJsonWriter(Writer out) {
        this(out, null, null, null);
    }

    public SparqlResultsGridJsonWriter(Writer out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
        this.w = new JsonWriter(out, urlPattern, defaultGraphUri, jsonCallback);
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
    public void startQueryResult(List<String> bindingNames)
                                    throws TupleQueryResultHandlerException {
        try {
            this.w.start(bindingNames);
            this.w.openBraces();
            // Write header
            this.w.writeKeyValue("head", bindingNames);
            this.w.writeComma();
            // Write results
            this.w.writeKey("rows");
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

            for (Iterator<String> i=this.w.columnHeaders.iterator();
                                                            i.hasNext(); ) {
                String key = i.next();
                this.w.writeKeyValue(key, bindingSet.getValue(key), Unknown);
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
            this.w.closeArray();
            this.w.closeBraces();
            this.w.end();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }
}
