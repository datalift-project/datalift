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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.FileStore;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.io.FileUtils;

import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * An implementation of {@link FileStoreFactory} that relies on the
 * local file system.
 *
 * @author lbihanic
 */
public class LocalFileStoreFactory extends FileStoreFactory
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // FileStoreFactory contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public FileStore getFileStore(String path, Configuration cfg) {
        FileStore fs = null;

        if (! isBlank(path)) {
            try {
                fs = new LocalFileStore(new File(path));
            }
            catch (IOException e) {
                log.warn("Local file store initialization failed for {}", e,
                         path);
            }
        }
        return fs;
    }


    //-------------------------------------------------------------------------
    // LocalFileStore nested class
    //-------------------------------------------------------------------------

    /**
     * A {@link FileStore} implementation that stores files on the
     * local file system.
     */
    public final static class LocalFileStore extends FileStore
    {
        //---------------------------------------------------------------------
        // Instance members
        //---------------------------------------------------------------------

        private final File root;

        //---------------------------------------------------------------------
        // Constructors
        //---------------------------------------------------------------------

        /**
         * Creates a new file store.
         * @param  path   the file store root directory.
         *
         * @throws IOException if any error occurred accessing the
         *         specified root directory path.
         */
        protected LocalFileStore(File path) throws IOException {
            super();

            if (path == null) {
                throw new IllegalArgumentException("path");
            }
            if (! path.isDirectory()) {
                throw new FileNotFoundException("Not a directory: " + path);
            }
            this.root = path;
        }

        //---------------------------------------------------------------------
        // FileStore contract support
        //---------------------------------------------------------------------

        /**
         * {@inheritDoc}
         * @return <code>true</code> always.
         */
        @Override
        public boolean isLocal() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public File getFile(String path) {
            if (isBlank(path)) {
                throw new IllegalArgumentException("path");
            }
            return new File(this.root, path);
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
            this.ensureManagedFile(file);
            return new FileInputStream(file);
        }

        /** {@inheritDoc} */
        @Override
        public void save(InputStream from, File to) throws IOException {
            if (from == null) {
                throw new IllegalArgumentException("from");
            }
            this.ensureManagedFile(to);
            FileUtils.save(from, to);
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
            return this.root.getPath();
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object o) {
            return ((o instanceof LocalFileStore) &&
                    (((LocalFileStore)o).root.equals(this.root)));
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.root.hashCode();
        }

        //---------------------------------------------------------------------
        // Specific implementation
        //---------------------------------------------------------------------

        /**
         * Checks that the specified file is relative to this file store
         * root directory.
         * @param  file   the file to check.
         *
         * @throws IOException if this file store root directory is not
         *         a parent of the file directory.
         */
        private void ensureManagedFile(File file) throws IOException {
            if (file == null) {
                throw new IllegalArgumentException("file");
            }
            if (! file.getAbsolutePath().startsWith(
                                                this.root.getAbsolutePath())) {
                throw new FileNotFoundException("Invalid file path: " + file);
            }
        }
    }
}
