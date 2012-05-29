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

package org.datalift.core.util;


import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Simple local in-memory cache using {@link java.util.concurrent}
 * package for high performance thread-safe design.
 * <p>
 * The original version of this code can be found at the
 * <a href="http://code.google.com/p/kitty-cache/">kitty-cache</a>
 * project on Google Code, published under the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>.</p>
 * <h3>Features:</h3>
 * <ul>
 *  <li>Super simple</li>
 *  <li>JSR 107 (javax.cache) like API</li>
 *  <li>Supports maximum cache size</li>
 *  <li>Supports item expiration</li>
 *  <li>About 3 times faster than Ehcache</li>
 * </ul>
 * <h3>Getting Started</h3>
 * <blockquote><pre>
 *  // Create a new cache, 5000 is max number of objects.
 *  SimpleCache<String,String> cache = new SimpleCache<String,String>(5000);
 *  // Put an object into the cache.
 *  cache.put("mykey", value, 500); // 500 is time to live in seconds
 *  // Get an object from the cache
 *  value = cache.get("mykey");
 * </pre></blockquote>
 *
 * @param  K   the type of keys maintained by this cache.
 * @param  V   the type of mapped values.
 *
 * @author treeder
 */
public class SimpleCache<K,V>
{
    /** The maximum number of cache entries allowed. */
    private final int maxSize;
    /** The default time-to-live of cache entries. */
    private final int defaultTtl;
    /** The actual data cache. */
    private final Map<K,Entry<K,V>> cache;
    /** Used to restrict the size of the cache map. */
    private final Queue<K> queue;
    /** Using this integer because ConcurrentLinkedQueue.size() is not
     * constant time. */
    private AtomicInteger size = new AtomicInteger();

    /**
     * Creates a cache with the specified maximum size and infinite
     * entry time-to-live.
     * @param  maxSize   the cache maximum size as a number of cached
     *                   entries; a value less than or equals to 0
     *                   indicates unlimited size.
     */
    public SimpleCache(int maxSize) {
        this(maxSize, -1);        
    }

    /**
     * Creates a cache with the specified maximum size and entry
     * time-to-live.
     * @param  maxSize      the cache maximum size as a number of cached
     *                      entries; a value less than or equals to 0
     *                      indicates unlimited size.
     * @param  defaultTtl   the default time-to-live of cache entries,
     *                      in seconds.
     */
    public SimpleCache(int maxSize, int defaultTtl) {
        super();
        this.maxSize = maxSize;
        this.defaultTtl = defaultTtl;
        this.cache = new ConcurrentHashMap<K,Entry<K,V>>();
        this.queue = new ConcurrentLinkedQueue<K>();
    }

    /**
     * Returns the value to which the specified key is mapped, or
     * <code>null</code> if this cache contains no mapping for the key
     * or if the entry expired.
     * @param  key   the key whose associated value is to be returned.
     *
     * @return the value to which the specified key is mapped, or
     *         <code>null</code> if this cache contains no mapping for
     *         the key or if the entry expired.
     */
    public V get(K key) {
        V v = null;
        if (this.cache.containsKey(key)) {
            Entry<K,V> e = this.cache.get(key);
            if (e.isExpired()) {
                v = e.getValue();
            }
            else {
                this.remove(key);
            }
        }
        return v;
    }

    /**
     * Returns whether this cache contains a mapping for the specified
     * key.
     * @param  key   key whose presence in this cache is to be tested.
     *
     * @return <code>true</code> if this cache contains a mapping for
     *         the specified key 
     */
    public boolean containsKey(K key) {
        this.get(key);                  // Get entry to remove it if expired.
        return this.cache.containsKey(key);
    }

    /**
     * Adds an entry to the cache with the default time-to-live.
     * @param  key   key with which the specified value is to be
     *               associated.
     * @param  v     value to be associated with the specified key.
     */
    public void put(K key, V v) {
        put0(key, v, this.defaultTtl);
    }

    /**
     * Adds an entry to the cache for the specified duration.
     * @param  key   key with which the specified value is to be
     *               associated.
     * @param  v     value to be associated with the specified key.
     * @param  ttl   the cache entry time to live, in seconds; a
     *               negative value means no expiry.
     */
    public void put(K key, V v, int ttl) {
        this.put0(key, v, (ttl < 0)? Long.MAX_VALUE:
                                     System.currentTimeMillis() + ttl);
    }

