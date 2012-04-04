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

package org.datalift.fwk.rdf;


import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import org.datalift.fwk.util.StringUtils;

import static org.datalift.fwk.MediaTypes.*;


public enum RdfFormat
{
    //-------------------------------------------------------------------------
    // Values
    //-------------------------------------------------------------------------

    /** "application/rdf+xml" */
    RDF_XML     ("RDF/XML", RDFFormat.RDFXML,
                 new String[] { "rdf", "rdfs", "owl", "xml" },
                 APPLICATION_RDF_XML_TYPE, APPLICATION_XML_TYPE),
    /** "text/turtle" */
    TURTLE      ("Turtle", RDFFormat.TURTLE, "ttl",
                 TEXT_TURTLE_TYPE, APPLICATION_TURTLE_TYPE),
    /** "text/n3" */
    N3          ("N3", RDFFormat.N3, "n3",
                 TEXT_N3_TYPE, TEXT_RDF_N3_TYPE, APPLICATION_N3_TYPE),
    /** "application/n-triples" */
    NTRIPLES    ("N-Triples", RDFFormat.NTRIPLES, "nt",
                 APPLICATION_NTRIPLES_TYPE),
    /** "application/trix" */
    TRIG        ("TriG", RDFFormat.TRIG, "trig", APPLICATION_TRIG_TYPE),
    /** "application/x-trig" */
    TRIX        ("TriX", RDFFormat.TRIX, "trix", APPLICATION_TRIX_TYPE);

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** A map to resolve MIME type strings into actual RDF type objects. */
    private final static Map<String,RdfFormat> mime2TypeMap =
                                                new HashMap<String,RdfFormat>();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The format display name. */
    public final String name;
    /** The OpenRDF RDFFormat object for this type. */
    private final RDFFormat format;
    /** The file extensions. */
    public final List<String> extensions;
    /** The MIME types that map to the official type. */
    public final List<MediaType> mimeTypes;

    //-------------------------------------------------------------------------
    // Class initializer
    //-------------------------------------------------------------------------

    static {
        for (RdfFormat r : values()) {
            for (MediaType t : r.mimeTypes) {
                mime2TypeMap.put(t.toString(), r);
            }
        }
    }

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new RdfType instance.
     * @param  type        the official type.
     * @param  mimeTypes   the MIME types that map to the official type.
     */
    RdfFormat(String name, RDFFormat format,
                           String extension, MediaType... mimeTypes) {
        this(name, format, new String[] { extension }, mimeTypes);
    }

    /**
     * Creates a new RdfType instance.
     * @param  type        the official type.
     * @param  mimeTypes   the MIME types that map to the official type.
     */
    RdfFormat(String name, RDFFormat format,
                           String[] extensions, MediaType... mimeTypes) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name");
        }
        if (format == null) {
            throw new IllegalArgumentException("format");
        }
        if ((extensions == null) || (extensions.length == 0)) {
            throw new IllegalArgumentException("extensions");
        }
        if ((mimeTypes == null) || (mimeTypes.length == 0)) {
            throw new IllegalArgumentException("mimeTypes");
        }
        this.name       = name;
        this.format     = format;
        this.extensions = Collections.unmodifiableList(
                                                    Arrays.asList(extensions));
        this.mimeTypes  = Collections.unmodifiableList(
                                                    Arrays.asList(mimeTypes));
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public String getName() {
        return this.name;
    }

    public MediaType getMimeType() {
        return this.mimeTypes.get(0);
    }

    /**
     * 
     */
    public String getFileExtension() {
        return this.extensions.get(0);
    }

    public boolean isExtensionKnown(String ext) {
        if (! StringUtils.isSet(ext)) {
            throw new IllegalArgumentException("ext");
        }
        return this.extensions.contains(ext.toLowerCase());
    }

    /**
     * Creates a new RDF parser object for this type.
     * @return an RDF parser.
     */
    public RDFParser newParser() {
        return Rio.createParser(this.format);
    }

    /**
     * Creates a new RDF parser object for this type.
     * @param  valueFactory   the factory to allocate RDF objects
     *                        (Resources, URIs, Values...).
     *
     * @return an RDF parser.
     */
    public RDFParser newParser(ValueFactory valueFactory) {
        return Rio.createParser(this.format, valueFactory);
    }

    /**
     * Creates a new RDF formatter object for this type.
     * @param  out   the byte stream to write formatted RDF to.
     *
     * @return an RDF formatter.
     */
    public RDFWriter newWriter(OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException("out");
        }
        return Rio.createWriter(this.format, out);
    }

    /**
     * Creates a new RDF formatter object for this type.
     * @param  out   the character stream to write formatted RDF to.
     *
     * @return an RDF formatter.
     */
    public RDFWriter newWriter(Writer w) {
        if (w == null) {
            throw new IllegalArgumentException("w");
        }
        return Rio.createWriter(this.format, w);
    }

    //-------------------------------------------------------------------------
    // Value resolvers
    //-------------------------------------------------------------------------

    /**
     * Returns the RDF type corresponding to the specified MIME type.
     * @param  mimeType   the MIME type to match.
     *
     * @return the RDF type corresponding to the MIME type or
     *         <code>null</code> if the MIME type is unknown.
     */
    public static RdfFormat find(MediaType mimeType) {
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType");
        }
        return find(mimeType.toString());
    }

    /**
     * Returns the RDF type corresponding to the specified MIME type.
     * @param  mimeType   the MIME type to match.
     *
     * @return the RDF type corresponding to the MIME type or
     *         <code>null</code> if the MIME type is unknown.
     */
    public static RdfFormat find(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            throw new IllegalArgumentException("mimeType");
        }
        return mime2TypeMap.get(mimeType);
    }

    /**
     * Returns the RDF type corresponding to the specified MIME type.
     * @param  mimeType   the MIME type to match.
     *
     * @return the RDF type corresponding to the MIME type.
     * @throws IllegalArgumentException if the MIME type is unknown.
     */
    public static RdfFormat get(MediaType mimeType) {
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType");
        }
        return get(mimeType.toString());
    }

    /**
     * Returns the RDF type corresponding to the specified MIME type.
     * @param  mimeType   the MIME type to match.
     *
     * @return the RDF type corresponding to the MIME type.
     * @throws IllegalArgumentException if the MIME type is unknown.
     */
    public static RdfFormat get(String mimeType) {
        RdfFormat mappedType = find(mimeType);
        if (mappedType == null) {
            throw new IllegalArgumentException(
                            "Unsupported MIME type for RDF data: " + mimeType);
        }
        return mappedType;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getMimeType().toString();
    }
}
