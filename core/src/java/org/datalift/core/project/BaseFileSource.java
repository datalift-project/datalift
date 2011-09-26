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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.annotation.RdfProperty;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.FileSource;


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
    @RdfProperty("dc:format")
    private String mimeType;
    @RdfProperty("datalift:path")
    private String filePath;

    private transient File storage = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new source of the specified type.
     * @param  type   the {@link SourceType source type}.
     */
    protected BaseFileSource(SourceType type) {
        super(type);
    }

    /**
     * Creates a new source of the specified type and identifier.
     * @param  type   the {@link SourceType source type}.
     * @param  uri    the source unique identifier (URI) or
     *                <code>null</code> if not known at this stage.
     */
    protected BaseFileSource(SourceType type, String uri) {
        super(type, uri);
    }

    //-------------------------------------------------------------------------
    // BaseSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration, URI baseUri)
                                                            throws IOException {
        super.init(configuration, baseUri);

        if (this.storage == null) {
            File docRoot = configuration.getPublicStorage();
            if ((docRoot == null) || (! docRoot.isDirectory())) {
                throw new TechnicalException("public.storage.not.directory",
                                             docRoot);
            }
            File f = new File(docRoot, this.filePath);
            if (! (f.isFile() && f.canRead())) {
                throw new FileNotFoundException(this.filePath);
            }
            this.storage = f;
        }
        // Else: Already initialized.
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
        if (this.storage == null) {
            throw new IllegalStateException("Not initialized");
        }
        return new FileInputStream(this.storage);
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
}
