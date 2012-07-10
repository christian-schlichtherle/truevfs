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
    public FsController<?> getController(
            final FsMountPoint mp,
            final FsCompositeDriver d) {
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
     * @param  controller the throttle controller for the most recently used
     *         archive file system.
     * @return {@code this}
     */
    void accessedMru(final PaceController controller) {
        if (controller.isTouched()) {
            final FsMountPoint mp = controller.getMountPoint();
            mru.put(mp, controller);
            logger.log(Level.FINEST, "accessedMru", mp);
        }
    }

    /**
     * If the number of mounted archive files exceeds {@link #getMaximumOfMostRecentlyUsedArchiveFiles()},
     * then this method syncs the least recently used (LRU) archive files
     * which exceed this value.
     * 
     * @throws FsSyncException 
     */
    void syncLru(final PaceController retain) throws FsSyncException {
        final FsMountPoint rmp = retain.getMountPoint();
        iterating: for (final Iterator<PaceController> i = lru.iterator(); i.hasNext(); ) {
            final PaceController c = i.next();
            final FsMountPoint mp = c.getMountPoint();
            final FsManager fm = new FsFilteringManager(delegate, mp);
            // Make sure not to umount a parent of a MRU controller because
            // this would umount the MRU controller, too, which might
            // result in excessive remounting.
            for (final FsController<?> fc : fm) {
                final FsMountPoint fmp = fc.getModel().getMountPoint();
                if (mru.containsKey(fmp)) {
                    if (fmp.equals(mp)) i.remove(); // evicted, then accessed again
                    continue iterating;
                }
                if (fmp.equals(rmp)) {
                    // The theory is that another thread might have just
                    // concurrently evicted the controller to retain for
                    // subsequent access.
                    // I assume this could only happen if there is heavy
                    // contention caused by many threads - but I have no test
                    // case to cover this.
                    i.remove();
                    mru.put(fmp, retain); // recover
                    continue iterating;
                }
            }
            i.remove(); // even if subsequent umount fails
            fm.sync(FsSyncOptions.SYNC);
            logger.log(Level.FINE, "syncedLru", mp);
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
        logger.log(Level.FINER, "clearedLruSize", getNumberOfLeastRecentlyUsedArchiveFiles());
        mru.clear();
        logger.log(Level.FINER, "clearedMruSize", getNumberOfMostRecentlyUsedArchiveFiles());
        delegate.sync(options);
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

        void clear() {
            writeLock.lock();
            try {
                map.clear();
            } finally {
                writeLock.unlock();
            }
        }
    } // MruControllerMap
}
