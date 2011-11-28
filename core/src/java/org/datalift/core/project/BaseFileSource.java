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

package org.datalift.core.project;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.annotation.RdfProperty;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.FileSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.util.StringUtils;

import static org.datalift.fwk.util.Env.*;


/**
 * An abstract superclass for implementations of the {@link FileSource}
 * interface.
 *
 * @author hdevos
 */
@MappedSuperclass
public abstract class BaseFileSource<T> extends BaseSource
                                        implements FileSource<T>
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("dc:format")
    private String mimeType;
    @RdfProperty("datalift:path")
    private String filePath;

    private transient File storage = null;
    private transient int bufferSize = getFileBufferSize();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new source of the specified type.
     * @param  type   the {@link SourceType source type}.
     *
     * @throws IllegalArgumentException if <code>type</code> is
     *         <code>null</code>.
     */
    protected BaseFileSource(SourceType type) {
        super(type);
    }

    /**
     * Creates a new source of the specified type, identifier and
     * owning project.
     * @param  type      the {@link SourceType source type}.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if any of <code>type</code>,
     *         <code>uri</code> or <code>project</code> is
     *         <code>null</code>.
     */
    protected BaseFileSource(SourceType type, String uri, Project project) {
        super(type, uri, project);
        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException("uri");
        }
    }

    //-------------------------------------------------------------------------
    // Source contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void delete() {
        super.delete();

        this.init();
        this.storage.delete();
    }

    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final String getMimeType() {
        return this.mimeType;
    }

    /** {@inheritDoc} */
    @Override
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /** {@inheritDoc} */
    @Override
    public final String getFilePath() {
        return this.filePath;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream() throws IOException {
        this.init();
        return new BufferedInputStream(new FileInputStream(this.storage),
                                       this.getBufferSize());
    }

    private void init() {
        if (this.storage == null) {
            File docRoot = Configuration.getDefault().getPublicStorage();
            if ((docRoot == null) || (! docRoot.isDirectory())) {
                throw new TechnicalException("public.storage.not.directory",
                                             docRoot);
            }
            File f = new File(docRoot, this.filePath);
            if (! (f.isFile() && f.canRead())) {
                throw new TechnicalException("file.not.found", this.filePath);
            }
            this.storage = f;
        }
        // Else: Already initialized.
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Sets the path (relative to the DataLift public storage
     * directory) of the file containing the source data.
     * @param  path   the data file relative path.
     */
    public void setFilePath(String path) {
        this.filePath = path;
    }

    /**
     * Returns the size of the file input buffer for this source.
     * @return the size of the file input buffer.
     */
    public int getBufferSize() {
        return this.bufferSize;
    }

    /**
     * Sets the size of the file input buffer for this source.
     * @param  size   the requested size for the file input buffer,
     *                as a number of bytes.
     */
    public void setBufferSize(int size) {
        this.bufferSize = (size < MIN_FILE_BUFFER_SIZE)?
                                                MIN_FILE_BUFFER_SIZE: size;
    }
}
