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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;

import org.ccil.cowan.tagsoup.Parser;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.semarglproject.sesame.rdf.rdfa.RDFaFormat;
import org.semarglproject.sesame.rdf.rdfa.SesameRDFaParser;

import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.util.StringUtils;

import static org.datalift.fwk.MediaTypes.*;


/**
 * The RDF representation formats supported by the Datalift platform.
 *
 * @author lbihanic
 */
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
    /** "application/trig" */
    TRIG        ("TriG", RDFFormat.TRIG, "trig",
                 APPLICATION_TRIG_TYPE, APPLICATION_X_TRIG_TYPE),
    /** "application/x-trig" */
    TRIX        ("TriX", RDFFormat.TRIX, "trix", APPLICATION_TRIX_TYPE),
    /** "application/x-trig" */
    NQUADS      ("N-Quads", RDFFormat.NQUADS, "nq", TEXT_NQUADS_TYPE),
    // /** "application/rdf+json" */
    // RDF_JSON    ("RDF/JSON", RDFFormat.RDFJSON, "json",
    //              APPLICATION_RDF_JSON_TYPE, APPLICATION_JSON_TYPE),
    /** RDFa (text/html) */
    RDFA        ("RDFa", RDFaFormat.RDFA, false,
                 new String[] { "html", "xhtml", "htm" },
                 APPLICATION_XHTML_XML_TYPE, TEXT_HTML_TYPE) {
            @Override
            public RDFParser newParser(ValueFactory valueFactory) {
                // Use TagSoup for crappy-HTML tolerant parsing of web pages.
                SesameRDFaParser parser = new SesameRDFaParser(new Parser());
                parser.setVocabExpansionEnabled(true);
                if (valueFactory != null) {
                    parser.setValueFactory(valueFactory);
                }
                return parser;
            }
        };

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /**
     * The supported RDF MIME types, in a format suitable for JAX-RS
     * {@link Request#selectVariant(List) content negotiation}.
     */
    public final static List<Variant> VARIANTS;

    /** A map to resolve MIME type strings into actual RDF type objects. */
    private final static Map<String,RdfFormat> mime2TypeMap =
                                        new LinkedHashMap<String,RdfFormat>();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The format display name. */
    public final String name;
    /** The OpenRDF RDFFormat object for this type. */
    private final RDFFormat format;
    /** Whether outputting to this format is support. */
    private final boolean canOutput;
    /** The file extensions. */
    public final List<String> extensions;
    /** The MIME types that map to the official type. */
    public final List<MediaType> mimeTypes;

    //-------------------------------------------------------------------------
    // Class initializer
    //-------------------------------------------------------------------------

    static {
        List<Variant> rdfVariants = new ArrayList<Variant>();
        for (RdfFormat r : values()) {
            for (MediaType t : r.mimeTypes) {
                mime2TypeMap.put(MediaTypes.toString(t), r);
                if (r.canOutput) {
                    rdfVariants.add(new Variant(t, null, null));
                }
            }
        }
        VARIANTS = Collections.unmodifiableList(rdfVariants);
    }

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new RdfFormat instance.
     * @param  name        the format display name.
     * @param  format      the Sesame RDF format descriptor.
     * @param  extension   the default file extension for the format.
     * @param  mimeTypes   the MIME types for the format, the first
     *                     element being the official/preferred MIME
     *                     type.
     *
     * @see    #RdfFormat(String, RDFFormat, boolean, String[], MediaType...)
     */
    RdfFormat(String name, RDFFormat format,
                           String extension, MediaType... mimeTypes) {
        this(name, format, true, new String[] { extension }, mimeTypes);
    }

    /**
     * Creates a new RdfFormat instance.
     * @param  name         the format display name.
     * @param  format       the Sesame RDF format descriptor.
     * @param  extensions   the file extensions for the format.
     * @param  mimeTypes    the MIME types for the format, the first
     *                      element being the official/preferred MIME
     *                      type.
     *
     * @see    #RdfFormat(String, RDFFormat, boolean, String[], MediaType...)
     */
    RdfFormat(String name, RDFFormat format,
                           String[] extensions, MediaType... mimeTypes) {
        this(name, format, true, extensions, mimeTypes);
    }

    /**
     * Creates a new RdfFormat instance.
     * @param  name         the format display name.
     * @param  format       the Sesame RDF format descriptor.
     * @param  canOutput    whether outputting to this format is
     *                      supported.
     * @param  extensions   the file extensions for the format.
     * @param  mimeTypes    the MIME types for the format, the first
     *                      element being the official/preferred MIME
     *                      type.
     */
    RdfFormat(String name, RDFFormat format, boolean canOutput,
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
        this.canOutput  = canOutput;
        this.extensions = Collections.unmodifiableList(
                                                    Arrays.asList(extensions));
        this.mimeTypes  = Collections.unmodifiableList(
                                                    Arrays.asList(mimeTypes));
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns the format name.
     * @return the format name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the official MIME type for this format.
     * @return the format MIME type.
     */
    public MediaType getMimeType() {
        return this.mimeTypes.get(0);
    }

    /**
     * Returns the known MIME types for this format.
     * @return the format MIME types.
     */
    public Collection<MediaType> getMimeTypes() {
        return this.mimeTypes;
    }

    /**
     * Returns the expected file extension for this format.
     * @return the format default file extension.
     */
    public String getFileExtension() {
        return this.extensions.get(0);
    }

    /**
     * Returns the supported file extensions for this format.
     * @return the supported file extensions.
     */
    public Collection<String> getFileExtensions() {
        return this.extensions;
    }

    /**
     * Returns whether the specified extension is one of the supported
     * file extensions for this format.
     * @param  ext   the file extension, without any leading dot.
     *
     * @return <code>true</code> if the specified file extension is
     *         supported for this format; <code>false</code> otherwise.
     */
    public boolean isExtensionKnown(String ext) {
        if (! StringUtils.isSet(ext)) {
            throw new IllegalArgumentException("ext");
        }
        return this.extensions.contains(ext.toLowerCase());
    }

    /**
     * Returns whether a writer is available to output data in this
     * RDF format. Some formats such as RDFa do not support outputting.
     * @return <code>true</code> if this format can provide a
     *         {@link #newWriter(OutputStream) writer};
     *         <code>false</code> otherwise.
     * @see    #newWriter(OutputStream)
     */
    public boolean canOutput() {
        return this.canOutput;
    }

    /**
     * Creates a new RDF parser object for this type.
     * @return an RDF parser.
     */
    public final RDFParser newParser() {
        return this.newParser(null);
    }

    /**
     * Creates a new RDF parser object for this type.
     * @param  valueFactory   the factory to allocate RDF objects
     *                        (Resources, URIs, Values...).
     *
     * @return an RDF parser.
     */
    public RDFParser newParser(ValueFactory valueFactory) {
        return (valueFactory == null)?
                        Rio.createParser(this.format):
                        Rio.createParser(this.format, valueFactory);
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
     * @param  w   the character stream to write formatted RDF to.
     *
     * @return an RDF formatter.
     */
    public RDFWriter newWriter(Writer w) {
        if (w == null) {
            throw new IllegalArgumentException("w");
        }
        return Rio.createWriter(this.format, w);
    }

    /**
     * Returns the native OpenRDF {@link RDFFormat RDF format} object
     * corresponding to the format.
     * @return the native OpenRDF RDF format object.
     */
    public RDFFormat getNativeFormat() {
        return this.format;
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
        return find(MediaTypes.toString(mimeType));
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
        return get(MediaTypes.toString(mimeType));
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MediaTypes.toString(this.getMimeType());
    }
}
