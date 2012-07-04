/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.throttle;

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
final class ThrottleManager extends FsDecoratingManager<FsManager> {

    private static final Logger
            logger = Logger.getLogger(  ThrottleManager.class.getName(),
                                        ThrottleManager.class.getName());

    /**
     * The minimum value for the maximum number of mounted archive file systems,
     * which is {@value}.
     */
    public static final int MIN_MAX_MOUNTS = 2;

    /**
     * The default value for the maximum number of mounted archive file systems,
     * which is {@value}.
     */
    public static final int DEFAULT_MAX_MOUNTS = MIN_MAX_MOUNTS;

    private volatile int maxMounts;

    private final Queue<ThrottleController> lru
            = new ConcurrentLinkedQueue<ThrottleController>();

    @SuppressWarnings("serial")
    private final Map<FsMountPoint, ThrottleController> mru;

    ThrottleManager(FsManager manager) {
        super(manager);
        setMaxMounts(Integer.parseInt(System.getProperty(
                ThrottleManager.class.getName() + ".maxMounts",
                Integer.toString(DEFAULT_MAX_MOUNTS))));
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

    /**
     * Returns the maximum number of archive files which may be mounted at any
     * time.
     * The mimimum value is one.
     * 
     * @return The maximum number of archive files which may be mounted at any
     *         time.
     */
    public int getMaxMounts() {
        return maxMounts;
    }

    /**
     * Sets the maximum number of archive files which may be mounted at any
     * time.
     * 
     * @param  maxMounts the maximum number of mounted archive files.
     * @throws IllegalArgumentException if {@code maxMounts} is less than
     *         {@link #MIN_MAX_MOUNTS}.
     */
    public void setMaxMounts(final int maxMounts) {
        if (maxMounts < MIN_MAX_MOUNTS) throw new IllegalArgumentException();
        this.maxMounts = maxMounts;
    }

    public int getMruSize() {
        return mru.size();
    }

    public int getLruSize() {
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
     * If the number of mounted archive files exceeds {@link #getMaxMounts()},
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
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super IOException, X> handler)
    throws X {
        logger.log(Level.FINER, "clearMruSize", getMruSize());
        mru.clear();
        logger.log(Level.FINER, "clearLruSize", getLruSize());
        lru.clear();
        delegate.sync(options, handler);
    }

    @SuppressWarnings("serial")
    private final class MruControllerMap
    extends LinkedHashMap<FsMountPoint, ThrottleController> {

        MruControllerMap() {
            super(HashMaps.initialCapacity(getMaxMounts() + 1), 0.75f, true);
        }

        @Override
        public boolean removeEldestEntry(
                final Map.Entry<FsMountPoint, ThrottleController> entry) {
            final boolean evict = size() > getMaxMounts();
            if (evict) {
                final ThrottleController c = entry.getValue();
                final boolean added = lru.add(c);
                assert added;
            }
            return evict;
        }
    } // MruControllerMap
}
