package org.datalift.fwk.util.io;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
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
            FileChannel channel = new RandomAccessFile(f, "r").getChannel();
            in = new BufferedInputStream(
                            new ByteCounterInputStream(
                                    Channels.newInputStream(channel), f),
                             bufferSize);
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

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private final static class ByteCounterInputStream extends FilterInputStream
    {
        private final File file;
        private final long logThreshold = 10 * 1024 * 1024L;    // 10 MB

        private long readBytes = 0L;
        private long markPos   = -1L;
        private long lastLog   = 0L;
        private long startTime = 0L;

        public ByteCounterInputStream(InputStream in, File f)
                                                            throws IOException {
            super(in);
            this.file = f;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            this.updateCounter(1L);
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            this.updateCounter(n);
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long l = super.skip(n);
            this.updateCounter(l);
            return l;
        }

        @Override
        public synchronized void mark(int readlimit) {
            super.mark(readlimit);
            this.markPos = this.readBytes;
        }

        @Override
        public synchronized void reset() throws IOException {
            super.reset();
            if (this.markPos >= 0L) {
                this.readBytes = this.markPos;
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (log.isDebugEnabled()) {
                long delay = System.currentTimeMillis() - this.startTime;
                if (delay > 0L) {
                    log.trace("Read {} MBs from {} in {} seconds ({} MB/s)",
                          Double.valueOf((this.readBytes / 1000L) / 1000.0),
                          this.file,
                          Double.valueOf(delay / 1000.0),
                          Double.valueOf((this.readBytes / delay) / 1000.0));
                }
            }
        }

        private final void updateCounter(long readCount) {
            if (this.startTime == 0L) {
                this.startTime = System.currentTimeMillis();
            }
            this.readBytes += readCount;
            if ((log.isTraceEnabled()) &&
                ((this.readBytes - this.lastLog) > this.logThreshold)) {
                long delay = System.currentTimeMillis() - this.startTime;
                log.trace("Read {} MBs from {} in {} seconds",
                          Double.valueOf((this.readBytes / 1000L) / 1000.0),
                          this.file,
                          Double.valueOf(delay / 1000.0));
                this.lastLog = (this.readBytes / this.logThreshold)
                                                        * this.logThreshold;
            }
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

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
