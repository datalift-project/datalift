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
import java.util.zip.ZipInputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.itadaki.bzip2.BZip2InputStream;

import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.web.HttpDateFormat;

import static org.datalift.fwk.util.StringUtils.isSet;
import static org.datalift.fwk.util.web.Charsets.UTF_8;


/**
 * A set of utility methods to help handling file-based data:
 * <ul>
 *  <li>Reading data from compressed files with automatic detection
 *   of the compression algorithm (ZIP, GZIPor BZip2),</li>
 *  <li>Downloading data from a URL to save them into a local file,</li>
 *  <li>Copying files,</li>
 *  <li>Etc.</li>
 * </ul>
 *
 * @author lbihanic
 */
public final class FileUtils
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /* BZip2 magic numbers. */
    private final static byte[] BZ2_HEADERS = { 0x42, 0x5a, 0x68 };
    /* MIME type for contents of unknown type. */
    private final static String CONTENT_UNKNOWN = "content/unknown";

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

    /**
     * Returns a buffered input stream reading data for the specified
     * file.
     * @param  f   the file to read data from.
     *
     * @return a buffered input stream.
     * @throws IOException   if the file does not exist, is not
     *                        read-accessible or if any I/O error
     *                        occurred while extracting a compressed
     *                        file encoding type (ZIP, GZIP or BZip2).
     */
    public static InputStream getInputStream(File f) throws IOException {
        return getInputStream(f, Env.getFileBufferSize());
    }

    /**
     * Returned a buffered input stream reading data for the specified
     * file with the specified buffer size.
     * @param  f            the file to read data from.
     * @param  bufferSize   the buffer size.
     *
     * @return a buffered input stream.
     * @throws IOException   if the file does not exist, is not
     *                        read-accessible or if any I/O error
     *                        occurred while extracting a compressed
     *                        file encoding type (ZIP, GZIP or BZip2).
     */
    public static InputStream getInputStream(File f, int bufferSize)
                                                            throws IOException {
        if (f == null) {
            throw new IllegalArgumentException("f");
        }
        if (! (f.isFile() && f.canRead())) {
            throw new IllegalArgumentException(
                                new FileNotFoundException(String.valueOf(f)));
        }
        InputStream in = null;
        try {
            in = Channels.newInputStream(
                                    new RandomAccessFile(f, "r").getChannel());
            if (log.isDebugEnabled()) {
                in = new ByteCounterInputStream(in, f);
            }
            log.trace("Creating input stream for \"{}\"", f);
            in = getInputStream(in, bufferSize);
        }
        catch (IOException e) {
            closeQuietly(in);
            throw e;
        }
        return in;
    }

    /**
     * Returned a buffered input stream reading data for the specified
     * file with the specified buffer size.
     * @param  in           the raw input stream to wrap.
     * @param  bufferSize   the buffer size.
     *
     * @return a buffered input stream.
     * @throws IOException   if the file does not exist, is not
     *                        read-accessible or if any I/O error
     *                        occurred while extracting a compressed
     *                        file encoding type (ZIP, GZIP or BZip2).
     */
    public static InputStream getInputStream(InputStream in, int bufferSize)
                                                            throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("in");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize");
        }
        // Get a buffered input stream on file data.
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
                in = new ZipInputStream(in);
                ((ZipInputStream)in).getNextEntry();
                log.trace("File content identified as ZIP-compressed");
            }
            catch (Exception e) { /* Ignore... */ }
        }
        else if (get16(buf, 0) == GZIPInputStream.GZIP_MAGIC) {
            // GZip-compressed data.
            in = new GZIPInputStream(in, bufferSize);
            log.trace("File content identified as GZIP-compressed");
        }
        else if ((buf[0] == BZ2_HEADERS[0]) &&
                 (buf[1] == BZ2_HEADERS[1]) && (buf[2] == BZ2_HEADERS[2])) {
            // BZip2-compressed data.
            in = new BZip2InputStream(in, false);
            log.trace("File content identified as BZip2-compressed");
        }
        // Else: regular file!

        return in;
    }

    /**
     * Retrieves data from the specified URL and saves them into the
     * specified file.
     * @param  u    the URL to download the data from.
     * @param  to   the local file to save data to.
     *
     * @return a {@link DownloadInfo} object holding some information
     *         provided by the server.
     * @throws IOException   if any error occurred reading or writing
     *                       the data.
     */
    public static DownloadInfo save(URL u, File to) throws IOException {
        return save(u, null, null, to);
    }

    /**
     * Retrieves data from the specified URL and saves them into the
     * specified file.
     * @param  u            the URL to download the data from.
     * @param  query        the data to send to the server (enforces
     *                      usage of POST method for HTTP connections).
     * @param  properties   the request properties, i.e. HTTP headers.
     * @param  to           the local file to save data to.
     *
     * @return a {@link DownloadInfo} object holding some information
     *         provided by the server.
     * @throws IOException   if any error occurred reading or writing
     *                       the data.
     */
    public static DownloadInfo save(URL u, String query,
                                    Map<String,String> properties, File to)
                                                            throws IOException {
        DownloadInfo info = null;
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
                out.write(query.getBytes(UTF_8));
                out.flush();
                out.close();
                out = null;
            }
            // Force server connection.
            log.trace("Connecting to \"{}\"...", u);
            cnx.connect();
            // Check for HTTP status code.
            int status = 0;
            if (cnx instanceof HttpURLConnection) {
                status = ((HttpURLConnection)cnx).getResponseCode();
                if (((status / 100) * 100) == 200) {
                    status = 0;                 // Success!
                }
            }
            info = new DownloadInfo(status,
                            parseContentType(cnx.getContentType()),
                            getExpiration(cnx, System.currentTimeMillis()));
            if (status == 0) {
                // No error. => Save data locally.
                log.debug("Downloading data from \"{}\" ({})...", u, info.mimeType);
                save(cnx.getInputStream(), to);
            }
            else if (status == 304) {
                // Not modified: Local file is up-to-date.
                // => NOP!
            }
            else if (status == 404) {
                // Not found.
                throw new FileNotFoundException(u.toString());
            }
            else {
                // Error. => Gather (first 1024 characters of) error message.
                char[] buf = new char[1024];
                int l = 0;
                Reader r = null;
                try {
                    String cs = MediaTypes.getCharset(info.mimeType);
                    InputStream in = ((HttpURLConnection)cnx).getErrorStream();
                    r = (cs == null)? new InputStreamReader(in):
                                      new InputStreamReader(in, cs);
                    l = r.read(buf);
                }
                catch (Exception e) { /* Ignore... */ }
                finally {
                    closeQuietly(r);
                }
                IOException e = new IOException("Failed to connect to \"" +
                                                u + "\": status=" + status);
                if (l > 0) {
                    log.fatal("{}, message=\"{}\"", e,
                                        e.getMessage(), new String(buf, 0, l));
                }
                // Else: No server-provided error message to log.
                //       => Just report failure.
                throw e;
            }
        }
        finally {
            closeQuietly(out);
        }
        return info;
    }

    /**
     * Reads data from the specified input stream and saves them into
     * the specified file. The input stream is automatically closed,
     * regardless whether the operation succeeded.
     * @param  from   the input stream to read the data from.
     * @param  to     the local file to save data to.
     *
     * @throws IOException   if any error occurred reading or writing
     *                       the data.
     * @see    #save(InputStream, File, boolean)
     */
    public static void save(InputStream from, File to) throws IOException {
        save(from, to, true);
    }

    /**
     * Reads data from the specified input stream and saves them into
     * the specified file.
     * @param  from         the input stream to read the data from.
     * @param  to           the local file to save data to.
     * @param  closeInput   whether to close the input stream after
     *                      all data have been read (case of a regular
     *                      file or URL connection) or leave it open
     *                      (case of a compressed file stream).
     *
     * @throws IOException   if any error occurred reading or writing
     *                       the data.
     */
    public static void save(InputStream from, File to, boolean closeInput)
                                                            throws IOException {
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
            if (closeInput) {
                closeQuietly(in);
            }
            closeQuietly(out);
            if (copyFailed) {
                to.delete();
            }
            else {
                long delay = System.currentTimeMillis() - t0;
                log.debug("{} MBs of data written to {} in {} seconds",
                          Double.valueOf((byteCount / 1000) / 1000.0), to,
                          Double.valueOf(delay / 1000.0));
            }
        }
    }

    /**
     * Copies a file.
     * @param  from       the file to copy.
     * @param  to         the target file location.
     * @param  compress   whether to (GZip) compress during copy.
     *
     * @throws IOException   if any error occurred reading or writing
     *                       the data.
     */
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
                    closeQuietly(in);
                    closeQuietly(out);
                }
            }
            else {
                FileInputStream  in  = null;
                FileOutputStream out = null;
                try {
                    in  = new FileInputStream(from);
                    out = new FileOutputStream(to);
                    FileChannel chIn  = in.getChannel();
                    FileChannel chOut = out.getChannel();

                    long start = 0L;
                    long end   = chIn.size();
                    while (end != 0L) {
                        long l = Math.min(end, chunkSize);
                        l = chIn.transferTo(start, l, chOut);
                        if (l == 0L) {
                            // Should at least copy one byte!
                            throw new IOException(
                                    "Copy stalled after " + start + " bytes");
                        }
                        start += l;
                        end   -= l;
                    }
                    chOut.force(true);  // Sync data on disk.
                    chOut.close();
                    out.flush();
                    out.close();
                    out = null;
                    copyFailed = false;
                    byteCount = start;
                }
                finally {
                    closeQuietly(in);
                    closeQuietly(out);
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

    /**
     * Deletes a file or directory, recursively.
     * @param  f         the file or directory to delete.
     * @param  recurse   whether to recursively delete the directory
     *                   content. Ignored if <code>f</code> is a file.
     */
    public static void delete(File f, boolean recurse) {
        if ((f != null) && (f.exists())) {
            if (f.isDirectory() && recurse) {
                for (File x : f.listFiles()) {
                    delete(x, true);
                }
            }
            f.delete();
        }
        // Else: ignore...
    }

    /**
     * Closes a file or a (byte or character) stream, absorbing errors.
     * @param  c   the stream to close  or <code>null</code> if the
     *             stream creation failed.
     */
    public final static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (Exception e) { /* Ignore... */ }
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Parses the value of the Content-Type HTTP header to extract
     * the content type (MIME type) and character encoding information.
     * @param  contentType   the value of the HTTP Content-Type header.
     *
     * @return the content type and character encoding as an array of
     *         strings.
     */
    private static MediaType parseContentType(String contentType) {
        MediaType t = MediaTypes.APPLICATION_OCTET_STREAM_TYPE;
        if ((isSet(contentType)) && (! CONTENT_UNKNOWN.equals(contentType))) {
            t = MediaType.valueOf(contentType);
        }
        return t;
    }

    /**
     * Returns the time when the document should be considered expired.
     * @return the time when the document should be considered expired,
     *         <code>0</code> if the document shall always be
     *         revalidated or <code>-1</code> if no expiry time was
     *         specified by the server.
     */
    private static long getExpiration(URLConnection cnx, long baseTime) {
        long expiry = -1L;
        String header = cnx.getHeaderField("Cache-Control");
        if (header != null) {
            for (String token : header.split("\\s*,\\s*")) {
               if ("must-revalidate".equals(token)) {
                   expiry = 0L;
               }
               else if (token.startsWith("max-age")) {
                   try {
                       int seconds = Integer.parseInt(
                                       token.substring(token.indexOf('=') + 1));
                       expiry = baseTime + (seconds * 1000L);
                   }
                   catch (Exception e) { /* Ignore... */ }
               }
            }
        }
        header = cnx.getHeaderField("Expires");
        if (header != null) {
            try {
                long expires = HttpDateFormat.parseDate(header).getTime();
                expiry = (expiry > 0L)? Math.min(expiry, expires): expires;
            }
            catch (Exception e) { /* Ignore... */ }
        }
        return expiry;
    }

    /**
     * Fetches unsigned 16-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     * @param  b        the byte array to read data from.
     * @param  offset   the offset of the first byte to read.
     *
     * @return the unsigned 16-bit value read at the specified location.
     */
    private final static int get16(byte b[], int offset) {
        return (b[offset] & 0xff) | ((b[offset+1] & 0xff) << 8);
    }

    /**
     * Fetches unsigned 32-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     * @param  b        the byte array to read data from.
     * @param  offset   the offset of the first byte to read.
     *
     * @return the unsigned 32-bit value read at the specified location.
     */
    private final static long get32(byte b[], int offset) {
        return get16(b, offset) | ((long)get16(b, offset+2) << 16);
    }


    //-------------------------------------------------------------------------
    // DownloadInfo nested class
    //-------------------------------------------------------------------------

    public final static class DownloadInfo
    {
        public final int httpStatus;
        public final MediaType mimeType;
        public final long expires;

        public DownloadInfo(int status, MediaType mediaType, long expires) {
            this.httpStatus = status;
            this.mimeType = mediaType;
            this.expires = expires;
        }

        @Override
        public String toString() {
            return "{ status: "   + this.httpStatus +
                   ", mimeType: \"" + MediaTypes.toString(this.mimeType) +
                   "\", expires: "  + this.expires + " }";
        }
    }


    //-------------------------------------------------------------------------
    // ByteCounterInputStream nested class
    //-------------------------------------------------------------------------

    /**
     * An {@link InputStream} {@link FilterInputStream decorator} that
     * counts and reports the number of bytes read.
     */
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
}
