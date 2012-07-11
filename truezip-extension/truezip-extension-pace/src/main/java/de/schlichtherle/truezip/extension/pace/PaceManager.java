/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.pace;

import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.HashMaps;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class PaceManager
extends FsDecoratingManager<FsManager> implements PaceManagerMXBean {

    private static final Logger
            logger = Logger.getLogger(  PaceManager.class.getName(),
                                        PaceManager.class.getName());

    private volatile int
            maxMounts = DEFAULT_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES;

    private final Collection<PaceController> lru
            = new ConcurrentLinkedQueue<PaceController>();

    @SuppressWarnings("serial")
    private final MruControllerMap mru = new MruControllerMap();

    public PaceManager(final FsManager manager) {
        super(manager);
    }

    @Override
    public FsController<?> getController(FsMountPoint mp, FsCompositeDriver d) {
        return new PaceController(this, delegate.getController(mp, d));
    }

    @Override
    public int getNumberOfManagedArchiveFiles() {
        return delegate.getSize();
    }

    @Override
    public int getMaximumOfMostRecentlyUsedArchiveFiles() {
        return maxMounts;
    }

    @Override
    public void setMaximumOfMostRecentlyUsedArchiveFiles(final int maxMounts) {
        if (maxMounts < MINIMUM_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES)
            throw new IllegalArgumentException();
        this.maxMounts = maxMounts;
    }

    @Override
    public int getNumberOfMostRecentlyUsedArchiveFiles() {
        return mru.size();
    }

    @Override
    public int getNumberOfLeastRecentlyUsedArchiveFiles() {
        return lru.size();
    }

    /**
     * Registers the archive file system of the given controller as the most
     * recently used (MRU).
     * 
     * @param  c the controller for the most recently used file system.
     */
    void accessedMru(final PaceController c) {
        if (c.isTouched()) {
            final FsMountPoint mp = c.getMountPoint();
            mru.put(mp, c);
            logger.log(Level.FINEST, "accessed", mp);
        }
    }

    /**
     * If the number of mounted archive files exceeds {@link #getMaximumOfMostRecentlyUsedArchiveFiles()},
     * then this method sync()s the least recently used (LRU) archive files
     * which exceed this value.
     * 
     * @param  c the controller for the file system to retain mounted for
     *         subsequent access.
     * @throws FsSyncException 
     */
    void syncLru(final PaceController c) throws FsSyncException {
        final FsMountPoint mp = c.getMountPoint();
        iterating: for (final Iterator<PaceController> i = lru.iterator(); i.hasNext(); ) {
            final PaceController lc = i.next();
            final FsMountPoint lmp = lc.getMountPoint();
            final FsManager fm = new FsFilteringManager(delegate, lmp);
            for (final FsController<?> fc : fm) {
                final FsMountPoint fmp = fc.getModel().getMountPoint();
                if (mp.equals(fmp) || mru.containsKey(fmp)) {
                    if (lmp.equals(fmp) || mru.containsKey(lmp)) {
                        i.remove();
                        logger.log(Level.FINER, "recollected", lmp);
                    } else {
                        logger.log(Level.FINER, "retained", lmp);
                    }
                    continue iterating;
                }
            }
            i.remove(); // even if subsequent umount fails
            fm.sync(FsSyncOptions.SYNC);
            logger.log(Level.FINE, "synced", lmp);
        }
    }

    @Override
    public void sync() throws FsSyncException {
        sync(FsSyncOptions.NONE);
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        lru.clear();
        try {
            delegate.sync(options);
        } finally {
            logger.log(Level.FINER, "cleared", mru.clear());
        }
    }

    @SuppressWarnings("serial")
    private final class MruControllerMap {
        private final LinkedHashMap<FsMountPoint, PaceController> map
                = new LinkedHashMap<FsMountPoint, PaceController>(
                    HashMaps.initialCapacity(getMaximumOfMostRecentlyUsedArchiveFiles() + 1),
                    0.75f,
                    true) {
            @Override
            public boolean removeEldestEntry(
                    final Map.Entry<FsMountPoint, PaceController> entry) {
                final boolean evict
                        = size() > getMaximumOfMostRecentlyUsedArchiveFiles();
                if (evict) {
                    final PaceController c = entry.getValue();
                    final boolean added = lru.add(c);
                    assert added;
                    logger.log(Level.FINER, "evicted", entry.getKey());
                }
                return evict;
            }
        };

        private final ReadLock readLock;
        private final WriteLock writeLock;

        MruControllerMap() {
            final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
            readLock = lock.readLock();
            writeLock = lock.writeLock();
        }

        int size() {
            readLock.lock();
            try {
                return map.size();
            } finally {
                readLock.unlock();
            }
        }

        boolean containsKey(FsMountPoint key) {
            readLock.lock();
            try {
                return map.containsKey(key);
            } finally {
                readLock.unlock();
            }
        }

        PaceController put(FsMountPoint key, PaceController value) {
            writeLock.lock();
            try {
                return map.put(key, value);
            } finally {
                writeLock.unlock();
            }
        }

        int clear() {
            writeLock.lock();
            try {
                int c = 0;
                for (final Iterator<PaceController> i = map.values().iterator(); i.hasNext(); ) {
                    if (!i.next().isTouched()) {
                        i.remove();
                        c++;
                    }
                }
                return c;
            } finally {
                writeLock.unlock();
            }
        }
    } // MruControllerMap
}
