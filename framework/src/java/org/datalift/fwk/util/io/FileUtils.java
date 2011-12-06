package org.datalift.fwk.util.io;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.itadaki.bzip2.BZip2InputStream;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.Env;


public final class FileUtils
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /* BZip2 magic numbers. */
    private static final byte[] BZ2_HEADERS = { 0x42, 0x5a, 0x68 };

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor, private on purpose. */
    private FileUtils() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // FileUtils contract definition
    //-------------------------------------------------------------------------

    public static InputStream getInputStream(File f) throws IOException {
        return getInputStream(f, Env .getFileBufferSize());
    }

    public static InputStream getInputStream(File f, int bufferSize)
                                                            throws IOException {
        if ((f == null) || (! f.isFile()) || (! f.canRead())) {
            throw new IllegalArgumentException(
                                new FileNotFoundException(String.valueOf(f)));
        }
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f), bufferSize);
            // Read file magic number, if any.
            byte[] buf = new byte[4];
            in.mark(32);
            in.read(buf);
            // Reset current position to start of file.
            in.reset();
            // Try to match known compressed file formats.
            if (get32(buf, 0) == ZipInputStream.LOCSIG) {
                // Zip-compressed data.
                try {
                    in = new ZipWrapperInputStream(new ZipFile(f));
                    log.trace("File \"{}\" identified as ZIP-compressed", f);
                }
                catch (Exception e) { /* Ignore... */ }
            }
            else if (get16(buf, 0) == GZIPInputStream.GZIP_MAGIC) {
                // GZip-compressed data.
                in = new GZIPInputStream(in, bufferSize);
                log.trace("File \"{}\" identified as GZIP-compressed", f);
            }
            else if ((buf[0] == BZ2_HEADERS[0]) &&
                     (buf[1] == BZ2_HEADERS[1]) && (buf[2] == BZ2_HEADERS[2])) {
                // BZip2-compressed data.
                in = new BZip2InputStream(in, false);
                log.trace("File \"{}\" identified as BZip2-compressed", f);
            }
            // Else: regular file!
        }
        catch (IOException e) {
            if (in != null) {
                try { in.close(); } catch (Exception e1) { /* Ignore... */ }
            }
            throw e;
        }
        return in;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Fetches unsigned 16-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private static final int get16(byte b[], int offset) {
        return (b[offset] & 0xff) | ((b[offset+1] & 0xff) << 8);
    }

    /**
     * Fetches unsigned 32-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private static final long get32(byte b[], int offset) {
        return get16(b, offset) | ((long)get16(b, offset+2) << 16);
    }

    private final static class ZipWrapperInputStream extends FilterInputStream
    {
        private final ZipFile zipFile;

        public ZipWrapperInputStream(ZipFile f) throws IOException {
            super(f.getInputStream(f.entries().nextElement()));
            this.zipFile = f;
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.zipFile.close();
        }
    }
}
