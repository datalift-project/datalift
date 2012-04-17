package org.datalift.fwk.util.web.json;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;

import info.aduna.io.IndentingWriter;
import info.aduna.text.StringUtil;


public abstract class AbstractJsonWriter
{
    public enum ResourceType {
        Object          (0),
        Predicate       (1),
        Graph           (2),
        Unknown         (3);

        public final int value;
        private ResourceType(int value) {
            this.value = value;
        }
    }

    protected final static String[] CONSTRUCT_VARS =
                                        { "subject", "predicate", "object" };

    private final IndentingWriter writer;
    private final MessageFormat urlPattern;
    private final String defaultGraphUri;

    private boolean firstTupleWritten;
    protected List<String> columnHeaders;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public AbstractJsonWriter(OutputStream out) {
        this(out, null, null);
    }

    public AbstractJsonWriter(OutputStream out,
                              String urlPattern, String defaultGraphUri) {
        this(new OutputStreamWriter(out, Charset.forName("UTF-8")),
             urlPattern, defaultGraphUri);
    }

    public AbstractJsonWriter(Writer out) {
        this(out, null, null);
    }

    public AbstractJsonWriter(Writer out, String urlPattern,
                                          String defaultGraphUri) {
        if (out == null) {
            throw new IllegalArgumentException("out");
        }
        if (! (out instanceof BufferedWriter)) {
            out = new BufferedWriter(out, 1024);
        }
        this.writer = new IndentingWriter(out);
        this.urlPattern = (urlPattern != null)? new MessageFormat(urlPattern):
                                                null;
        this.defaultGraphUri = defaultGraphUri;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    protected void start(List<String> headers) throws IOException {
        this.columnHeaders = headers;
        this.firstTupleWritten = false;
    }

    protected void end() throws IOException {
        this.writer.flush();
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
        this.writer.flush();
    }

    protected void writeKeyValue(String key, Value value) throws IOException {
        this.writeKeyValue(key, value, null);
    }

    protected void writeKeyValue(String key, Value value, ResourceType type)
                                                            throws IOException {
        if (value instanceof Resource) {
            this.writeKeyValue(key, (Resource)value, type);
        }
        else {
            this.writeKeyValue(key, (Literal)value);
        }
    }

    protected void writeKeyValue(String key, Resource value)
                                                            throws IOException {
        this.writeKeyValue(key, value, ResourceType.Unknown);
    }

    protected void writeKeyValue(String key, Resource value, ResourceType type)
                                                            throws IOException {
        if (value instanceof BNode) {
            this.writeKeyValue(key, "_:" + value.stringValue(), type);
        }
        else {
            this.writeKeyValue(key, value.stringValue(), type);
        }
    }

    protected void writeKeyValue(String key, Literal value) throws IOException {
        this.writeKeyValue(key, (value != null)? value.stringValue(): "");
    }

    protected void writeKeyValue(String key, String value) throws IOException {
        this.writeKeyValue(key, value, null);
    }

    protected void writeKeyValue(String key, String value, ResourceType type)
                                                            throws IOException {
        if ((type != null) && (this.urlPattern != null) &&
            ((type != ResourceType.Unknown) ||
             (value.startsWith("http://") || (value.startsWith("https://"))))) {
            Object[] args = new Object[] { URLEncoder.encode(value, "UTF-8"),
                                           Integer.valueOf(type.value),
                                           this.defaultGraphUri };
            value = "<a href=\"" + this.urlPattern.format(args) + "\">"
                                                            + value + "</a>";            
        }
        this.writeKey(key);
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
        this.writer.writeEOL();
    }
}
