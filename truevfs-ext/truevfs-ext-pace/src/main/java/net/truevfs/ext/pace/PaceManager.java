/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.pace;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.spec.*;
import net.truevfs.kernel.spec.util.BitField;
import net.truevfs.kernel.spec.util.HashMaps;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class PaceManager
extends FsDecoratingManager<FsManager> implements PaceManagerMXBean {

    private volatile int
            maxMounted = MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE;

    private final Collection<PaceController> evicted
            = new ConcurrentLinkedQueue<>();

    @SuppressWarnings("serial")
    private final MountedFileSystemMap mounted = new MountedFileSystemMap();

    public PaceManager(final FsManager manager) {
        super(manager);
    }

    @Override
    public FsController<?> controller(FsCompositeDriver d, FsMountPoint mp) {
        return new PaceController(this, manager.controller(d, mp));
    }

    @Override
    public int getFileSystemsTotal() {
        return manager.size();
    }

    @Override
    public int getFileSystemsMounted() {
        return mounted.size();
    }

    @Override
    public int getMaximumFileSystemsMounted() {
        return maxMounted;
    }

    @Override
    public void setMaximumFileSystemsMounted(final int maxMounted) {
        if (maxMounted < MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE)
            throw new IllegalArgumentException();
        this.maxMounted = maxMounted;
    }

    @Override
    public int getTopLevelArchiveFileSystemsTotal() {
        int total = 0;
        for (FsController<?> controller : manager)
            if (isTopLevelArchive(controller)) total++;
        return total;
    }

    @Override
    public int getTopLevelArchiveFileSystemsMounted() {
        int mounted = 0;
        for (FsController<?> controller : manager)
            if (isTopLevelArchive(controller))
                if (controller.getModel().isMounted()) mounted++;
        return mounted;
    }

    private boolean isTopLevelArchive(final FsController<?> controller) {
        final FsController<?> parent = controller.getParent();
        return null != parent && null == parent.getParent();
    }

    /**
     * Registers the archive file system of the given controller as the most
     * recently used (MRU).
     * 
     * @param  c the controller for the most recently used file system.
     */
    void accessed(final PaceController c) {
        if (c.isMounted()) mounted.put(c.getMountPoint(), c);
    }

    /**
     * If the number of mounted archive files exceeds {@link #getMaximumFileSystemsMounted()},
     * then this method sync()s the least recently used (LRU) archive files
     * which exceed this value.
     * 
     * @param  c the controller for the file system to retain mounted for
     *         subsequent access.
     * @throws FsSyncException 
     */
    void retain(final PaceController c) throws FsSyncException {
        final FsMountPoint mp = c.getMountPoint();
        iterating: for (final Iterator<PaceController> i = evicted.iterator(); i.hasNext(); ) {
            final PaceController lc = i.next();
            final FsMountPoint lmp = lc.getMountPoint();
            final FsManager fm = new FsFilteringManager(manager, lmp);
            for (final FsController<?> fc : fm) {
                final FsMountPoint fmp = fc.getModel().getMountPoint();
                if (mp.equals(fmp) || mounted.containsKey(fmp)) {
                    if (lmp.equals(fmp) || mounted.containsKey(lmp)) i.remove();
                    continue iterating;
                }
            }
            i.remove(); // even if subsequent umount fails
            fm.sync(FsSyncOptions.SYNC);
        }
    }

    @Override
    public void sync() throws FsSyncException {
        sync(FsSyncOptions.NONE);
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        evicted.clear();
        try {
            manager.sync(options);
        } finally {
            mounted.clear();
        }
    }

    @SuppressWarnings("serial")
    private final class MountedFileSystemMap {
        private final LinkedHashMap<FsMountPoint, PaceController> map
                = new LinkedHashMap<FsMountPoint, PaceController>(
                    HashMaps.initialCapacity(getMaximumFileSystemsMounted() + 1),
                    0.75f,
                    true) {
            @Override
            public boolean removeEldestEntry(
                    final Map.Entry<FsMountPoint, PaceController> entry) {
                final boolean evict
                        = size() > getMaximumFileSystemsMounted();
                if (evict) {
                    final PaceController c = entry.getValue();
                    final boolean added = evicted.add(c);
                    assert added;
                }
                return evict;
            }
        };

        private final ReadLock readLock;
        private final WriteLock writeLock;

        MountedFileSystemMap() {
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
                    if (!i.next().isMounted()) {
                        i.remove();
                        c++;
                    }
                }
                return c;
            } finally {
                writeLock.unlock();
            }
        }
    } // MountedFileSystemMap
}
