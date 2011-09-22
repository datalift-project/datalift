package org.datalift.core.project;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.annotation.RdfProperty;

import org.datalift.fwk.project.CachingSource;


@MappedSuperclass
public abstract class CachingSourceImpl extends BaseSource
                                        implements CachingSource
{
    @RdfProperty("datalift:cacheDuration")
    private int cacheDuration;
    
    protected CachingSourceImpl(SourceType type) {
            super(type, null);
    }
    
    protected CachingSourceImpl(SourceType type, String uri) {
        super(type, uri);
    }
    
    /** {@inheritDoc} */
    public int getCacheDuration() {
        return cacheDuration;
    }

    /** {@inheritDoc} */
    public void setCacheDuration(int cacheDuration) {
        this.cacheDuration = cacheDuration;
    }
    
    /**
     * Get the data of the database form the cache file, if it has
     * been populated.
     * @param  cacheFile   the cache file path.
     *
     * @return an input stream on the cache file, if it exists.
     * @throws IOException if any error occurred accessing the cache
     *         file.
     */
    protected InputStream getCacheStream(File cacheFile) throws IOException {
        if (this.cacheDuration > 0) {
            long now = System.currentTimeMillis();
            if ((cacheFile.exists()) &&
                (cacheFile.lastModified() < (now + this.cacheDuration))) {
                return new FileInputStream(cacheFile);
            }
        }
        return null;
    }
}
