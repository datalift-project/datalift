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
import java.text.ChoiceFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import info.aduna.io.IndentingWriter;
import info.aduna.text.StringUtil;

import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.util.web.Charsets.UTF_8;


/**
 * A helper class to write custom JSON serializers for RDF data.
 *
 * @author hdevos
 */
public abstract class AbstractJsonWriter
{
    //-------------------------------------------------------------------------
    // ResourceType enum
    //-------------------------------------------------------------------------

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

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Default variables to outputting RDF statements. */
    protected final static String[] CONSTRUCT_VARS =
                                        { "subject", "predicate", "object" };

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    protected final IndentingWriter writer;
    protected final String jsonCallback;
    protected List<String> fields;

    private final Map<String,String> nsPrefixes = new HashMap<String,String>();
    private boolean firstTupleWritten;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Create a new RDF JSON serializer.
     * @param  out   the byte stream to write JSON text to.
     */
    public AbstractJsonWriter(OutputStream out) {
        this(out, null);
    }

    /**
     * Create a new RDF JSON serializer.
     * @param  out            the byte stream to write JSON text to.
     * @param  jsonCallback   the JSONP callback function to wrap the
     *                        generated JSON object or <code>null</code>
     *                        to produce standard JSON.
     */
    public AbstractJsonWriter(OutputStream out, String jsonCallback) {
        this(new OutputStreamWriter(out, UTF_8), jsonCallback);
    }

    /**
     * Create a new RDF JSON serializer.
     * @param  out   the character stream to write JSON text to.
     */
    public AbstractJsonWriter(Writer out) {
        this(out, null);
    }

