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
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.HashMaps;
import net.java.truevfs.comp.jmx.JmxManager;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * The pace manager.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class PaceManager extends JmxManager<PaceMediator> {

    private static final int INITIAL_CAPACITY = HashMaps.initialCapacity(
            PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE + 1);

    private final Collection<FsController> evicted = new ConcurrentLinkedQueue<>();
    private final MountedControllerMap mounted = new MountedControllerMap(evicted);
    private final SyncedControllerSet synced = new SyncedControllerSet(mounted);

    PaceManager(PaceMediator mediator, FsManager manager) {
        super(mediator, manager);
    }

    int getMaximumFileSystemsMounted() {
        return mounted.max;
    }

    void setMaximumFileSystemsMounted(final int max) {
        if (max < PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_MINIMUM_VALUE)
            throw new IllegalArgumentException();
        mounted.max = max;
    }

    @Override
    protected Object newView() { return new PaceManagerView(this); }

    /**
     * If the number of mounted archive files exceeds
     * {@link #getMaximumFileSystemsMounted()}, then this method
     * {@linkplain #sync syncs} the least recently used (LRU) archive files
     * which exceed this value.
     *
     * @param  controller the controller for the file system to retain mounted
     *         for subsequent access.
     */
    void retain(final FsController controller) throws FsSyncException {
        final Iterator<FsController> i = evicted.iterator();
        if (!i.hasNext()) return;
        final FsManager manager = FsManagerLocator.SINGLETON.get();
        final FsMountPoint mp = controller.getModel().getMountPoint();
        next: while (i.hasNext()) {
            final FsController ec = i.next();
            final FsMountPoint emp = ec.getModel().getMountPoint();
            final FsManager fm = new FsFilteringManager(emp, manager);
            for (final FsController fc : fm) {
                final FsMountPoint fmp = fc.getModel().getMountPoint();
                if (mp.equals(fmp) || synced.contains(fmp)) {
                    if (emp.equals(fmp) || synced.contains(emp)) i.remove();
                    continue next;
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
        if (controller.getModel().isMounted()) synced.add(controller);
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        evicted.clear();
        try {
            manager.sync(options);
        } finally {
            synced.mount(manager);
        }
    }

    @SuppressWarnings("serial")
    private static final class MountedControllerMap
    extends LinkedHashMap<FsMountPoint, FsController> {
        volatile int max =
                PaceManagerMXBean.MAXIMUM_FILE_SYSTEMS_MOUNTED_DEFAULT_VALUE;
        final Collection<FsController> evicted;
        
        MountedControllerMap(final Collection<FsController> evicted) {
            super(INITIAL_CAPACITY, 0.75f, true);
            assert null != evicted;
            this.evicted = evicted;
        }

        @Override
        public boolean removeEldestEntry(
                final Map.Entry<FsMountPoint, FsController> entry) {
            final boolean evict = size() > max;
            if (evict) {
                final FsController c = entry.getValue();
                final boolean added = evicted.add(c);
                assert added;
            }
            return evict;
        }
    } // MountedControllerMap

    private static final class SyncedControllerSet {
        final Map<FsMountPoint, FsController> mounted;
        final ReentrantReadWriteLock.ReadLock readLock;
        final ReentrantReadWriteLock.WriteLock writeLock;

        SyncedControllerSet(final Map<FsMountPoint, FsController> mounted) {
            final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
            readLock = lock.readLock();
            writeLock = lock.writeLock();
            assert null != mounted;
            this.mounted = mounted;
        }

        boolean contains(FsMountPoint key) {
            readLock.lock();
            try {
                return mounted.containsKey(key);
            } finally {
                readLock.unlock();
            }
        }

        void add(final FsController controller) {
            final FsMountPoint mp = controller.getModel().getMountPoint();
            writeLock.lock();
            try {
                mounted.put(mp, controller);
            } finally {
                writeLock.unlock();
            }
        }

        int mount(final FsManager manager) {
            writeLock.lock();
            try {
                mounted.clear();
                for (final FsController c : manager)
                    if (c.getModel().isMounted())
                        mounted.put(c.getModel().getMountPoint(), c);
                return mounted.size();
            } finally {
                writeLock.unlock();
            }
        }
    } // SyncedControllerSet
}
