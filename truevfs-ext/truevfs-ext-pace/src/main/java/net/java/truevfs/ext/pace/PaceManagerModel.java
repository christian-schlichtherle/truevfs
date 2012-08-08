/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.HashMaps;
import static net.java.truevfs.ext.pace.PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE;
import static net.java.truevfs.ext.pace.PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE;
import net.java.truevfs.kernel.spec.*;

/**
 * The pace manager model.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class PaceManagerModel {

    private final Collection<FsController> evicted = new ConcurrentLinkedQueue<>();
    private final MountedControllerSet mounted = new MountedControllerSet();
    private volatile int maxMounted = MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE;
    private @Nullable volatile FsManager manager;

    void init(final FsManager manager) {
        assert !(manager instanceof PaceManagerController);
        if (null != this.manager) throw new IllegalStateException();
        this.manager = manager;
        mounted.sync();
    }

    int getFileSystemsMounted() {
        return mounted.sync();
    }

    int getMaximumFileSystemsMounted() {
        return maxMounted;
    }

    void setMaximumFileSystemsMounted(final int maxMounted) {
        if (maxMounted < MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE)
            throw new IllegalArgumentException();
        this.maxMounted = maxMounted;
    }

    /**
     * If the number of mounted archive files exceeds {@link #getMaximumFileSystemsMounted()},
     * then this method sync()s the least recently used (LRU) archive files
     * which exceed this value.
     *
     * @param controller the controller for the file system to retain mounted
     * for subsequent access.
     * @throws FsSyncException
     */
    void retain(final FsController controller)
    throws FsSyncException {
        final FsMountPoint mp = controller.getModel().getMountPoint();
        iterating:
        for (final Iterator<FsController> i = evicted.iterator(); i.hasNext();) {
            final FsController ec = i.next();
            final FsMountPoint emp = ec.getModel().getMountPoint();
            final FsManager fm = new FsFilteringManager(emp, manager);
            for (final FsController fc : fm) {
                final FsMountPoint fmp = fc.getModel().getMountPoint();
                if (mp.equals(fmp) || mounted.contains(fmp)) {
                    if (emp.equals(fmp) || mounted.contains(emp)) i.remove();
                    continue iterating;
                }
            }
            i.remove(); // even if subsequent umount fails
            fm.sync(FsSyncOptions.SYNC);
        }
    }

    /**
     * Registers the archive file system of the given controller as the most
     * recently used (MRU).
     *
     * @param controller the controller for the most recently used file system.
     */
    void accessed(final FsController controller) {
        if (controller.getModel().isMounted()) mounted.add(controller);
    }

    void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        evicted.clear();
        try {
            manager.sync(options);
        } finally {
            mounted.sync();
        }
    }

    @SuppressWarnings("serial")
    private final class MountedControllerSet {
        private final LinkedHashMap<FsMountPoint, FsController> map = new LinkedHashMap<FsMountPoint, FsController>(
                HashMaps.initialCapacity(getMaximumFileSystemsMounted() + 1),
                0.75f,
                true) {
            @Override
            public boolean removeEldestEntry(
                    final Map.Entry<FsMountPoint, FsController> entry) {
                final boolean evict = size() > getMaximumFileSystemsMounted();
                if (evict) {
                    final FsController c = entry.getValue();
                    final boolean added = evicted.add(c);
                    assert added;
                }
                return evict;
            }
        };
        private final ReadLock readLock;
        private final WriteLock writeLock;

        MountedControllerSet() {
            final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
            readLock = lock.readLock();
            writeLock = lock.writeLock();
        }

        boolean contains(FsMountPoint key) {
            readLock.lock();
            try {
                return map.containsKey(key);
            } finally {
                readLock.unlock();
            }
        }

        void add(final FsController controller) {
            final FsMountPoint mp = controller.getModel().getMountPoint();
            writeLock.lock();
            try {
                map.put(mp, controller);
            } finally {
                writeLock.unlock();
            }
        }

        int sync() {
            if (null == manager) return map.size();
            writeLock.lock();
            try {
                map.clear();
                for (final FsController c : manager)
                    if (c.getModel().isMounted())
                        map.put(c.getModel().getMountPoint(), c);
                return map.size();
            } finally {
                writeLock.unlock();
            }
        }
    } // MountedControllerSet
}
