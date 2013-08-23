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
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

import static org.datalift.fwk.util.web.Charsets.UTF_8;
import static org.datalift.fwk.util.web.json.AbstractJsonWriter.ResourceType.*;


/**
 * An implementation of both {@link AbstractJsonWriter} to serialize
 * SPARQL query results and RDF statements into a compact JSON syntax,
 * suitable for directly filling HTML tables with minimum client-side
 * processing.
 *
 * @author hdevos
 */
public abstract class AbstractGridJsonWriter extends AbstractJsonWriter
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Regex to identify native XML schema data types. */
    private final static Pattern NATIVE_TYPES_PATTERN = Pattern.compile(
                            "boolean|float|decimal|double|int|" +
                            ".*[bB]yte|.*[iI]nteger|.*[lL]ong|.*[sS]hort");

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /**
     * The pattern to format HTML links when substituting URLs to
     * RDF resource URIs.
     */
    private final MessageFormat urlPattern;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out   the byte stream to write JSON text to.
     */
    public AbstractGridJsonWriter(OutputStream out) {
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
    public AbstractGridJsonWriter(OutputStream out, MessageFormat urlPattern,
                                                    String jsonCallback) {
        super(out, jsonCallback);
        this.urlPattern = urlPattern;
    }

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out   the character stream to write JSON text to.
     */
    public AbstractGridJsonWriter(Writer out) {
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
    public AbstractGridJsonWriter(Writer out, MessageFormat urlPattern,
                                              String jsonCallback) {
        super(out, jsonCallback);
        this.urlPattern = urlPattern;
    }

    //-------------------------------------------------------------------------
    // AbstractJsonWriter contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation relies on
     * {@link #writeValueSimple(Value, ResourceType)} to output RDF
     * values in a format suitable for directly filling HTML tables
     * with minimum client-side processing.</p>
     */
    @Override
    protected final void writeValue(Value value, ResourceType type)
                                                            throws IOException {
        this.writeValueSimple(value, type);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Writes the beginning of the JSON document.
     * @param  fields   the fields that will be present in the
     *                  subsequent RDF data rows.
     *
     * @throws IOException if any error occurred outputting the
     *         JSON text.
     */
    protected final void startDocument(List<String> fields)
                                                        throws IOException {
        this.start(fields);
        this.openBraces();
        // Write header
        this.writeKeyValue("head", fields);
        this.writeComma();
        // Write results
        this.writeKey("rows");
        this.openArray();
    }

    /**
     * Appends the specified binding set to the JSON document.
     * @param  bindingSet   the binding set.
     *
     * @throws IOException if any error occurred outputting the
     *         JSON text.
     */
    protected final void write(BindingSet bindingSet) throws IOException {
        this.startSolution();           // start of new solution
        for (Iterator<String> i=this.fields.iterator(); i.hasNext(); ) {
            String key = i.next();
            this.writeKeyValue(key, bindingSet.getValue(key), Unknown);
            if (i.hasNext()) {
                this.writeComma();
            }
        }
        this.endSolution();             // end solution
    }

    /**
     * Appends the specified RDF statement to the JSON document.
     * @param  stmt   the RDF statement.
     *
     * @throws IOException if any error occurred outputting the
     *         JSON text.
     */
    protected final void write(Statement stmt) throws IOException {
        this.startSolution();           // start of new solution
        this.writeKeyValue(CONSTRUCT_VARS[0], stmt.getSubject(), Object);
        this.writeComma();
        this.writeKeyValue(CONSTRUCT_VARS[1], stmt.getPredicate(), Predicate);
        this.writeComma();
        this.writeKeyValue(CONSTRUCT_VARS[2], stmt.getObject(), Unknown);
        this.endSolution();             // end solution
    }

    /**
     * Terminates and closes the JSON document.
     * @throws IOException if any error occurred outputting the
     *         JSON text.
     */
    protected final void endDocument() throws IOException {
        this.closeArray();              // rows array
        this.closeBraces();             // root braces
        this.end();
    }

/*
    public void handleRow(Row<?> row) throws IOException {
        this.startSolution();           // start of new solution

        for (Iterator<String> i=this.fields.iterator(); i.hasNext(); ) {
            String key = i.next();
            this.writeKeyValue(key, new LiteralImpl(row.getString(key)), Unknown);
            if (i.hasNext()) {
                this.writeComma();
            }
        }
        this.endSolution();             // end solution
    }
*/

    /**
     * Appends an RDF field value (URI, blank node, literal...) as a
     * simple string, substituting URLs to URIs if a URL format has
     * been provided.
     * @param  value   the RDF value.
     * @param  type    the type of RDF resource referenced by the URI
     *                 value. If set, the type value is passed to the
     *                 URL format.
     *
     * @throws IOException if any error occurred output the JSON text.
     */
    private final void writeValueSimple(Value value, ResourceType type)
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

    private void writeValue(URI u, ResourceType type) throws IOException {
        String label = null;
        String prefix = this.getPrefix(u.getNamespace());
        if (prefix != null) {
            label = prefix + ':' + u.getLocalName();
        }
        this.writeValue(u.stringValue(), label, type);
    }

    private void writeValue(String value, ResourceType type)
                                                            throws IOException {
        this.writeValue(value, null, type);
    }

    private void writeValue(String value, String label, ResourceType type)
                                                            throws IOException {
        if ((type != null) && (this.urlPattern != null) &&
            ((type != ResourceType.Unknown) ||
             (value.startsWith("http://") || (value.startsWith("https://"))))) {
            Object[] args = new Object[] { URLEncoder.encode(value, UTF_8.name()),
                                           Integer.valueOf(type.value) };
            value = "<a href=\"" + this.urlPattern.format(args) + "\">"
                                 + ((label != null)? label: value) + "</a>";
        }
        this.writeString(value);
    }
}