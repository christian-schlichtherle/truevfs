/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.ext.throttle;

import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.HashMaps;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ThrottleManager
extends FsDecoratingManager<FsManager> implements ThrottleManagerMXBean {

    private static final Logger
            logger = Logger.getLogger(  ThrottleManager.class.getName(),
                                        ThrottleManager.class.getName());

    private volatile int maxMounts;

    private final Queue<ThrottleController> lru
            = new ConcurrentLinkedQueue<ThrottleController>();

    @SuppressWarnings("serial")
    private final Map<FsMountPoint, ThrottleController> mru;

    ThrottleManager(FsManager manager) {
        super(manager);
        setMaximumOfMostRecentlyUsedArchiveFiles(Integer.parseInt(System.getProperty(
                ThrottleManager.class.getName() + ".maxMounts",
                Integer.toString(DEFAULT_MAXIMUM_OF_MOST_RECENTLY_USED_ARCHIVE_FILES))));
        // Requires initialized maxMounts!
        mru = Collections.synchronizedMap(new MruControllerMap());
    }

    @Override
    public FsController<?> getController(
            final FsMountPoint mountPoint,
            final FsCompositeDriver driver) {
        final FsController<?> controller
                = delegate.getController(mountPoint, driver);
        return null != controller.getParent()
                ? new ThrottleController(this, controller)
                : controller;
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
    ThrottleManager accessMru(final ThrottleController controller) {
        final FsMountPoint mp = controller.getMountPoint();
        logger.log(Level.FINEST, "accessMru", mp);
        mru.put(mp, controller);
        return this;
    }

    /**
     * If the number of mounted archive files exceeds {@link #getMaximumOfMostRecentlyUsedArchiveFiles()},
     * then this method syncs the least recently used (LRU) archive files
     * which exceed this value.
     * 
     * @throws FsSyncException 
     */
    void syncLru() throws FsSyncException {
        iterating: for (final Iterator<ThrottleController> i = lru.iterator(); i.hasNext(); ) {
            final ThrottleController c = i.next();
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
            }
            i.remove(); // even if subsequent umount fails
            logger.log(Level.FINE, "syncLru", mp);
            fm.sync(FsSyncOptions.SYNC);
        }
    }

    @Override
    public void sync() throws FsSyncException {
        sync(FsSyncOptions.NONE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super IOException, X> handler)
    throws X {
        logger.log(Level.FINER, "clearLruSize", getNumberOfLeastRecentlyUsedArchiveFiles());
        lru.clear();
        logger.log(Level.FINER, "clearMruSize", getNumberOfMostRecentlyUsedArchiveFiles());
        mru.clear();
        try {
            delegate.sync(options, handler);
        } catch (final IOException ex) {
            // Rebuild the MRU cache and pass on the exception.
            for (final FsController<?> c : delegate) {
                final FsMountPoint mp = c.getModel().getMountPoint();
                mru.put(mp, new ThrottleController(this, c));
            }
            throw (X) ex;
        }
    }

    @SuppressWarnings("serial")
    private final class MruControllerMap
    extends LinkedHashMap<FsMountPoint, ThrottleController> {

        MruControllerMap() {
            super(HashMaps.initialCapacity(getMaximumOfMostRecentlyUsedArchiveFiles() + 1), 0.75f, true);
        }

        @Override
        public boolean removeEldestEntry(
                final Map.Entry<FsMountPoint, ThrottleController> entry) {
            final boolean evict = size() > getMaximumOfMostRecentlyUsedArchiveFiles();
            if (evict) {
                final ThrottleController c = entry.getValue();
                final boolean added = lru.add(c);
                assert added;
            }
            return evict;
        }
    } // MruControllerMap
}
