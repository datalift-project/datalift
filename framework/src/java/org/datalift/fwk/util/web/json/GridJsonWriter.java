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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import org.datalift.fwk.project.Row;


public class GridJsonWriter extends AbstractJsonWriter
                            implements TupleQueryResultWriter, RDFHandler
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public GridJsonWriter(OutputStream out) {
        super(out);
    }

    public GridJsonWriter(Writer out) {
        super(out);
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
            this.writeKeyValue("head", columnHeaders);
            this.writeComma();
            // Write results
            this.writeKey("rows");
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

            Iterator<Binding> bindingIter = bindingSet.iterator();
            Iterator<String> headerIter = columnHeaders.iterator();

            while (bindingIter.hasNext() && headerIter.hasNext()) {
                Binding binding = bindingIter.next();
                this.writeKeyValue(headerIter.next(), binding.getValue());
                if (bindingIter.hasNext() && headerIter.hasNext()) {
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
            this.closeArray();
            this.closeBraces();
            this.end();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    //-------------------------------------------------------------------------
    // RDFHandler contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startRDF() throws RDFHandlerException {
        try {
            this.startQueryResult(Arrays.asList(CONSTRUCT_VARS));
        }
        catch (Exception e) {
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
            this.writeKeyValue(CONSTRUCT_VARS[0], stmt.getSubject());
            this.writeComma();
            this.writeKeyValue(CONSTRUCT_VARS[1], stmt.getPredicate());
            this.writeComma();
            this.writeKeyValue(CONSTRUCT_VARS[2], stmt.getObject());
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
            this.endQueryResult();
        }
        catch (Exception e) {
            throw new RDFHandlerException(e);
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void handleRow(Row<?> row) throws IOException {
        this.startSolution();           // start of new solution

        for (Iterator<String> i=this.columnHeaders.iterator(); i.hasNext(); ) {
            String key = i.next();
            this.writeKeyValue(key, row.getString(key));
            if (i.hasNext()) {
                this.writeComma();
            }
        }
        this.endSolution();             // end solution
    }
}