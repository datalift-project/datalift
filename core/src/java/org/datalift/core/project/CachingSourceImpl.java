package org.datalift.core.project;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.annotation.RdfProperty;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CachingSource;
import org.datalift.fwk.util.StringUtils;


@MappedSuperclass
public abstract class CachingSourceImpl extends BaseSource
                                        implements CachingSource
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:cacheDuration")
    private int cacheDuration;

    private transient File cacheFile = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    protected CachingSourceImpl(SourceType type) {
            super(type, null);
    }

    protected CachingSourceImpl(SourceType type, String uri) {
        super(type, uri);
    }

    //-------------------------------------------------------------------------
    // Source contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void delete() {
        super.delete();

        if (this.cacheFile != null) {
            this.cacheFile.delete();
        }
    }

    //-------------------------------------------------------------------------
    // CachingSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public int getCacheDuration() {
        return cacheDuration;
    }

    /** {@inheritDoc} */
    @Override
    public void setCacheDuration(int cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns the cache file this source shall use to store temporary
     * data.
     * @param configuration   the DataLift configuration.
     *
     * @return the cache file.
     */
    protected File getCacheFile() {
        if (this.cacheFile == null) {
            String fileName = this.getClass().getSimpleName() + '-' +
                                        StringUtils.urlify(this.getTitle());
            this.cacheFile = new File(
                                Configuration.getDefault().getPrivateStorage(),
                                fileName);
            log.debug("Created cache file: {}", this.cacheFile);
            cacheFile.deleteOnExit();
        }
        return this.cacheFile;
    }

    protected boolean isCacheValid() {
        boolean cacheValid = false;

        File f = this.getCacheFile();
        if (this.cacheDuration > 0) {
            long now = System.currentTimeMillis();
            cacheValid = ((f.exists()) &&
                          (f.lastModified() < (now + this.cacheDuration)));
        }
        return cacheValid;
    }

    /**
     * Get an input stream on the local data cache file, populating it
     * if needed.
     *
     * @return an input stream on the cache file.
     * @throws IOException if any error occurred accessing the cache
     *         file.
     */
    protected InputStream getInputStream() throws IOException {
        if (! this.isCacheValid()) {
            this.reloadCache();
        }
        return new FileInputStream(this.getCacheFile());
    }

    abstract protected void reloadCache() throws IOException;
}
