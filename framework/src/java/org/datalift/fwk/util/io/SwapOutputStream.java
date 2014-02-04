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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.datalift.fwk.util.Env;


/**
 * An output stream which will retain data in memory until a specified
 * threshold is reached, and only then commit it to disk in a temporary
 * file.
 * <p>
 * If the stream is closed before the threshold is reached, the data
 * will not be written to disk at all.</p>
 * <p>
 * Once outputting data is complete, the method {@link #close()} shall
 * be invoked prior accessing the output data. The data can be retrieve
 * by accessing either the underlying stream ({@link #getInputStream()})
 * or the temporary file ({@link #getFile()}). In the latter case, the
 * user endorses the responsibility of deleting the file.</p>
 * <p>
 * If the data are not read one way or another, the user <b>must</b>
 * call the method {@link #reset()} to ensure the disk storage is
 * properly released.</p>
 *
 * @author lbihanic
 */
public final class SwapOutputStream extends OutputStream
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

    /**
     * The output stream to which data will be written prior to the
     * threshold being reached.
     */
    protected DirectAccessByteArrayOutputStream memoryOutputStream;
    /** The output stream to which data will be written at any given time. */
    private volatile OutputStream currentOutputStream;
    /**
     * The file to which output will be directed if the threshold
     * is exceeded.
     */
    private File outputFile;
    /** Whether to delete the temporary file after read. */
    private volatile int tmpFileOpenStreams = 0;
    /** The number of bytes written to the output stream. */
    private long written;
    /** True when {@link #close()} has been called successfully. */
    private volatile boolean closed = false;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a swap output stream that will save data to a file
     * beyond the specified threshold.
     * @param  threshold   the number of bytes at which to switch to
     *                     file.
     */
    public SwapOutputStream(int threshold) {
        this(threshold, null, null);
    }

    /**
     * Creates a swap output stream that will save data to a file
     * beyond the specified threshold.
     * @param  threshold   the number of bytes at which to switch to
     *                     file.
     * @param  prefix      the temporary file prefix or
     *                     <code>null</code> to use the default prefix.
     * @param  suffix      the temporary file suffix or
     *                     <code>null</code> to use the default suffix.
     */
    public SwapOutputStream(int threshold, String prefix, String suffix) {
        this(threshold, prefix, suffix, null);
    }

    /**
     * Creates a swap output stream that will save data to a file
     * in the specified directory beyond the specified threshold.
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
    public SwapOutputStream(int threshold, String prefix,
                                           String suffix, File directory) {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold");
        }
        this.threshold        = threshold;
        this.tmpFilePrefix    =
            ((prefix != null) && (prefix.trim().length() != 0))? prefix: "tmp";
        this.tmpFileSuffix    = suffix;
        this.tmpFileDirectory = directory;

        this.init();
    }

    //-------------------------------------------------------------------------
    // InputStream contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void write(int b) throws IOException {
        this.checkThreshold(1);
        this.currentOutputStream.write(b);
        this.written++;
    }

    /** {@inheritDoc} */
    @Override
    public void write(byte b[]) throws IOException {
        this.checkThreshold(b.length);
        this.currentOutputStream.write(b);
        this.written += b.length;
    }

    /** {@inheritDoc} */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (off < 0) {
            throw new IllegalArgumentException("off");
        }
        if (len < 0) {
            throw new IllegalArgumentException("len");
        }
        this.checkThreshold(len);
        this.currentOutputStream.write(b, off, len);
        this.written += len;
    }

    /** {@inheritDoc} */
    @Override
    public void flush() throws IOException {
        this.currentOutputStream.flush();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (this.closed == false) {
            this.flush();
            this.currentOutputStream.close();
            this.closed = true;
        }
        // Else: ignore...
    }

    //-------------------------------------------------------------------------
    // SwapOutputStream contract definition
    //-------------------------------------------------------------------------

    /**
     * Resets this output stream so that all currently accumulated
     * output is discarded, i.e. the memory buffer is cleared and the
     * disk storage (if any) is deleted.
     * <p>
     * If is has not been closed, the output stream can be used again,
     * reusing the already allocated buffer space.</p>
     *
     * @see    #close()
     */
    public void reset() {
        if (this.isInMemory()) {
            this.memoryOutputStream.reset();
            this.memoryOutputStream = null;
        }
        else {
            if (this.closed == false) {
                try {
                    this.currentOutputStream.close();
                }
                catch (IOException e) { /* Ignore... */ }

                this.init();
            }
            this.deleteTemporaryFile(true);
        }
        this.currentOutputStream = null;
    }

    /**
     * Returns a newly allocated byte array containing the data hold
     * by this output stream, assuming that the data has been retained
     * in memory.  If the data was written to disk, this method returns
     * <code>null</code>.
     * @return the memory contents of this output stream, as a byte
     *         array or <code>null</code> if no such data is available.
     */
    public byte[] toByteArray() {
        byte[] data = null;

        if (this.isInMemory()) {
            data = this.memoryOutputStream.toByteArray();
        }
        return data;
    }

    /**
     * Returns the number of bytes that have been written to this
     * output stream.
     * @return the number of bytes written.
     */
    public long size() {
        return this.written;
    }

    /**
     * Writes the data from this output stream to the specified
     * stream, after it has been closed.
     * @param  out   output stream to write to.
     *
     * @throws IOException if this stream is not yet closed or
     *                     an I/O error occurs.
     */
    public void writeTo(OutputStream out) throws IOException {
        this.checkClosed();

        if (this.isInMemory()) {
            this.memoryOutputStream.writeTo(out);
        }
        else {
            InputStream in = this.getInputStream();
            try {
                this.copy(in, out);
            }
            finally {
                try { in.close(); } catch (IOException e) { /* Ignore... */ }
            }
        }
    }

    /**
     * Determines whether or not the data for this output stream has
     * been retained in memory.
     * @return <code>true</code> if the data is available in memory;
     *         <code>false</code> otherwise.
     *
     * @see    #getInputStream()
     * @see    #getFile()
     */
    public boolean isInMemory() {
        return (this.currentOutputStream == this.memoryOutputStream);
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
        this.checkClosed();

        InputStream is = null;
        if (this.isInMemory()) {
            is = new ByteArrayInputStream(
                                this.memoryOutputStream.getBuffer(), 0,
                                this.memoryOutputStream.size());
        }
        else {
            is = this.getFileInputStream();
        }
        return is;
    }

    /**
     * Returns the temporary file storage or <code>null</code> if
     * data are held in memory.
     * <p>
     * Method {@link #getInputStream()} is the recommended way to
     * access the written data as it is storage-independent.</p>
     * @return the temporary file used by this stream, or
     *         <code>null</code> if no such file exists.
     *
     * @see    #getInputStream()
     */
    public File getFile() {
        this.tmpFileOpenStreams++;
        return this.outputFile;
    }

    /**
     * Returns the temporary file storage, forcing the creation of the
     * file if requested.
     * @param  force   whether to force the creation of the file if it
     *                 does not exist yet.
     *
     * @return the temporary file.
     * @throws IOException if any error occurred creating the file.
     */
    public File getFile(boolean force) throws IOException {
        if ((force) && (this.isInMemory())) {
            this.createFile();
        }
        return this.getFile();
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Checks to see if writing the specified number of bytes would
     * cause the configured threshold to be exceeded.  If so, switches
     * the underlying output stream from a memory based stream to one
     * that is backed by disk.
     * @param  count   the number of bytes about to be written to the
     *                 underlying stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    private void checkThreshold(int count) throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }

        if ((this.memoryOutputStream != null) &&
            (this.written + count > this.threshold)) {
            this.createFile();
        }
    }

    /**
     * Creates a temporary file and dumps memory buffer content to it.
     *
     * @throws IOException if any error occurred while creating the
     *                     temporary file or writing data.
     */
    private void createFile() throws IOException {
        File out = File.createTempFile(this.tmpFilePrefix,
                                       this.tmpFileSuffix,
                                       this.tmpFileDirectory);
        OutputStream fos = new FileOutputStream(out);
        try {
            this.memoryOutputStream.writeTo(fos);
            this.currentOutputStream = fos;
            this.memoryOutputStream  = null;

            out.deleteOnExit();
            this.outputFile = out;

            if (this.closed) {
                fos.flush();
                fos.close();
            }
        }
        catch (IOException e) {
            try { fos.close(); } catch (IOException e2) { /* Ignore.. */ }
            out.delete();

            throw e;
        }
    }

    /**
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need
     * to use a <code>BufferedInputStream</code>.</p>
     * @param  input    the <code>InputStream</code> to read from.
     * @param  output   the <code>OutputStream</code> to write to.
     *
     * @throws NullPointerException if the input or output is
     *                              <code>null</code>.
     * @throws IOException          if an I/O error occurs.
     */
    private void copy(InputStream input, OutputStream output)
                                                        throws IOException {
        byte[] buffer = new byte[Env.getFileBufferSize()];
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
    }

    /**
     * Initializes the memory output stream.
     */
    private void init() {
        this.memoryOutputStream = new DirectAccessByteArrayOutputStream(
                                                            this.threshold / 4);
        this.currentOutputStream = this.memoryOutputStream;
    }

    /**
     * Check that this output stream has been closed.
     *
     * @throws IOException if the stream is not closed.
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
            is = new FileInputStream(this.outputFile)
                    {
                        public void close() throws IOException {
                            try {
                                super.close();
                            }
                            catch (Exception e) { /* Ignore... */ }
                            tmpFileOpenStreams--;
                            deleteTemporaryFile(false);
                        }
                    };
            this.tmpFileOpenStreams++;
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
            ((this.tmpFileOpenStreams == 0) || (force))) {
            this.outputFile.delete();
        }
    }

    //-------------------------------------------------------------------------
    // DirectAccessByteArrayOutputStream nested class definition
    //-------------------------------------------------------------------------

    /**
     * A ByteArrayOutputStream allowing direct access to its internal
     * buffer.
     */
    protected final static class DirectAccessByteArrayOutputStream
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
         * @return the underlying byte array.
         */
        public byte[] getBuffer() {
            return this.buf;
        }
    }
}
