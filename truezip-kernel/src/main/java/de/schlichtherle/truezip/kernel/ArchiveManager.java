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
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ArchiveManager extends FsManager {

    static {
        Logger  .getLogger( ArchiveManager.class.getName(),
                            ArchiveManager.class.getName())
                .config("banner");
    }

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     */
    @GuardedBy("this")
    private final Map<FsMountPoint, Link<FsController<?>>>
            schedulers = new WeakHashMap<>();

    private final Type optionalScheduleType;

    ArchiveManager() {
        this(WEAK);
    }

    /** Solely provided for unit testing. */
    ArchiveManager(final Type optionalScheduleType) {
        assert null != optionalScheduleType;
        this.optionalScheduleType = optionalScheduleType;
    }

    @Override
    public synchronized FsController<?> controller(
            FsCompositeDriver driver,
            FsMountPoint mountPoint) {
        return controller(driver, mountPoint, null);
    }

    private FsController<?> controller(
            final FsCompositeDriver driver,
            final FsMountPoint mountPoint,
            @CheckForNull FsController<?> parent) {
        if (null == mountPoint.getParent()) {
            if (null != parent)
                throw new IllegalArgumentException("Parent/member mismatch!");
            final FsModel model = new FsModel(mountPoint, null);
            return driver.newController(this, model, null);
        }
        FsController<?> controller = target(schedulers.get(mountPoint));
        if (null == controller) {
            if (null == parent)
                parent = controller(driver, mountPoint.getParent(), null);
            final ScheduledModel model = new ScheduledModel(
                    mountPoint, parent.getModel());
            model.setController(controller = driver.newController(this, model, parent));
        }
        return controller;
    }

    @Override
    public final FsController<?> newController(
            final FsArchiveDriver<?> driver,
            final FsModel model,
            final FsController<?> parent) {
        assert !(model instanceof LockModel);
        final LockModel lmodel = new LockModel(model);
        // HC SUNT DRACONES!
        // The FalsePositiveArchiveController decorates the FinalizeController
        // so that the decorated controller (chain) does not need to resolve
        // operations on false positive archive files.
        // The FinalizeController decorates the driver's controllers so that
        // each and every resource which may get opened by the decorated
        // controller (chain) is ensured to get closed.
        // The driver's controllers decorate the LockController because the
        // former shall not get guarded by the file system locks but should
        // otherwise not need to be concerned with most other aspects of
        // implementing a virtual file system - other than passing on a
        // FalsePositiveException or a NeedsLockRetryException.
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
                    new FinalizeController(
                        driver.decorate(
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
                snapshot = new TreeSet<>(ReverseControllerComparator.INSTANCE);
        for (final Link<FsController<?>> link : schedulers.values()) {
            final FsController<?> controller = target(link);
            if (null != controller)
                snapshot.add(controller);
        }
        return snapshot;
    }

    @Override
    public void sync(BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        SyncShutdownHook.cancel();
        super.sync(options);
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
            if (this.touched != touched) {
                if (touched)
                    SyncShutdownHook.register(ArchiveManager.this);
                schedule(touched);
                this.touched = touched;
            }
        }

        @SuppressWarnings("unchecked")
        void schedule(final boolean mandatory) {
            final FsMountPoint mountPoint = getMountPoint();
            final Link<FsController<?>> link = (Link<FsController<?>>)
                    (mandatory ? STRONG : optionalScheduleType).newLink(controller);
            synchronized (ArchiveManager.this) {
                schedulers.put(mountPoint, link);
            }
        }
    } // ScheduledModel

    /**
     * Orders file system controllers so that all file systems appear before
     * any of their parent file systems.
     */
    private static final class ReverseControllerComparator
    implements Comparator<FsController<?>> {
        static final ReverseControllerComparator INSTANCE
                = new ReverseControllerComparator();

        @Override
        public int compare(FsController<?> o1, FsController<?> o2) {
            return o2.getModel().getMountPoint().toHierarchicalUri()
                    .compareTo(o1.getModel().getMountPoint().toHierarchicalUri());
        }
    } // ReverseControllerComparator
}
