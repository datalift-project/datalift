package org.datalift.fwk.util.web.json;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


public class JsonRdfHandler extends AbstractJsonWriter implements RDFHandler
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public JsonRdfHandler(OutputStream out) {
        super(out);
    }

    public JsonRdfHandler(Writer out) {
        super(out);
    }

    //-------------------------------------------------------------------------
    // RDFHandler contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startRDF() throws RDFHandlerException {
        try {
            this.start(Arrays.asList(CONSTRUCT_VARS));
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
            throw new RDFHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleNamespace(String prefix, String uri)
                                                    throws RDFHandlerException {
        // Ignore namespace prefixes.
    }

    /** {@inheritDoc} */
    @Override
    public void handleStatement(Statement stmt) throws RDFHandlerException {
        try {
            this.startSolution();       // start of new solution
            this.writeKeyValue(CONSTRUCT_VARS[0], stmt.getSubject(), null);
            this.writeComma();
            this.writeKeyValue(CONSTRUCT_VARS[1], stmt.getPredicate(), null);
            this.writeComma();
            this.writeKeyValue(CONSTRUCT_VARS[2], stmt.getObject(), null);
            this.endSolution();         // end solution
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
            this.closeArray(); // bindings array
            this.closeBraces(); // results braces
            this.closeBraces(); // root braces
            this.end();
        }
        catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }
}
