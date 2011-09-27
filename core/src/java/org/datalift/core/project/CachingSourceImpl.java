package org.datalift.core.project;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

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

    private transient Configuration configuration = null;
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
    public void init(Configuration configuration, URI baseUri)
                                                            throws IOException {
        super.init(configuration, baseUri);

        this.configuration = configuration;
    }

    /** {@inheritDoc} */
    @Override
    public void delete() {
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
        if (this.configuration == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (this.cacheFile == null) {
            String fileName = this.getClass().getSimpleName() + '-' +
                                        StringUtils.urlify(this.getTitle());
            this.cacheFile = new File(configuration.getPrivateStorage(),
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
     * Get the data of the database form the cache file, if it has
     * been populated.
     * @param  checkValid   whether to check for cache validity.
     *
     * @return an input stream on the cache file or <code>null</code>
     *         if a validity check was done and the cache is missing
     *         or is no longer valid.
     * @throws IOException if any error occurred accessing the cache
     *         file.
     */
    protected InputStream getInputStream(boolean checkValid)
                                                            throws IOException {
        return ((! checkValid) || (this.isCacheValid()))?
                                new FileInputStream(this.getCacheFile()): null;
    }
}