    /**
     * Adds an entry to the cache with the specified expiry date.
     * @param  key       key with which the specified value is to be
     *                   associated.
     * @param  v         value to be associated with the specified key.
     * @param  expires   the cache entry expiry date or
     *                   <code>null</code> for no expiry.
     */
    public void put(K key, V v, Date expires) {
        this.put0(key, v, (expires != null)? expires.getTime(): Long.MAX_VALUE);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     * <p>
     * This method returns a boolean to stay compatible with
     * <a href="http://ehcache.org/">Ehcache</a> and
     * <a href="http://memcached.org/">Memcached</a>.</p>
     * @param  key  key whose mapping is to be removed from the map.
     *
     * @return <code>true</code> if there was a mapping for key;
     *         <code>false</code> otherwise.
     *
     * @see    #getAndRemove for alternate version.
     */
    public boolean remove(K key) {
        return (this.getAndRemove(key) != null);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     * @param  key  key whose mapping is to be removed from the map.
     *
     * @return the previous value associated with <code>key</code>, or
     *         <code>null</code> if there was no mapping for key.
     */
    public V getAndRemove(K key) {
        return this.remove0(key, false);
    }

    /**
     * Returns the key-value mappings for the specified keys.
     * @param  keys   the keys whose associated values are to be
     *                returned.
     *
     * @return a map containing the key-value mappings. For each key,
     *         the value is sets to <code>null</code> if this cache
     *         contains no mapping for the key or if the entry expired.
     */
    public Map<K,V> getAll(Collection<? extends K> keys) {
        Map<K,V> m = new HashMap<K,V>();
        for (K key : keys) {
            m.put(key, this.get(key));
        }
        return m;
    }

    /**
     * Removes all of the mappings from this cache.
     */
    public void removeAll() {
        this.clear0(0);
    }

    /**
     * Removes entries for the specified keys.
     * @param  keys   the keys to remove.
     */
    public void removeAll(Collection<? extends K> keys) {
        for (K key : keys) {
            this.remove(key);
        }
    }

    /**
     * Returns the number of key-value mappings in this cache. If the
     * cache contains more than {@link Integer#MAX_VALUE} elements,
     * returns {@link Integer#MAX_VALUE}.
     * @return the number of key-value mappings in this cache.
     */
    public int size() {
        return this.size.get();
    }

    private V put0(K key, V v, long expires) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        // Add entry to cache.
        Entry<K,V> oldEntry = this.cache.put(key,
                                             new Entry<K,V>(expires, key, v));
        this.queue.add(key);
        this.size.incrementAndGet();
        // Check cache size and purge oldest entries if needed.
        if (this.maxSize > 0) {
            this.clear0(this.maxSize);
        }
        return (oldEntry != null)? oldEntry.getValue(): null;
    }

    private V remove0(K key, boolean decrementSizeAnyway) {
        V v = null;
        Entry<K,V> e = this.cache.remove(key);
        if (e != null) {
            v = e.getValue();
        }
        if ((e != null) || (decrementSizeAnyway)) {
            this.size.decrementAndGet();
        }
        return v;
    }

    private void clear0(int until) {
        while (this.size() > until) {
            K toRemove = this.queue.poll();
            if (toRemove != null) {
                this.remove0(toRemove, true);
            }
            else break;
        }
    }

    /**
     * A cache entry.
     */
    protected final static class Entry<K,V>
    {
        private final long expires;
        private final K key;
        private final V value;

        /**
         * Creates a new cache entry.
         * @param expires   the entry expiry time.
         * @param key       the cache key.
         * @param value     the cached value.
         */
        public Entry(long expires, K key, V value) {
            this.expires = expires;
            this.key     = key;
            this.value   = value;
        }

        /**
         * Returns the key corresponding to this entry.
         * @return the key corresponding to this entry.
         */
        public K getKey() {
            return this.key;
        }

        /**
         * Returns the value stored in the cache when this entry
         * was created.
         * @return the value corresponding to this entry.
         */
        public V getValue() {
            return this.value;
        }

        private boolean isExpired() {
            return (this.expires - System.currentTimeMillis() <= 0L);
        }
    }
}
