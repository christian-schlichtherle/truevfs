/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.*;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.Link;
import de.truezip.kernel.util.Link.Type;
import static de.truezip.kernel.util.Link.Type.STRONG;
import static de.truezip.kernel.util.Link.Type.WEAK;
import static de.truezip.kernel.util.Links.target;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ArchiveManager extends FsManager {

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     * All access to this map must be externally synchronized!
     */
    private final Map<FsMountPoint, Link<FsController<?>>>
            schedulers = new WeakHashMap<>();

    private final Type optionalScheduleType;

    ArchiveManager() { this(WEAK); }

    ArchiveManager(final Type optionalScheduleType) {
        assert null != optionalScheduleType;
        this.optionalScheduleType = optionalScheduleType;
    }

    @Override
    public synchronized FsController<?>
    controller(  FsMountPoint mountPoint,
                    FsCompositeDriver driver) {
        return controller(mountPoint, null, driver);
    }

    private FsController<?> controller(
            final FsMountPoint mountPoint,
            @CheckForNull FsController<?> parent,
            final FsCompositeDriver driver) {
        if (null == mountPoint.getParent()) {
            if (null != parent)
                throw new IllegalArgumentException("Parent/member mismatch!");
            final FsModel model = new FsModel(mountPoint, null);
            return driver.newController(this, model, null);
        }
        FsController<?> controller = target(schedulers.get(mountPoint));
        if (null == controller) {
            if (null == parent)
                parent = controller(mountPoint.getParent(), null, driver);
            final ScheduledModel model = new ScheduledModel(
                    mountPoint, parent.getModel());
            model.setController(controller = driver.newController(this, model, parent));
        }
        return controller;
    }

    @Override
    public final <E extends FsArchiveEntry> FsController<?> newController(
            final FsArchiveDriver<E> driver,
            final FsModel model,
            final FsController<?> parent) {
        assert !(model instanceof LockModel);
        final LockModel lmodel = new LockModel(model);
        // HC SUNT DRACONES!
        // The FalsePositiveArchiveController decorates the driver's
        // controller(s) so that the decorated controller (chain) does not need
        // to resolve operations on false positive archive files.
        // The driver's controllers decorate the FinalizeController so that
        // the former do not need to be concerned with most aspects of
        // implementing a virtual file system, other than passing on a
        // FalsePositiveException or a NeedsLockRetryException.
        // The FinalizeController decorates the LockController because even the
        // finalizer thread needs to acquire a file system lock when closing
        // streams or channels.
        // The LockController decorates the SyncController so that
        // the decorated controller (chain) doesn't need to be thread safe.
        // The SyncController decorates the CacheController because the
        // selective entry cache needs to get flushed on a NeedsSyncException.
        // The CacheController decorates the ResourceController because the
        // cache entries terminate streams and channels and shall not stop the
        // decorated controller (chain) from getting synced.
        // The ResourceController decorates the TargetArchiveController so that
        // trying to sync the file system while any stream or channel to the
        // latter is open gets detected and properly dealt with.
        return  new FalsePositiveArchiveController(
                    driver.decorate(
                        new FinalizeController(
                            new LockController(
                                new SyncController(
                                    new CacheController(driver.getIOPool(),
                                        new ResourceController(
                                            new TargetArchiveController<>(
                                                driver, lmodel, parent))))))));
    }

    @Override
    public synchronized int size() {
        return schedulers.size();
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return getControllers().iterator();
    }

    private synchronized Set<FsController<?>> getControllers() {
        final Set<FsController<?>>
                snapshot = new TreeSet<>(FsControllerComparator.REVERSE);
        for (final Link<FsController<?>> link : schedulers.values()) {
            final FsController<?> controller = target(link);
            if (null != controller)
                snapshot.add(controller);
        }
        return snapshot;
    }

    /**
     * A model which schedules its controller for
     * {@link #sync(BitField) synchronization} by &quot;observing&quot; its
     * {@code touched} property.
     * Extending its sub-class to register for updates to the {@code touched}
     * property is simpler, faster and requires a smaller memory footprint than
     * the alternative observer pattern.
     */
    private final class ScheduledModel extends FsModel {
        FsController<?> controller;
        boolean touched;

        ScheduledModel(FsMountPoint mountPoint, FsModel parent) {
            super(mountPoint, parent);
        }

        void setController(final FsController<?> controller) {
            assert null != controller;
            assert !touched;
            this.controller = controller;
            schedule(false);
        }

        @Override
        public boolean isTouched() {
            return touched;
        }

        /**
         * Schedules the file system controller for synchronization according
         * to the given touch status.
         */
        @Override
        public void setTouched(final boolean touched) {
            if (touched == this.touched)
                return;
            this.touched = touched;
            schedule(touched);
        }

        @SuppressWarnings("unchecked")
        void schedule(boolean mandatory) {
            synchronized (ArchiveManager.this) {
                schedulers.put(getMountPoint(), (Link<FsController<?>>)
                        (mandatory ? STRONG : optionalScheduleType)
                            .link(controller));
            }
        }
    } // ScheduledModel

    /**
     * Orders file system controllers so that all file systems appear before
     * any of their parent file systems.
     */
    private static final class FsControllerComparator
    implements Comparator<FsController<?>> {
        static final FsControllerComparator REVERSE
                = new FsControllerComparator();

        @Override
        public int compare( final FsController<?> l,
                            final FsController<?> r) {
            return r.getModel().getMountPoint().toHierarchicalUri()
                    .compareTo(l.getModel().getMountPoint().toHierarchicalUri());
        }
    } // FsControllerComparator
}