    /**
     * Create a new RDF JSON serializer.
     * @param  out            the character stream to write JSON text to.
     * @param  jsonCallback   the JSONP callback function to wrap the
     *                        generated JSON object or <code>null</code>
     *                        to produce standard JSON.
     */
    public AbstractJsonWriter(Writer out, String jsonCallback) {
        if (out == null) {
            throw new IllegalArgumentException("out");
        }
        if (! (out instanceof BufferedWriter)) {
            out = new BufferedWriter(out, 1024);
        }
        this.writer = new CompactWriter(out);
        this.jsonCallback = trimToNull(jsonCallback);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Starts a new JSON document.
     * @param  fields   the field names for the forthcoming JSON objects.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected void start(List<String> fields) throws IOException {
        this.fields = Collections.unmodifiableList(fields);
        this.firstTupleWritten = false;
        if (this.jsonCallback != null) {
            this.writer.write(this.jsonCallback);
            this.writer.write('(');
        }
        this.nsPrefixes.clear();
    }

    /**
     * Terminates a JSON document.
     * @throws IOException if any error occurred output the JSON text.
     */
    protected void end() throws IOException {
        if (this.jsonCallback != null) {
            this.writer.write(')');
        }
        this.writer.flush();
    }

    /**
     * Defines a namespace prefix mapping.
     * @param  prefix   the namespace prefix.
     * @param  nsUri    the namespace URI.
     */
    protected final void setPrefix(String prefix, String nsUri) {
        this.nsPrefixes.put(nsUri, prefix);
    }

    /**
     * Returns the prefix associated to the specified namespace URI.
     * @return the prefix associated to the namespace URI or
     *         <code>null</code> if no prefix mapping has been defined
     *         fir the namespace.
     */
    protected final String getPrefix(String nsUri) {
        return this.nsPrefixes.get(nsUri);
    }

    /**
     * Starts a new solution, wrapped in a JSON object.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected void startSolution() throws IOException {
        if (firstTupleWritten) {
            this.writeComma();
        }
        else {
            this.firstTupleWritten = true;
        }
        this.openBraces();              // start of new solution
    }

    /**
     * Terminates the current, closing the associated JSON object.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected void endSolution() throws IOException {
        this.closeBraces();             // end solution
    }

    /**
     * Appends an object field the value of which is an RDF value (URI,
     * blank node, literal...).
     * @param  key     the JSON object field name.
     * @param  value   the field RDF value.
     *
     * @throws IOException if any error occurred output the JSON text.
     * @see    #writeKeyValue(String, Value, ResourceType)
     */
    protected void writeKeyValue(String key, Value value) throws IOException {
        this.writeKeyValue(key, value, null);
    }

    /**
     * Appends an object field.
     * @param  key     the JSON object field name.
     * @param  value   the field value as a string.
     *
     * @throws IOException if any error occurred output the JSON text.
     * @see    #writeKey(String)
     * @see    #writeString(String)
     */
    protected void writeKeyValue(String key, String value) throws IOException {
        this.writeKey(key);
        this.writeString(value);
    }

    /**
     * Appends an object field the value of which is an RDF value (URI,
     * blank node, literal...).
     * @param  key     the JSON object field name.
     * @param  value   the field RDF value.
     * @param  type    the type of RDF resource referenced by the URI
     *                 value, <code>null</code> if not applicable.
     *
     * @throws IOException if any error occurred output the JSON text.
     * @see    #writeKey(String)
     * @see    #writeValue(Value, ResourceType)
     */
    protected void writeKeyValue(String key, Value value, ResourceType type)
                                                            throws IOException {
        this.writeKey(key);
        this.writeValue(value, type);
    }

    /**
     * Appends an RDF field value (URI, blank node, literal...).
     * <p>
     * This implementation provides a default implementation to
     * delegate calls to this method to:
     * {@link #writeJsonValue(Value, ResourceType)} that writes the
     * RDF value in compliance with the
     * <a href="https://github.com/iand/rdf-json">RDF/JSON format.</p>
     * @param  value   the RDF value.
     * @param  type    the type of RDF resource referenced by the URI
     *                 value, <code>null</code> if not applicable.
     *
     * @throws IOException if any error occurred output the JSON text.
     * @see    #writeJsonValue(Value, ResourceType)
     */
    abstract protected void writeValue(Value value, ResourceType type)
                                                            throws IOException;

    /**
     * Appends an RDF field value (URI, blank node, literal...) in
     * compliance with the
     * <a href="https://github.com/iand/rdf-json">RDF/JSON format</a>.
     * @param  value   the RDF value.
     * @param  type    the type of RDF resource referenced by the URI
     *                 value: <strong>ignored</strong>.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected final void writeJsonValue(Value value, ResourceType type)
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

    protected final void writeKeyValue(String key, Iterable<String> array)
                                                            throws IOException {
        this.writeKey(key);
        this.writeArray(array);
    }

    protected final void writeKey(String key) throws IOException {
        this.writeString(key);
        this.writer.write(": ");
    }

    /**
     * Append a JSON string value, escaping special characters
     * (/, \, \b, \f, \n, \r, \t).
     * @param  value   the string value.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
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

    /**
     * Outputs a JSON array.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected void writeArray(Iterable<String> array) throws IOException {
        this.openArray();

        Iterator<String> iter = array.iterator();
        while (iter.hasNext()) {
            this.writeString(iter.next());
            if (iter.hasNext()) {
                this.writeComma();
            }
        }
        this.closeArray();
    }

    /**
     * Starts a new JSON array.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected final void openArray() throws IOException {
        this.writer.write("[");
        this.writer.writeEOL();
        this.writer.increaseIndentation();
    }

    /**
     * Closes the current JSON array.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected final void closeArray() throws IOException {
        this.writer.writeEOL();
        this.writer.decreaseIndentation();
        this.writer.write("]");
    }

    /**
     * Starts a new JSON object.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected final void openBraces() throws IOException {
        this.writer.write("{");
        this.writer.writeEOL();
        this.writer.increaseIndentation();
    }

    /**
     * Terminates the current JSON object.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected final void closeBraces() throws IOException {
        this.writer.writeEOL();
        this.writer.decreaseIndentation();
        this.writer.write("}");
    }

    /**
     * Appends a comma separator.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    protected final void writeComma() throws IOException {
        this.writer.write(", ");
    }

    //-------------------------------------------------------------------------
    // CompactWriter nested class
    //-------------------------------------------------------------------------

    /**
     * An {@link IndentingWriter} implementation without any indentation
     * or line feeds.
     */
    private final static class CompactWriter extends IndentingWriter
    {
        public CompactWriter(Writer out) {
            super(out);
        }

        @Override public void writeEOL() throws IOException {
            this.out.append(' ');
        }

        @Override public void increaseIndentation() { /* NOP */ }
        @Override public void decreaseIndentation() { /* NOP */ }

        @Override public void setIndentationLevel(int l) {
            super.setIndentationLevel(0);
        }
    }
}
