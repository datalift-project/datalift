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

    private final static long HOURS_TO_MILLIS = 3600L * 1000L;

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
            // Don't mark cache file for deletion. Let's reuse it!
            // this.cacheFile.deleteOnExit();
        }
        return this.cacheFile;
    }

    protected boolean isCacheValid() {
        File f = this.getCacheFile();

        boolean cacheValid = f.exists();
        if ((cacheValid) && (this.cacheDuration > 0)) {
            long oldestUpdate = System.currentTimeMillis() -
                                        (this.cacheDuration * HOURS_TO_MILLIS);
            cacheValid = (f.lastModified() > oldestUpdate);
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
