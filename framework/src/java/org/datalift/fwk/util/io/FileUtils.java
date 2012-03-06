package org.datalift.fwk.util.io;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.ws.rs.HttpMethod;

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
            // Get a buffered input stream on file data.
            in = Channels.newInputStream(
                                    new RandomAccessFile(f, "r").getChannel());
            if (log.isDebugEnabled()) {
                in = new ByteCounterInputStream(in, f);
            }
            in = new BufferedInputStream(in, bufferSize);
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
            close(in);
            throw e;
        }
        return in;
    }

    public static void save(URL u, File to) throws IOException {
        save(u, null, null, to);
    }

    public static void save(URL u, String query,
                            Map<String,String> properties, File to)
                                                            throws IOException {
        OutputStream out = null;
        try {
            URLConnection cnx = u.openConnection();
            // Set HTTP headers as request properties.
            if ((properties != null) && (! properties.isEmpty())) {
                for (Map.Entry<String,String> e : properties.entrySet()) {
                    cnx.setRequestProperty(e.getKey(), e.getValue());
                }
            }
            // Follow HTTP redirects, if any.
            if (cnx instanceof HttpURLConnection) {
                ((HttpURLConnection)cnx).setInstanceFollowRedirects(true);
            }
            // Append query string in case of HTTP POST.
            if (query != null) {
                cnx.setDoOutput(true);
                if (cnx instanceof HttpURLConnection) {
                    ((HttpURLConnection)cnx).setRequestMethod(HttpMethod.POST);
                }
                out = cnx.getOutputStream();
                out.write(query.getBytes("UTF-8"));
                out.flush();
                out.close();
                out = null;
            }
            // Force server connection.
            log.debug("Connecting to \"{}\"...", u);
            cnx.connect();
            // Check for HTTP status code.
            int status = 0;
            if (cnx instanceof HttpURLConnection) {
                status = ((HttpURLConnection)cnx).getResponseCode();
                if (((status / 100) * 100) == 200) {
                    status = 0;                 // Success!
                }
            }
            if (status == 0) {
                // No error. => Save data locally.
                log.debug("Downloading source data from \"{}\"...", u);
                save(cnx.getInputStream(), to);
            }
            else {
                // Error. => Gather (first 1024 characters of) error message.
                char[] buf = new char[1024];
                int l = 0;
                Reader r = null;
                try {
                    String[] ct = parseContentType(cnx.getContentType());
                    InputStream in = ((HttpURLConnection)cnx).getErrorStream();
                    r = (ct[1] == null)? new InputStreamReader(in):
                                         new InputStreamReader(in, ct[1]);
                    l = r.read(buf);
                }
                catch (Exception e) { /* Ignore... */ }
                finally {
                    close(r);
                }
                IOException e = new IOException("Failed to connect to \"" +
                                                u + "\": status=" + status);
                log.fatal("{}, message=\"{}\"", e,
                                        e.getMessage(), new String(buf, 0, l));
                throw e;
            }
        }
        finally {
            close(out);
        }
    }

    public static void save(InputStream from, File to) throws IOException {
        if (from == null) {
            throw new IllegalArgumentException("from");
        }
        if (to == null) {
            throw new IllegalArgumentException("to");
        }
        long t0 = System.currentTimeMillis();
        long byteCount = 0L;

        final int chunkSize = Env.getFileBufferSize();
        boolean copyFailed = true;
        InputStream in = null;
        OutputStream out = null;
        try {
            in  = new BufferedInputStream(from, chunkSize);
            out = new BufferedOutputStream(new FileOutputStream(to), chunkSize);

            byte[] buf = new byte[chunkSize];
            int l;
            while ((l = in.read(buf)) != -1) {
                byteCount += l;
                out.write(buf, 0, l);
            }
            out.flush();
            out.close();
            out = null;
            copyFailed = false;
        }
        finally {
            close(in);
            close(out);
            if (copyFailed) {
                to.delete();
            }
            else {
                long delay = System.currentTimeMillis() - t0;
                log.debug("Saved {} MBs of data to {} in {} seconds",
                          Double.valueOf((byteCount / 1000) / 1000.0), to,
                          Double.valueOf(delay / 1000.0));
            }
        }
    }
    
    public final static void copy(File from, File to, boolean compress)
                                                            throws IOException {
        if ((from == null) || (! from.canRead())) {
            throw new IllegalArgumentException("from");
        }
        if (to == null) {
            throw new IllegalArgumentException("to");
        }
        long t0 = System.currentTimeMillis();
        long byteCount = 0L;

        final int chunkSize = Env.getFileBufferSize();
        boolean copyFailed = true;
        try {
            if (compress) {
                InputStream  in  = null;
                OutputStream out = null;
                try {
                    in  = new BufferedInputStream(new FileInputStream(from),
                                                  chunkSize);
                    out = new GZIPOutputStream(new FileOutputStream(to),
                                               chunkSize);
                    byte[] buf = new byte[chunkSize];
                    int l;
                    while ((l = in.read(buf)) != -1) {
                        byteCount += l;
                        out.write(buf, 0, l);
                    }
                    out.flush();
                    out.close();
                    out = null;
                    copyFailed = false;
                }
                finally {
                    close(in);
                    close(out);
                }
            }
            else {
                FileChannel in  = null;
                FileChannel out = null;
                try {
                    in  = new FileInputStream(from).getChannel();
                    out = new FileOutputStream(to).getChannel();
        
                    long start = 0L;
                    long end   = in.size();
                    while (end != 0L) {
                        long l = Math.min(end, chunkSize);
                        l = in.transferTo(start, l, out);
                        if (l == 0L) {
                            // Should at least copy one byte!
                            throw new IOException(
                                    "Copy stalled after " + start + " bytes");
                        }
                        start += l;
                        end   -= l;
                    }
                    out.force(true);  // Sync data on disk.
                    out.close();
                    out = null;
                    copyFailed = false;
                    byteCount = start;
                }
                finally {
                    close(in);
                    close(out);
                }
            }
        }
        finally {
            if (copyFailed) {
                to.delete();
            }
            else {
                long delay = System.currentTimeMillis() - t0;
                log.debug("Copied {} MBs of data from {} to {} in {} seconds",
                          Double.valueOf((byteCount / 1000) / 1000.0),
                          from, to, Double.valueOf(delay / 1000.0));
            }
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private static String[] parseContentType(String contentType) {
        String[] elts = new String[2];

        final String CHARSET_TAG = "charset=";
        if ((contentType != null) && (contentType.length() != 0)) {
            String[] s = contentType.split("\\s;\\s");
            elts[0] = s[0];
            if ((s.length > 1) && (s[1].startsWith(CHARSET_TAG))) {
                elts[1] = s[1].substring(CHARSET_TAG.length());
            }
        }
        return elts;
    }

    private final static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (Exception e) { /* Ignore... */ }
        }
    }

    /**
     * Fetches unsigned 16-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private final static int get16(byte b[], int offset) {
        return (b[offset] & 0xff) | ((b[offset+1] & 0xff) << 8);
    }

    /**
     * Fetches unsigned 32-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    private final static long get32(byte b[], int offset) {
        return get16(b, offset) | ((long)get16(b, offset+2) << 16);
    }

    //-------------------------------------------------------------------------
    // ByteCounterInputStream nested class
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
                    log.debug("Read {} MBs from {} in {} seconds ({} MB/s)",
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
    // ZipWrapperInputStream nested class
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
