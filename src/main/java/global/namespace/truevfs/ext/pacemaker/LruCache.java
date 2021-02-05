/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static global.namespace.truevfs.commons.shed.HashMaps.initialCapacity;

/**
 * A simple cache set with a least-recently-used (LRU) eviction strategy.
 * Note that unlike other caches whenever an item gets evicted, it gets added to a concurrent set which can get queried
 * using the {@link #evicted} method for further processing, e.g. to close resources.
 * <p>
 * This class is thread-safe.
 *
 * @param <T> the type of the items
 * @author Christian Schlichtherle
 */
final class LruCache<T> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final ConcurrentMap<T, Boolean> evicted = new ConcurrentHashMap<>();
    private final CacheMap cached = new CacheMap();

    private volatile int maximumSize;

    LruCache(final int initialMaximumSize) {
        this.maximumSize = initialMaximumSize;
    }

    Set<T> getEvictedView() { return evicted.keySet(); }

    int getMaximumSize() { return maximumSize; }

    void setMaximumSize(final int maximumSize) {
        if (0 > maximumSize) {
            throw new IllegalArgumentException();
        }
        this.maximumSize = maximumSize;
    }

    void add(T item) {
        writeLocked(() -> cached.put(item, true));
    }

    void remove(T item) {
        writeLocked(() -> cached.remove(item));
    }

    /**
     * Records access to the given mount point.
     * This method has no effect if the given mount point is not present in this LRU cache.
     */
    void recordAccess(T item) {
        // The lookup needs to be write-locked because the access-ordered cache map may get structurally modified as a
        // side effect:
        writeLocked(() -> cached.get(item));
    }

    boolean exists(Predicate<T> predicate) {
        return readLocked(() -> cached.exists(predicate));
    }

    private <U> U readLocked(Supplier<U> expr) {
        return locked(readLock, expr);
    }

    private <U> U writeLocked(Supplier<U> expr) {
        return locked(writeLock, expr);
    }

    private static <U> U locked(final Lock lock, final Supplier<U> expr) {
        lock.lock();
        try {
            return expr.get();
        } finally {
            lock.unlock();
        }
    }

    private final class CacheMap extends LinkedHashMap<T, Boolean> {

        private static final long serialVersionUID = 0;

        CacheMap() {
            super(initialCapacity(maximumSize), 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<T, Boolean> eldest) {
            if (size() > maximumSize) {
                evicted.put(eldest.getKey(), eldest.getValue());
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Boolean put(final T key, final Boolean value) {
            evicted.remove(key);
            return super.put(key, value);
        }

        @Override
        public Boolean remove(final Object key) {
            evicted.remove(key);
            return super.remove(key);
        }

        boolean exists(Predicate<T> predicate) {
            return keySet().stream().anyMatch(predicate);
        }
    }
}
