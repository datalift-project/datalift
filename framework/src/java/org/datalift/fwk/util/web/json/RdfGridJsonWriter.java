package org.datalift.fwk.util.web.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFWriterBase;

import static org.openrdf.rio.RDFFormat.*;

import static org.datalift.fwk.util.web.json.JsonWriter.CONSTRUCT_VARS;
import static org.datalift.fwk.util.web.json.JsonWriter.ResourceType.*;


public class RdfGridJsonWriter extends RDFWriterBase
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final JsonWriter w;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public RdfGridJsonWriter(OutputStream out) {
        this(out, null, null, null);
    }

    public RdfGridJsonWriter(OutputStream out, String urlPattern,
                             String defaultGraphUri, String jsonCallback) {
        this.w = new JsonWriter(out, urlPattern, defaultGraphUri, jsonCallback);
    }

    public RdfGridJsonWriter(Writer out) {
        this(out, null, null, null);
    }

    public RdfGridJsonWriter(Writer out, String urlPattern,
                             String defaultGraphUri, String jsonCallback) {
        this.w = new JsonWriter(out, urlPattern, defaultGraphUri, jsonCallback);
    }

    //-------------------------------------------------------------------------
    // RDFHandler contract support
    //-------------------------------------------------------------------------

    @Override
    public RDFFormat getRDFFormat() {
        return new RDFFormat("JSON", "application/json",
                             Charset.forName("UTF-8"), "json",
                             NO_NAMESPACES, NO_CONTEXTS);
    }

    /** {@inheritDoc} */
    @Override
    public void startRDF() throws RDFHandlerException {
        try {
            this.w.start(Arrays.asList(CONSTRUCT_VARS));
            this.w.openBraces();
            // Write header
            this.w.writeKeyValue("head", this.w.columnHeaders);
            this.w.writeComma();
            // Write results
            this.w.writeKey("rows");
            this.w.openArray();
        }
        catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleNamespace(String prefix, String uri)
                                                    throws RDFHandlerException {
        this.w.setPrefix(prefix, uri);
    }

    /** {@inheritDoc} */
    @Override
    public void handleStatement(Statement stmt) throws RDFHandlerException {
        try {
            this.w.startSolution();     // start of new solution
            this.w.writeKeyValue(CONSTRUCT_VARS[0], stmt.getSubject(), Object);
            this.w.writeComma();
            this.w.writeKeyValue(CONSTRUCT_VARS[1], stmt.getPredicate(),
                                                                    Predicate);
            this.w.writeComma();
            this.w.writeKeyValue(CONSTRUCT_VARS[2], stmt.getObject(), Unknown);
            this.w.endSolution();       // end solution
        }
        catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleComment(String comment) throws RDFHandlerException {
        // Ignore comments.
    }

    /** {@inheritDoc} */
    @Override
    public void endRDF() throws RDFHandlerException {
        try {
            this.w.closeArray();
            this.w.closeBraces();
            this.w.end();
        }
        catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }
}
