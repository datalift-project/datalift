package org.datalift.projectmanager;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.annotation.RdfProperty;

import org.datalift.fwk.project.FileSource;


@MappedSuperclass
public abstract class BaseFileSource<T> extends BaseSource
                                        implements FileSource<T>
{
    @RdfProperty("datalift:mimeType")
    private String mimeType;
    @RdfProperty("datalift:path")
    private String filePath;

    private transient File storage;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    protected BaseFileSource() {
        super();
    }

    protected BaseFileSource(String uri) {
        super(uri);
    }

    //-------------------------------------------------------------------------
    // BaseSource contract support
    //-------------------------------------------------------------------------

    @Override
    public void init(File docRoot, URI baseUri) throws IOException {
        if (docRoot == null) {
            throw new IllegalArgumentException("docRoot");
        }
        File f = new File(docRoot, this.filePath);
        if (! (f.isFile() && f.canRead())) {
            throw new FileNotFoundException(this.filePath);
        }
        this.storage = f;
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
    public final String getFilePath() {
        return this.filePath;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream() throws IOException {
        if (this.storage == null) {
            throw new IllegalStateException();
        }
    	return new FileInputStream(this.storage);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFilePath(String path) {
        this.filePath = path;
    }
}
