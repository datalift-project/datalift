package org.datalift.core.project;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.annotation.RdfProperty;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CachingSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.util.StringUtils;


@MappedSuperclass
public abstract class CachingSourceImpl extends BaseSource
                                        implements CachingSource
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static long MINUTES_TO_MILLIS = 60 * 1000L;
    private final static long HOURS_TO_MILLIS   = 60 * MINUTES_TO_MILLIS;

    private final static long MIN_CACHE_DURATION = 3 * MINUTES_TO_MILLIS;

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

    /**
     * Creates a new source of the specified type.
     * @param  type   the {@link SourceType source type}.
     *
     * @throws IllegalArgumentException if <code>type</code> is
     *         <code>null</code>.
     */
    protected CachingSourceImpl(SourceType type) {
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
     * @throws IllegalArgumentException if <code>type</code> is
     *         <code>null</code> or if <code>uri</code> is specified
     *         but <code>project</code> is <code>null</code>.
     */
    protected CachingSourceImpl(SourceType type, String uri, Project project) {
        super(type, uri, project);
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
    public void setCacheDuration(int durationInHours) {
        this.cacheDuration = durationInHours;
    }

    /** {@inheritDoc} */
    @Override
    public Date getLastCacheUpdate() {
        File f = this.getCacheFile();
        return (f.exists())? new Date(f.lastModified()): null;
    }

    /** {@inheritDoc} */
    @Override
    public Date getCacheExpiryDate() {
        long expiry = this.getCacheExpiry();
        return (expiry > 0L)? new Date(expiry): null;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns the cache file this source shall use to store temporary
     * data. The file may not exist depending on whether data have been
     * loaded.
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
            // Don't mark cache file for deletion. Let's reuse it!
            // this.cacheFile.deleteOnExit();
        }
        return this.cacheFile;
    }

    protected long getCacheDurationMillis() {
        int durationInHours = this.getCacheDuration();
        return (durationInHours > 0)? durationInHours * HOURS_TO_MILLIS:
                                      MIN_CACHE_DURATION;
    }

    protected boolean isCacheValid() {
        return (this.getCacheExpiry() > System.currentTimeMillis());
    }

    /**
     * Get an input stream on the local data cache file, populating it
     * if needed.
     * @return an input stream on the cache file.
     * @throws IOException if any error occurred accessing the local
     *         cache or saving the data into it.
     */
    protected InputStream getInputStream() throws IOException {
        if (! this.isCacheValid()) {
            this.reloadCache();
        }
        return new FileInputStream(this.getCacheFile());
    }

    /**
     * Loads or reloads source remote data in the local cache file.
     * @throws IOException if any error occurred accessing the local
     *         cache or saving the data into it.
     */
    abstract protected void reloadCache() throws IOException;

    /**
     * Return the local cached data expiry date as a number of
     * milliseconds since midnight, January 1, 1970 UTC.
     * @return the local cached data expiry date in milliseconds.
     */
    private long getCacheExpiry() {
        File f = this.getCacheFile();
        return ((f.exists()) && (this.getCacheDuration() > 0))?
                        f.lastModified() + this.getCacheDurationMillis(): -1L;
    }
}
