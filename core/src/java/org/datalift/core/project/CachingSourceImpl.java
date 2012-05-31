package org.datalift.core.project;


import java.io.BufferedInputStream;
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
import org.datalift.fwk.project.Source;

import static org.datalift.fwk.util.StringUtils.urlify;
import static org.datalift.fwk.util.Env.*;


/**
 * An abstract superclass for implementations of the {@link Source}
 * interface supporting caching of the source data on the local
 * file system.
 *
 * @author lbihanic
 */
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

    /** The name of the directory where to store temporary files. */
    private final static String CACHE_DIRECTORY_NAME    = "source-data";

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

        this.invalidateCache();
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
     * Returns the size of the cache input buffer for this source.
     * @return the size of the cache input buffer.
     */
    public int getBufferSize() {
        return this.bufferSize;
    }

    /**
     * Sets the size of the file cache buffer for this source.
     * @param  size   the requested size for the cache input buffer,
     *                as a number of bytes.
     */
    public void setBufferSize(int size) {
        this.bufferSize = (size < MIN_FILE_BUFFER_SIZE)?
                                                MIN_FILE_BUFFER_SIZE: size;
    }

    /**
     * Returns the cache file this source shall use to store temporary
     * data. The file may not exist depending on whether data have been
     * loaded.
     * @return the cache file.
     */
    protected File getCacheFile() {
        if (this.cacheFile == null) {
            String ext = this.getCacheFileExtension();
            ext = (ext != null)? "." + ext: "";
            String filePath = CACHE_DIRECTORY_NAME
                                + File.separatorChar
                                + DefaultProjectManager.PROJECT_DIRECTORY_NAME
                                + File.separatorChar
                                + urlify(this.getProject().getTitle())
                                + File.separatorChar
                                + urlify(this.getTitle()) + "-cache" + ext;
            File f = new File(Configuration.getDefault().getTempStorage(),
                              filePath);
            // Make sure parent directories exist.
            f.getParentFile().mkdirs();
            log.trace("Cache file for \"{}\": {}", this.getTitle(), f);
            // Don't mark cache file for deletion. Let's reuse it!
            // f.deleteOnExit();
            this.cacheFile = f;
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
     * Invalidates the local cache to force data reload.
     */
    protected void invalidateCache() {
        File f = this.getCacheFile();
        if (f.exists()) {
            f.delete();
        }
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
        return new BufferedInputStream(new FileInputStream(this.getCacheFile()),
                                       this.getBufferSize());
    }

    /**
     * Loads or reloads source remote data in the local cache file.
     * @throws IOException if any error occurred accessing the local
     *         cache or saving the data into it.
     */
    abstract protected void reloadCache() throws IOException;

    /**
     * Returns the file name extension for the cache file.
     * <p>
     * The default implementation returns <code>tmp</code>.</p>
     * @return the file name extension for the cache file.
     */
    protected String getCacheFileExtension() {
        return "tmp";
    }

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
