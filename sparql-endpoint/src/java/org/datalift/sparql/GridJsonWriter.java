/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.sparql;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import info.aduna.io.IndentingWriter;
import info.aduna.text.StringUtil;

import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;


public class GridJsonWriter implements TupleQueryResultWriter
{
    private IndentingWriter writer;

    private boolean firstTupleWritten;
    private List<String> columnHeaders;

    public GridJsonWriter(OutputStream out) {
        Writer w = new OutputStreamWriter(out, Charset.forName("UTF-8"));
        w = new BufferedWriter(w, 1024);
        writer = new IndentingWriter(w);
    }

    public final TupleQueryResultFormat getTupleQueryResultFormat() {
        return TupleQueryResultFormat.JSON;
    }

    public void startQueryResult(List<String> columnHeaders)
                                    throws TupleQueryResultHandlerException {
    	this.columnHeaders = columnHeaders;
        try {
            this.openBraces();
            // Write header
            this.writeKeyValue("head", columnHeaders);
            this.writeComma();
            // Write results
            this.writeKey("rows");
            this.openArray();

            this.firstTupleWritten = false;
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    public void endQueryResult() throws TupleQueryResultHandlerException {
        try {
            this.closeArray();
            this.closeBraces();
            this.writer.flush();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    public void handleSolution(BindingSet bindingSet)
                                    throws TupleQueryResultHandlerException {
        try {
            if (firstTupleWritten) {
                this.writeComma();
            }
            else {
                this.firstTupleWritten = true;
            }
            this.openBraces(); // start of new solution

            Iterator<Binding> bindingIter = bindingSet.iterator();
            Iterator<String> headerIter = columnHeaders.iterator();

            while (bindingIter.hasNext() && headerIter.hasNext()) {
                Binding binding = bindingIter.next();
                String head = headerIter.next();
                String v = binding.getValue().stringValue();
                if (v.startsWith("http://")) {
                    this.writeKeyValue(head,
                                       "<a href=\"" + v + "\">" + v + "</a>");
                }
                else {
                    this.writeKeyValue(head, v);
                }
                if (bindingIter.hasNext()) {
                    this.writeComma();
                }
            }
            this.closeBraces(); // end solution
            this.writer.flush();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    private void writeKeyValue(String key, String value) throws IOException {
        this.writeKey(key);
        this.writeString(value);
    }

    private void writeKeyValue(String key, Iterable<String> array)
                                                            throws IOException {
        this.writeKey(key);
        this.writeArray(array);
    }

    private void writeKey(String key) throws IOException {
        this.writeString(key);
        this.writer.write(": ");
    }

    private void writeString(String value) throws IOException {
        // Escape special characters
        value = StringUtil.gsub("\\", "\\\\", value);
        value = StringUtil.gsub("\"", "\\\"", value);
        value = StringUtil.gsub("/", "\\/", value);
        value = StringUtil.gsub("\b", "\\b", value);
        value = StringUtil.gsub("\f", "\\f", value);
        value = StringUtil.gsub("\n", "\\n", value);
        value = StringUtil.gsub("\r", "\\r", value);
        value = StringUtil.gsub("\t", "\\t", value);

        this.writer.write("\"");
        this.writer.write(value);
        this.writer.write("\"");
    }

    private void writeArray(Iterable<String> array) throws IOException {
        this.writer.write("[ ");

        Iterator<String> iter = array.iterator();
        while (iter.hasNext()) {
            this.writeString(iter.next());
            if (iter.hasNext()) {
                this.writer.write(", ");
            }
        }
        this.writer.write(" ]");
    }

    private void openArray() throws IOException {
        this.writer.write("[");
        this.writer.writeEOL();
        this.writer.increaseIndentation();
    }

    private void closeArray() throws IOException {
        this.writer.writeEOL();
        this.writer.decreaseIndentation();
        this.writer.write("]");
    }

    private void openBraces() throws IOException {
        this.writer.write("{");
        this.writer.writeEOL();
        this.writer.increaseIndentation();
    }

    private void closeBraces() throws IOException {
        this.writer.writeEOL();
        this.writer.decreaseIndentation();
        this.writer.write("}");
    }

    private void writeComma() throws IOException {
        this.writer.write(", ");
        this.writer.writeEOL();
    }
}