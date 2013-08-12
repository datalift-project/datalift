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


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import info.aduna.io.IndentingWriter;
import info.aduna.text.StringUtil;

import static org.datalift.fwk.util.web.Charsets.UTF_8;


/**
 * A helper class to write custom JSON serializers for RDF data.
 *
 * @author hdevos
 */
public class JsonWriter
{
    /** Types of RDF objects recognized for JSON serialization. */
    public enum ResourceType {
        /** RDF subject or object, literals excluded. */
        Object          (0),
        /** Predicate. */
        Predicate       (1),
        /** Named graph. */
        Graph           (2),
        /** Object type to be determined by analyzing the query results. */
        Unknown         (3);

        /**
         * The resource type identifier as an integer value, suitable
         * for testing in a {@link ChoiceFormat}.
         */
        public final int value;

        /**
         * Default constructor.
         * @param  value   the resource type identifier.
         */
        private ResourceType(int value) {
            this.value = value;
        }
    }

    public final static String[] CONSTRUCT_VARS =
                                        { "subject", "predicate", "object" };
    private final static Pattern NATIVE_TYPES_PATTERN = Pattern.compile(
                "boolean|.*[iI]nt.*|.*[lL]ong|.*[sS]hort|decimal|double|float");

    private final IndentingWriter writer;
    private final MessageFormat urlPattern;
    private final String defaultGraphUri;
    private final String jsonCallback;
    private final Map<String,String> nsPrefixes = new HashMap<String,String>();

    private boolean firstTupleWritten;
    protected List<String> columnHeaders;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public JsonWriter(OutputStream out) {
        this(out, null, null, null);
    }

    public JsonWriter(OutputStream out, String urlPattern,
                      String defaultGraphUri, String jsonCallback) {
        this(new OutputStreamWriter(out, UTF_8),
             urlPattern, defaultGraphUri, jsonCallback);
    }

    public JsonWriter(Writer out) {
        this(out, null, null, null);
    }

