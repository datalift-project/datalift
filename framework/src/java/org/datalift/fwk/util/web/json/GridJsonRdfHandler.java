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

package org.datalift.fwk.util.web.json;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Arrays;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;


/**
 * An implementation of {@link RDFHandler} that serializes RDF
 * statements into a compact JSON syntax, suitable for directly
 * filling HTML tables with minimum client-side processing.
 *
 * @author hdevos
 */
public class GridJsonRdfHandler extends AbstractGridJsonWriter
                                implements RDFHandler
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out   the byte stream to write JSON text to.
     */
    public GridJsonRdfHandler(OutputStream out) {
        this(out, null, null);
    }

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out            the byte stream to write JSON text to.
     * @param  urlPattern     a message format to replace RDF resource
     *                        URIs with HTML links
     *                        (<code>&lt;a href=.../&gt;</code>).
     * @param  jsonCallback   the JSONP callback function to wrap the
     *                        generated JSON object or <code>null</code>
     *                        to produce standard JSON.
     */
    public GridJsonRdfHandler(OutputStream out, MessageFormat urlPattern,
                                            String jsonCallback) {
        super(out, urlPattern, jsonCallback);
    }

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out   the character stream to write JSON text to.
     */
    public GridJsonRdfHandler(Writer out) {
        this(out, null, null);
    }

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out            the character stream to write JSON text to.
     * @param  urlPattern     a message format to replace RDF resource
     *                        URIs with HTML links
     *                        (<code>&lt;a href=.../&gt;</code>).
     * @param  jsonCallback   the JSONP callback function to wrap the
     *                        generated JSON object or <code>null</code>
     *                        to produce standard JSON.
     */
    public GridJsonRdfHandler(Writer out, MessageFormat urlPattern,
                                      String jsonCallback) {
        super(out, urlPattern, jsonCallback);
    }

    //-------------------------------------------------------------------------
    // RDFHandler contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startRDF() throws RDFHandlerException {
        try {
            this.startDocument(Arrays.asList(CONSTRUCT_VARS));
        }
        catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleNamespace(String prefix, String uri)
                                                    throws RDFHandlerException {
        this.setPrefix(prefix, uri);
    }

    /** {@inheritDoc} */
    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        try {
            this.write(st);
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
            this.endDocument();
        }
        catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }
}