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

package org.datalift.filestore.mongodb;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import org.datalift.fwk.FileStore;
import org.datalift.fwk.util.io.FileUtils;

import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A {@link FileStore} implementation based on MongoDB GridFS.
 *
 * @author lbihanic
 */
public final class MongoFileStore extends FileStore
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The file last modified timestamp metadata. */
    private final static String LAST_MODIFIED_METADATA = "lastModified";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final MongoClientURI uri;
    private final GridFS fs;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Create a new MongoDB GridFS distributed file store access.
     * @param  uri   the MongoDB database URI.
     * @param  fs    the MongoDB GridFS.
     */
    /* package */ MongoFileStore(MongoClientURI uri, GridFS fs) {
        this.uri = uri;
        this.fs  = fs;
    }

    //-------------------------------------------------------------------------
    // FileStore contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean isLocal() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public File getFile(String path) {
        if (isBlank(path)) {
            throw new IllegalArgumentException("path");
        }
        return new MongoFile(path);
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(File file) {
        boolean exist = false;
        try {
            this.ensureManagedFile(file);
            exist = file.exists();
        }
        catch (Exception e) { /* Ignore... */ }

        return exist;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream(File file) throws IOException {
        return this.ensureManagedFile(file).getInputStream();
    }

    /** {@inheritDoc} */
    @Override
    public void save(InputStream from, File to) throws IOException {
        if (from == null) {
            throw new IllegalArgumentException("from");
        }
        this.ensureManagedFile(to).writeData(from);
    }

    /** {@inheritDoc} */
    @Override
    public void read(File from, File to) throws IOException {
        FileUtils.save(this.getInputStream(from), to, true);
    }

    /** {@inheritDoc} */
    @Override
    public boolean delete(File file) {
        return (this.exists(file))? file.delete(): false;
    }

    //---------------------------------------------------------------------
    // Object contract support
    //---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.uri.getURI();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        return ((o instanceof MongoFileStore) &&
                (((MongoFileStore)o).uri.getURI().equals(this.uri.getURI())));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.uri.getURI().hashCode();
    }

    //---------------------------------------------------------------------
    // Specific implementation
    //---------------------------------------------------------------------

    /**
     * Checks that the specified file is relative to this file store
     * root directory.
     * @param  file   the file to check.
     *
     * @throws IOException if the file is not a MongoDB GridFS file.
     */
    private MongoFile ensureManagedFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file");
        }
        if (! ((file instanceof MongoFile) &&
               (((MongoFile)file).getFS() == this.fs))) {
            throw new FileNotFoundException("Invalid file: " + file);
        }
        return (MongoFile)file;
    }


    //-------------------------------------------------------------------------
    // MongoFile nested class
    //-------------------------------------------------------------------------

    public final class MongoFile extends File
    {
        //---------------------------------------------------------------------
        // Instance members
        //---------------------------------------------------------------------

        /** The file canonical (and absolute) path. */
        private final String canonicalPath;
        /** The MongoDB file. */
        private GridFSDBFile dbFile = null;

        //---------------------------------------------------------------------
        // Constructors
        //---------------------------------------------------------------------

        /**
         * Creates a new MongoDB-backed file.
         * @param  path   the file path.
         */
        private MongoFile(String path) {
            super(path);
            this.canonicalPath = this.canonicalize(path);
        }

        //---------------------------------------------------------------------
        // File contract support
        //---------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        public boolean exists() {
            return this.getDbFile(true) != null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean createNewFile() throws IOException {
            boolean created = false;
            if (! this.exists()) {
                this.writeData(null);
                created = true;
            }
            return created;
        }

        /** {@inheritDoc} */
        @Override
        public boolean delete() {
            boolean deleted = false;
            if (this.exists()) {
                try {
                    fs.remove(this.getCanonicalPath());
                    deleted = true;
                }
                catch (Exception e) { /* Report deletion did not occur. */ }
            }
            return deleted;
        }

        /** {@inheritDoc} */
        @Override
        public void deleteOnExit() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public boolean renameTo(File dest) {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public String getAbsolutePath() {
            return this.getCanonicalPath();
        }

        /** {@inheritDoc} */
        @Override
        public File getAbsoluteFile() {
            return this.getCanonicalFile();
        }

        /** {@inheritDoc} */
        @Override
        public String getCanonicalPath() {
            return this.canonicalPath;
        }

        /** {@inheritDoc} */
        @Override
        public File getCanonicalFile() {
            return new MongoFile(this.getCanonicalPath());
        }

        /** {@inheritDoc} */
        @Override
        public long lastModified() {
            long lastModified = 0L;
            GridFSDBFile f = this.getDbFile(true);
            if (f != null) {
                Object attr = f.get(LAST_MODIFIED_METADATA);
                if (attr instanceof Long) {
                    lastModified = ((Long)attr).longValue();
                }
            }
            return lastModified;
        }

        /** {@inheritDoc} */
        @Override
        public long length() {
            GridFSDBFile f = this.getDbFile(true);
            return (f != null)? f.getLength(): 0L;
        }

        /** {@inheritDoc} */
        @Override
        public boolean setLastModified(long time) {
            boolean updated = false;
            GridFSDBFile f = this.getDbFile(true);
            if (f != null) {
                try {
                    f.put(LAST_MODIFIED_METADATA, wrap(time));
                    f.save();
                    updated = true;
                }
                catch (Exception e) { /* Report update did not occur. */ }
            }
            return updated;
        }

        /** {@inheritDoc} */
        @Override public boolean canRead()      { return this.exists(); }
        /** {@inheritDoc} */
        @Override public boolean canWrite()     { return this.exists(); }
        /** {@inheritDoc} */
        @Override public boolean canExecute()   { return false; }
        /** {@inheritDoc} */
        @Override public boolean isFile()       { return this.exists(); }
        /** {@inheritDoc} */
        @Override public boolean isDirectory()  { return false; }
        /** {@inheritDoc} */
        @Override public boolean isAbsolute()   { return false; }
        /** {@inheritDoc} */
        @Override public boolean isHidden()     { return false; }
        /** {@inheritDoc} */

        /** {@inheritDoc} */
        @Override public String[] list() { return null;}
        /** {@inheritDoc} */
        @Override public String[] list(FilenameFilter filter) { return null; }
        /** {@inheritDoc} */
        @Override public File[] listFiles() { return null; }
        /** {@inheritDoc} */
        @Override public File[] listFiles(FilenameFilter filter) { return null; }
        /** {@inheritDoc} */
        @Override public File[] listFiles(FileFilter filter) { return null; }

        /** {@inheritDoc} */
        @Override public boolean mkdir()  { return false; }
        /** {@inheritDoc} */
        @Override public boolean mkdirs() { return false; }

        /** {@inheritDoc} */
        @Override public boolean setReadOnly() { return false; }
        /** {@inheritDoc} */
        @Override public boolean setWritable(boolean writable, boolean ownerOnly) { return false; }
        /** {@inheritDoc} */
        @Override public boolean setWritable(boolean writable) { return false; }
        /** {@inheritDoc} */
        @Override public boolean setReadable(boolean readable, boolean ownerOnly) { return false; }
        /** {@inheritDoc} */
        @Override public boolean setReadable(boolean readable) { return false; }
        /** {@inheritDoc} */
        @Override public boolean setExecutable(boolean executable, boolean ownerOnly) { return false; }
        /** {@inheritDoc} */
        @Override public boolean setExecutable(boolean executable) { return false; }

        /** {@inheritDoc} */
        @Override
        public int compareTo(File pathname) {
            return this.getAbsolutePath().compareTo(pathname.getAbsolutePath());
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            return ((obj instanceof MongoFile) &&
                    (this.compareTo((MongoFile)obj) == 0));
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.getCanonicalPath().hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public URI toURI() {
            String dbUri = uri.getURI();
            int i = dbUri.indexOf('?');
            if (i != -1) {
                dbUri = dbUri.substring(0, i);
            }
            return URI.create(dbUri + "?filename=" + this.getCanonicalPath());
        }

        /**
         * Returns the underlying MondoDB GridFS storage.
         * @return the underlying MondoDB GridFS storage.
         */
        /* package */ GridFS getFS() {
            return fs;
        }

        /**
         * Returns an input stream to read the file content from.
         * @return an input stream on the file data.
         * @throws IOException if any error occurred accessing the
         *         MongoDB file.
         */
        /* package */ InputStream getInputStream() throws IOException {
            try {
                GridFSDBFile f = this.getDbFile(false);
                if (f == null) {
                    throw new FileNotFoundException(this.getPath());
                }
                return f.getInputStream();
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }

        /**
         * Replaces the MongoDB file content with the data read from
         * the specified input stream.
         * @param  from   the input stream to read the file data from.
         * @throws IOException if any error occurred accessing the
         *         MongoDB file.
         */
        /* package */ void writeData(InputStream from) throws IOException {
            try {
                String path = this.getCanonicalPath();
                if (this.getDbFile(false) != null) {
                    fs.remove(path);
                    this.dbFile = null;
                }
                GridFSInputFile f = (from == null)?
                                            fs.createFile(path):
                                            fs.createFile(from, path, true);
                f.put(LAST_MODIFIED_METADATA, wrap(System.currentTimeMillis()));
                f.save();
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }

        /**
         * Returns the MongoDB {@link GridFSDBFile} object associated
         * to this file.
         * @param  safe   whether to silently absorb error that may
         *                occur while accessing the underlying file
         *                store.
         *
         * @return the MongoDB <code>GridFSDBFile</code> object
         *         associated to the file or <code>null</code> if no
         *         MongoDB object matched the file canonical path or
         *         an error occurred and <code>safe</code> was set to
         *         <code>true</code>.
         * @throws MongoException if any error occurred while accessing
         *         the underlying file storage and <code>safe</code>
         *         was set to <code>false</code>.
         */
        private GridFSDBFile getDbFile(boolean safe) {
            if (this.dbFile == null) {
                try {
                    this.dbFile = fs.findOne(this.getCanonicalPath());
                }
                catch (MongoException e) {
                    if (! safe) throw e;        // Propagate...
                }
            }
            return this.dbFile;
        }

        /**
         * Returns the canonical (and also absolute) path of the file
         * for the MongoDB GridFS storage.
         * @param  path   the application-specified file path.
         *
         * @return the canonical path.
         */
        private String canonicalize(String path) {
            // Use '/' as separator character.
            if (File.separatorChar != '/') {
                path = path.replace(File.separatorChar, '/');
            }
            // Ensure no '\' character is present.
            if (path.indexOf('\\') != -1) {
                path = path.replace('\\', '/');
            }
            // Ensure path is absolute.
            if (path.charAt(0) != '/') {
                path = "/" + path;
            }
            return path;
        }
    }
}
