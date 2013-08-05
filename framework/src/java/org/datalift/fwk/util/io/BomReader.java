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

package org.datalift.fwk.util.io;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.datalift.fwk.util.Env;


/**
 * An {@link InputStreamReader} implementation that detects the data
 * encoding from an potential Byte Order Mark (BOM) present at the
 * beginning of the data.
 *
 * @author lbihanic
 */
public class BomReader extends InputStreamReader
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The supported BOMs (Byte Order Marks). */
    public enum BOM
    {
        /** UTF-16 Big Endian */
        UTF16_BE("UTF-16BE", (byte)0xFE, (byte)0xFF),
        /** UTF-16 Little Endian */
        UTF16_LE("UTF-16LE", (byte)0xFF, (byte)0xFE),
        /** UTF-8 */
        UTF8    ("UTF-8",    (byte)0xEF, (byte)0xBB, (byte)0xBF),
        /** UTF-32 Big Endian */
        UTF32_BE("UTF-32BE", (byte)0x00, (byte)0x00, (byte)0xFE, (byte)0xFF),
        /** UTF-32 Little Endian BOM: FF FE 00 00 */
        UTF32_LE("UTF-32LE", (byte)0xFF, (byte)0xFE, (byte)0x00, (byte)0x00);

        /** The name of the character set. */
        public final String name;
        /** The character set. */
        public final Charset charset;
        /** The byte order mark value as an array of bytes. */
        public final byte[] value;

        private BOM(String charset, byte... bom) {
            this.name    = charset;
            this.charset = Charset.forName(charset);
            this.value   = bom;
        }
    }

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates an BomReader that uses the default charset if no BOM
     * is found.
     * @param  in   the {@link InputStream} to read data from.
     */
    public BomReader(InputStream in) {
        this(new BomInputStream(in), Charset.defaultCharset());
    }

    /**
     * Creates an BomReader that uses the specified character set
     * if no BOM is found.
     * @param  in            the {@link InputStream} to read data from.
     * @param  charsetName   the character set to use if no BOM is found.
     *
     * @throws UnsupportedEncodingException If the named charset is not
     *         supported.
     */
    public BomReader(InputStream in, String charsetName)
                                        throws UnsupportedEncodingException {
        this(new BomInputStream(in), charsetName);
    }

    /**
     * Creates an BomReader that uses the specified character set
     * if no BOM is found.
     * @param  in   the {@link InputStream} to read data from.
     * @param  cs   the character set to use if no BOM is found.
     */
    public BomReader(InputStream in, Charset cs) {
        this(new BomInputStream(in), cs);
    }

    /**
     * Internal constructor that relies on the provided
     * {@link BomInputStream} to extract the character encoding
     * information from the underlying input stream.
     * @param  in            the {@link BomInputStream} to read the BOM
     *                       and data from.
     * @param  charsetName   the character set to use if no BOM is found.
     *
     * @throws UnsupportedEncodingException If the named charset is not
     *         supported.
     */
    private BomReader(BomInputStream in, String charsetName)
                                        throws UnsupportedEncodingException {
        super(in, (in.bom != null)? in.bom.name: charsetName);
    }

    /**
     * Internal constructor that relies on the provided
     * {@link BomInputStream} to extract the character encoding
     * information from the underlying input stream.
     * @param  in   the {@link BomInputStream} to read the BOM
     *              and data from.
     */
    private BomReader(BomInputStream in, Charset charset) {
        super(in, (in.bom != null)? in.bom.charset: charset);
    }

    //-------------------------------------------------------------------------
    // BomInputStream nested class
    //-------------------------------------------------------------------------

    /**
     * An {@link InputStream} implementation that extracts the Byte
     * Order Mark (BOM) from the underlying input stream.
     */
    private final static class BomInputStream extends BufferedInputStream
    {
        /** The matched BOM or <code>null</code> if none was found/matched. */
        public final BOM bom;

        /**
         * Creates a new BomInputStream to extract the BOM information
         * from the specified input stream.
         * @param  in   the input stream.
         */
        public BomInputStream(InputStream in) {
            super(in, Env.getFileBufferSize());
            this.bom = this.extractBom(this);
        }

        /**
         * Extracts and consumes the BOM information from the specified
         * input stream.
         * @param  in   the input stream.
         *
         * @return The matched BOM or <code>null</code> if none was
         *         found or matched.
         */
        private BOM extractBom(InputStream in) {
            BOM matchedBom = null;
            try {
                BOM[] boms = BOM.values();
                int max = boms[boms.length-1].value.length;
                byte[] buf = new byte[max];
                // Read BOM and reset stream.
                in.mark(max);
                int l = in.read(buf);
                in.reset();
                // Match the supported BOMs
                for (BOM b : boms) {
                    if (this.matchBom(b, buf, l)) {
                        // Matching BOM found.
                        matchedBom = b;
                        // Consume BOM.
                        in.read(buf, 0, b.value.length);
                        break;
                    }
                }
            }
            catch (IOException e) { /* Ignore... */ }

            return matchedBom;
        }

        /**
         * Return whether the first data read from a stream match the
         * specified BOM.
         * @param  b     the BOM to match.
         * @param  buf   the buffer holding the first data bytes read
         *               from the underlying input stream.
         * @param  l     the length of data in the buffer.
         *
         * @return <code>true</code> if the buffer contents matches the
         *         BOM signature; <code>false</code> otherwise.
         */
        private boolean matchBom(BOM b, byte[] buf, int l) {
            boolean matched = false;
            byte[] bom = b.value;
            if (l >= bom.length) {
                matched = true;
                for (int i=0; i<bom.length; i++) {
                    if (buf[i] != bom[i]) {
                        matched = false;
                        break;
                    }
                }
            }
            // Else: too short to match.

            return matched;
        }
    }
}
