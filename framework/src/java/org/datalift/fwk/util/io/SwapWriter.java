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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.datalift.fwk.util.Env;


/**
 * A writer which will retain data in memory until a specified
 * threshold is reached, and only then commit it to disk in a temporary
 * file.
 * <p>
 * If the writer is closed before the threshold is reached, the data
 * will not be written to disk at all.</p>
 * <p>
 * Once outputting data is complete, the method {@link #close()} shall
 * be invoked prior accessing the output data. The data can be retrieve
 * by accessing either the underlying stream ({@link #getReader()} or
 * {@link #getInputStream()}) or the temporary file
 *({@link #getFile()}). In the latter case, the user endorses the
 * responsibility of deleting the file.</p>
 * <p>
 * If the data are not read one way or another, the user <b>must</b>
 * call the method {@link #reset()} to ensure the disk storage is
 * properly released.</p>
 *
 * @author lbihanic
 */
public final class SwapWriter extends Writer
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The threshold at which the output will be switched to file. */
    public final int threshold;
    /** The output file prefix. */
    public final String tmpFilePrefix;
    /** The output file suffix. */
    public final String tmpFileSuffix;
    /** The output file directory. */
    public final File tmpFileDirectory;
    /** The character set to use when outputting data to disk. */
    public final Charset charset;

    /**
     * The writer to which data will be written prior to the
     * threshold being reached.
     */
    private final DirectAccessCharArrayWriter memoryWriter;
    /** The writer to which data will be written at any given time. */
    private Writer currentWriter;
    /**
     * The file to which output will be directed if the threshold
     * is exceeded.
     */
    private File outputFile = null;
    /** Whether to delete the temporary file after read. */
    private boolean deleteFileAfterRead = true;
    /** The number of characters written to the stream. */
    private long written = 0L;
    /** True when {@link #close()} has been called successfully. */
    private boolean closed = false;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a swap writer that will save data to a file beyond
     * the specified threshold.
     * @param  threshold   the number of bytes at which to switch
     *                     to file.
     */
    public SwapWriter(int threshold) {
        this(threshold, null, null);
    }

    /**
     * Creates a swap writer that will save data to a file beyond
     * the specified threshold with the specified encoding.
     * @param  threshold     the number of bytes at which to switch
     *                       to file.
     * @param  charsetName   the character set to encode the file output
     *                       or <code>null</code> to use the platform
     *                       default.
     *
     * @throws UnsupportedEncodingException   if the specified encoding
     *                                        is not supported.
     */
    public SwapWriter(int threshold, String charsetName)
                                        throws UnsupportedEncodingException {
        this(threshold, null, null, charsetName);
    }

    /**
     * Creates a swap writer that will save data to a file beyond
     * the specified threshold.
     * @param  threshold   the number of characters at which to switch
     *                     to file.
     * @param  prefix      the temporary file prefix or
     *                     <code>null</code> to use the default prefix.
     * @param  suffix      the temporary file suffix or
     *                     <code>null</code> to use the default suffix.
     */
    public SwapWriter(int threshold, String prefix, String suffix) {
        this(threshold, prefix, suffix, (File)null);
    }

    /**
     * Creates a swap writer that will save data to a file beyond
     * the specified threshold with the specified encoding.
     * @param  threshold     the number of characters at which to
     *                       switch to file.
     * @param  prefix        the temporary file prefix or
     *                       <code>null</code> to use the default
     *                       prefix.
     * @param  suffix        the temporary file suffix or
     *                       <code>null</code> to use the default
     *                       suffix.
     * @param  charsetName   the character set to encode the file output
     *                       or <code>null</code> to use the platform
     *                       default.
     *
     * @throws UnsupportedEncodingException   if the specified encoding
     *                                        is not supported.
     */
    public SwapWriter(int threshold, String prefix,
                                     String suffix, String charsetName)
                                        throws UnsupportedEncodingException {
        this(threshold, prefix, suffix, null, charsetName);
    }

    /**
     * Creates a swap writer that will save data to a file in the
     * specified directory beyond the specified threshold.
     *
     * @param  threshold   the number of bytes at which to switch to
     *                     file.
     * @param  prefix      the temporary file prefix or
     *                     <code>null</code> to use the default prefix.
     * @param  suffix      the temporary file suffix or
     *                     <code>null</code> to use the default suffix.
     * @param  directory   the directory in which the file is to be
     *                     created, or <code>null</code> if the default
     *                     temporary-file directory is to be used.
     */
    public SwapWriter(int threshold, String prefix,
                                     String suffix, File directory)
    {
        this(threshold, new DirectAccessCharArrayWriter(threshold / 4),
                        prefix, suffix, directory, Charset.defaultCharset());
    }

    /**
     * Creates a swap writer that will save data to a file in the
     * specified directory beyond the specified threshold with the
     * specified encoding.
     *
     * @param  threshold     the number of bytes at which to switch to
     *                       file.
     * @param  prefix        the temporary file prefix or
     *                       <code>null</code> to use the default
     *                       prefix.
     * @param  suffix        the temporary file suffix or
     *                       <code>null</code> to use the default
     *                       suffix.
     * @param  directory     the directory in which the file is to be
     *                       created, or <code>null</code> if the
     *                       default temporary-file directory is to be
     *                       used.
     * @param  charsetName   the charset to encode the file output or
     *                       <code>null</code> to use the platform
     *                       default.
     *
     * @throws UnsupportedEncodingException   if the specified encoding
     *                                        is not supported.
     */
    public SwapWriter(int threshold, String prefix, String suffix,
                                     File directory, String charsetName)
                                        throws UnsupportedEncodingException {
        this(threshold, new DirectAccessCharArrayWriter(threshold / 4),
             prefix, suffix, directory,
             ((charsetName != null) && (charsetName.trim().length() != 0))?
                                                Charset.forName(charsetName):
                                                Charset.defaultCharset());
    }

    /**
     * Private (main) constructor.
     */
    private SwapWriter(int threshold,
                       DirectAccessCharArrayWriter memoryWriter,
                       String prefix, String suffix, File directory,
                       Charset charset) {
        super(memoryWriter);    // Synchronize on memory buffer.

        if (threshold < 0) {
            throw new IllegalArgumentException("threshold");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset");
        }
        this.threshold        = threshold;
        this.memoryWriter     = memoryWriter;
        this.currentWriter    = memoryWriter;

        this.tmpFilePrefix    =
            ((prefix != null) && (prefix.trim().length() != 0))? prefix: "tmp";
        this.tmpFileSuffix    = suffix;
        this.tmpFileDirectory = directory;
        this.charset          = charset;
    }

    //-------------------------------------------------------------------------
    // Writer contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void write(int c) throws IOException {
        synchronized (this.lock) {
            this.checkThreshold(1);
            this.currentWriter.write(c);
            this.written++;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
        synchronized (this.lock) {
            this.checkThreshold(len);
            this.currentWriter.write(cbuf, off, len);
            this.written += len;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void flush() throws IOException {
        synchronized (this.lock) {
            this.currentWriter.flush();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            this.flush();
            this.currentWriter.close();
            this.closed = true;
        }
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------
    
    /**
     * Returns a newly allocated string containing the character data
     * hold by this writer, assuming that the data has been retained
     * in memory.  If the data was written to disk, this method returns
     * <code>null</code>.
     * @return the memory contents of this writer, as a string or
     *         <code>null</code> if no such data is available.
     */
    @Override
    public String toString() {
        synchronized (this.lock) {
            String data = null;
            if (this.isInMemory()) {
                data = this.memoryWriter.toString();
            }
            return data;
        }
    }

    //-------------------------------------------------------------------------
    // SwapWriter contract definition
    //-------------------------------------------------------------------------

    /**
     * Resets this writer so that all currently accumulated output
     * is discarded, i.e. the memory buffer is cleared and the disk
     * storage (if any) is deleted.
     * <p>
     * If is has not been closed, the writer can be used again,
     * reusing the already allocated buffer space.</p>
     *
     * @see    #close()
     */
    public void reset() {
        synchronized (this.lock) {
            if (! this.isInMemory()) {
                // Close and delete file buffer.
                if (this.closed == false) {
                    try {
                        this.currentWriter.close();
                    }
                    catch (IOException e) { /* Ignore... */ }
                }
                this.deleteTemporaryFile(true);
            }
            this.memoryWriter.reset();
            this.currentWriter = this.memoryWriter;
            this.closed = false;
            this.written = 0L;
            this.outputFile = null;     // Switch back to in-memory buffer.
        }
    }

    /**
     * Returns the number of bytes that have been written to this
     * writer.
     * @return the number of characters written.
     */
    public long size() {
        return this.written;
    }

    /**
     * Writes the data from this writer to the specified stream,
     * after it has been closed.
     * @param  out   writer to write to.
     *
     * @throws IOException if this writer is not yet closed or
     *                     an I/O error occurs.
     */
    public void writeTo(Writer out) throws IOException {
        synchronized (this.lock) {
            this.checkClosed();

            if (this.isInMemory()) {
                this.memoryWriter.writeTo(out);
            }
            else {
                Reader in = new InputStreamReader(
                            new FileInputStream(this.getFile()), this.charset);
                try {
                    this.copy(in, out);
                }
                finally {
                    try { in.close(); } catch (IOException e) { /* Ignore... */ }
                }
            }
        }
    }

    /**
     * Determines whether or not the data for this writer has been
     * retained in memory.
     * @return <code>true</code> if the data is available in memory;
     *         <code>false</code> otherwise.
     *
     * @see    #getInputStream()
     * @see    #getFile()
     */
    public boolean isInMemory() {
        return (this.outputFile == null);
    }

    /**
     * Returns a reader to the data held by this stream, regardless
     * their actual storage (memory or temporary file).
     * @return a reader to the data of this stream.
     *
     * @throws IOException if this writer is not yet closed or
     *                     an I/O error occurs.
     */
    public Reader getReader() throws IOException {
        synchronized (this.lock) {
            this.checkClosed();

            Reader reader = null;
            if (this.isInMemory()) {
                reader = new CharArrayReader(this.memoryWriter.getBuffer(), 0,
                                             this.memoryWriter.size());
            }
            else {
                reader = new InputStreamReader(this.getFileInputStream(),
                                               this.charset);
            }
            return reader;
        }
    }

    /**
     * Returns an input stream to the data held by this stream,
     * regardless their actual storage (memory or temporary file).
     * @return an input stream to the data of this stream.
     *
     * @throws IOException if this stream is not yet closed or
     *                     an I/O error occurs.
     */
    public InputStream getInputStream() throws IOException {
        synchronized (this.lock) {
            this.checkClosed();

            InputStream is = null;
            if (this.isInMemory()) {
                DirectAccessByteArrayOutputStream buf =
                                new DirectAccessByteArrayOutputStream(
                                                        (int)(this.size() * 2));
                Writer osw = new OutputStreamWriter(buf, this.charset);
                this.memoryWriter.writeTo(osw);
    
                is = new ByteArrayInputStream(buf.getBuffer(), 0, buf.size());
            }
            else {
                is = this.getFileInputStream();
            }
            return is;
        }
    }

    /**
     * Returns the temporary file storage or <code>null</code> if
     * data are held in memory.
     * <p>
     * Method {@link #getReader()} is the recommended way to access
     * the written data as it is storage-independent.</p>
     * @return the temporary file used by this stream, or
     *         <code>null</code> if no such file exists.
     *
     * @see    #getReader()
     */
    public File getFile() {
        this.deleteFileAfterRead = false;
        return this.outputFile;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Checks to see if writing the specified number of characters
     * would cause the configured threshold to be exceeded.  If so,
     * switches the underlying writer from a memory based stream to
     * one that is backed by disk.
     *
     * @param  count   the number of characters about to be written to
     *                 the underlying stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    private void checkThreshold(int count) throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
        if ((this.isInMemory()) && ((this.written + count) > this.threshold)) {
            File out = File.createTempFile(this.tmpFilePrefix,
                                           this.tmpFileSuffix,
                                           this.tmpFileDirectory);
            try {
                Writer fw = new OutputStreamWriter(
                                    new FileOutputStream(out), this.charset);
                this.memoryWriter.writeTo(fw);
                this.currentWriter = fw;
                this.memoryWriter.reset();

                out.deleteOnExit();
                this.outputFile = out;
            }
            catch (IOException e) {
                out.delete();
                throw e;
            }
        }
    }

    /**
     * Copies bytes from a <code>Reader</code> to a
     * <code>Writer</code>.
     * <p>
     * This method buffers the input internally, so there is no need
     * to use a <code>BufferedReader</code>.</p>
     * @param  input    the <code>Reader</code> to read from.
     * @param  output   the <code>Writer</code> to write to.
     *
     * @throws NullPointerException if the input or output is
     *                              <code>null</code>.
     * @throws IOException          if an I/O error occurs.
     */
    private void copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[Env.getFileBufferSize()];
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
    }

    /**
     * Check that this writer has been closed.
     *
     * @throws IOException   if the stream is not closed.
     */
    private void checkClosed() throws IOException {
        if (this.closed == false) {
            throw new IOException("Stream not closed");
        }
    }

    /**
     * Returns an input stream extracting data from the temporary
     * file.
     * @return an input stream extracting data from the temporary
     *         file or <code>null</code> if data are held in memory.
     *
     * @throws IOException if an I/O error occurs.
     */
    private InputStream getFileInputStream() throws IOException {
        InputStream is = null;
        if (this.outputFile != null) {
            is = new FileInputStream(this.outputFile) {
                    public void close() throws IOException {
                        super.close();
                        deleteTemporaryFile(false);
                    }
                };
        }
        return is;
    }

    /**
     * Deletes the temporary file once data have been read unless the
     * user endorsed the responsibility for it (by calling
     * {@link #getFile()}).
     * @param  force   whether to force file deletion even if the file
     *                 responsibility was transferred to the user.
     */
    private void deleteTemporaryFile(boolean force) {
        if ((this.outputFile != null) &&
            ((this.deleteFileAfterRead) || (force))) {
            this.outputFile.delete();
        }
    }

    //-------------------------------------------------------------------------
    // DirectAccessCharArrayWriter nested class definition
    //-------------------------------------------------------------------------

    private final static class DirectAccessCharArrayWriter
                                                extends CharArrayWriter {
        /**
         * Creates a new character array writer with the specified
         * initial capacity.
         * @param  capacity   the initial capacity of the buffer.
         */
        public DirectAccessCharArrayWriter(int capacity) {
            super(Math.max(capacity, Env.MIN_FILE_BUFFER_SIZE));
        }

        /**
         * Returns the underlying character array where data is
         * stored.
         *
         * @return the underlying character array.
         */
        public char[] getBuffer() {
            return this.buf;
        }
    }

    //-------------------------------------------------------------------------
    // DirectAccessByteArrayOutputStream nested class definition
    //-------------------------------------------------------------------------

    /**
     * A ByteArrayOutputStream allowing direct access to its internal
     * buffer.
     */
    private final static class DirectAccessByteArrayOutputStream
                                                extends ByteArrayOutputStream {
        /**
         * Creates a new character array writer with the specified
         * initial capacity.
         * @param  capacity   the initial capacity of the buffer.
         */
        public DirectAccessByteArrayOutputStream(int capacity) {
            super(Math.max(capacity, Env.MIN_FILE_BUFFER_SIZE));
        }

        /**
         * Returns the underlying byte array where data is stored.
         *
         * @return the underlying byte array.
         */
        public byte[] getBuffer() {
            return this.buf;
        }
    }
}