    public JsonWriter(Writer out, String urlPattern,
                      String defaultGraphUri, String jsonCallback) {
        if (out == null) {
            throw new IllegalArgumentException("out");
        }
        if (! (out instanceof BufferedWriter)) {
            out = new BufferedWriter(out, 1024);
        }
        // this.writer = new IndentingWriter(out);
        this.writer = new CompactWriter(out);
        this.urlPattern = (urlPattern != null)? new MessageFormat(urlPattern):
                                                null;
        this.defaultGraphUri = defaultGraphUri;
        if (jsonCallback != null) {
            jsonCallback = jsonCallback.trim();
            if (jsonCallback.length() == 0) {
                jsonCallback = null;
            }
        }
        this.jsonCallback = jsonCallback;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    protected void start(List<String> headers) throws IOException {
        this.columnHeaders = headers;
        this.firstTupleWritten = false;
        if (this.jsonCallback != null) {
            this.writer.write(this.jsonCallback);
            this.writer.write('(');
        }
        this.nsPrefixes.clear();
    }

    protected void end() throws IOException {
        if (this.jsonCallback != null) {
            this.writer.write(')');
        }
        this.writer.flush();
    }

    protected void setPrefix(String prefix, String nsUri) {
        this.nsPrefixes.put(nsUri, prefix);
    }

    protected void startSolution() throws IOException {
        if (firstTupleWritten) {
            this.writeComma();
        }
        else {
            this.firstTupleWritten = true;
        }
        this.openBraces();              // start of new solution
    }

    protected void endSolution() throws IOException {
        this.closeBraces();             // end solution
        // this.writer.flush();
    }

    protected void writeKeyValue(String key, Value value) throws IOException {
        this.writeKeyValue(key, value, null);
    }

    protected void writeKeyValue(String key, String value) throws IOException {
        this.writeKey(key);
        this.writeString(value);
    }

    protected void writeKeyValue(String key, Value value, ResourceType type)
                                                            throws IOException {
        this.writeKey(key);
        this.writeValue(value, type);
    }

    protected void writeValue(Value value, ResourceType type)
                                                            throws IOException {
        this.writeValueSimple(value, type);
    }

    protected void writeJsonValue(Value value, ResourceType type)
                                                            throws IOException {
        this.openBraces();

        if (value instanceof URI) {
                this.writeKeyValue("type", "uri");
                this.writeComma();
                this.writeKeyValue("value", ((URI)value).toString());
        }
        else if (value instanceof BNode) {
                this.writeKeyValue("type", "bnode");
                this.writeComma();
                this.writeKeyValue("value", ((BNode)value).getID());
        }
        else if (value instanceof Literal) {
                Literal l = (Literal)value;
                if (l.getDatatype() != null) {
                        this.writeKeyValue("type", "typed-literal");
                        this.writeComma();
                        this.writeKeyValue("datatype",
                                           l.getDatatype().toString());
                }
                else {
                        this.writeKeyValue("type", "literal");
                        if (l.getLanguage() != null) {
                                this.writeComma();
                                this.writeKeyValue("xml:lang", l.getLanguage());
                        }
                }
                this.writeComma();
                this.writeKeyValue("value", l.getLabel());
        }
        else {
            throw new IOException(
                            "Unknown Value object type: " + value.getClass());
        }
        this.closeBraces();
    }

    protected void writeValueSimple(Value value, ResourceType type)
                                                            throws IOException {
        if (value instanceof BNode) {
            this.writeValue("_:" + value.stringValue(), type);
        }
        else if (value instanceof Literal) {
            Literal l = (Literal)value;
            if ((l.getDatatype() != null) &&
                (NATIVE_TYPES_PATTERN.matcher(l.getDatatype().getLocalName())
                                     .matches())) {
                this.writer.write(l.getLabel());
            }
            else {
                this.writeValue(l.toString(), type);
            }
        }
        else if (value instanceof URI) {
            this.writeValue((URI)value, type);
        }
        else {
            this.writeValue((value != null)? value.stringValue(): "", type);
        }
    }

    protected void writeValue(URI u, ResourceType type) throws IOException {
        String label = null;
        String prefix = this.nsPrefixes.get(u.getNamespace());
        if (prefix != null) {
            label = prefix + ':' + u.getLocalName();
        }
        this.writeValue(u.stringValue(), label, type);
    }

    protected void writeValue(String value, ResourceType type)
                                                            throws IOException {
        this.writeValue(value, null, type);
    }

    protected void writeValue(String value, String label, ResourceType type)
                                                            throws IOException {
        if ((type != null) && (this.urlPattern != null) &&
            ((type != ResourceType.Unknown) ||
             (value.startsWith("http://") || (value.startsWith("https://"))))) {
            Object[] args = new Object[] { URLEncoder.encode(value, UTF_8.name()),
                                           Integer.valueOf(type.value),
                                           this.defaultGraphUri };
            value = "<a href=\"" + this.urlPattern.format(args) + "\">"
                                 + ((label != null)? label: value) + "</a>";
        }
        this.writeString(value);
    }

    protected void writeKeyValue(String key, Iterable<String> array)
                                                            throws IOException {
        this.writeKey(key);
        this.writeArray(array);
    }

    protected void writeKey(String key) throws IOException {
        this.writeString(key);
        this.writer.write(": ");
    }

    protected void writeString(String value) throws IOException {
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

    protected void writeArray(Iterable<String> array) throws IOException {
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

    protected void openArray() throws IOException {
        this.writer.write("[");
        this.writer.writeEOL();
        this.writer.increaseIndentation();
    }

    protected void closeArray() throws IOException {
        this.writer.writeEOL();
        this.writer.decreaseIndentation();
        this.writer.write("]");
    }

    protected void openBraces() throws IOException {
        this.writer.write("{");
        this.writer.writeEOL();
        this.writer.increaseIndentation();
    }

    protected void closeBraces() throws IOException {
        this.writer.writeEOL();
        this.writer.decreaseIndentation();
        this.writer.write("}");
    }

    protected void writeComma() throws IOException {
        this.writer.write(", ");
    }


    private final static class CompactWriter extends IndentingWriter
    {
        public CompactWriter(Writer out) {
            super(out);
        }

        @Override public void writeEOL()                    { /* NOP */ }
        @Override public void increaseIndentation()         { /* NOP */ }
        @Override public void decreaseIndentation()         { /* NOP */ }
        @Override public void setIndentationLevel(int l)    { /* NOP */ }
    }
}
