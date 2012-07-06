/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Link.Type;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;
import static de.schlichtherle.truezip.util.Links.getTarget;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsDefaultManager extends FsManager {

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     */
    private final Map<FsMountPoint, Link<FsFalsePositiveController>> controllers
            = new WeakHashMap<FsMountPoint, Link<FsFalsePositiveController>>();

    private final Type optionalScheduleType;

    private final ReadLock readLock;
    private final WriteLock writeLock;

    public FsDefaultManager() {
        this(WEAK);
    }

    /** Solely provided for unit testing. */
    FsDefaultManager(final Type optionalScheduleType) {
        assert null != optionalScheduleType;
        this.optionalScheduleType = optionalScheduleType;
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    @Override
    public FsController<?> getController(
            FsMountPoint mountPoint,
            FsCompositeDriver driver) {
        writeLock.lock();
        try {
            return getController0(mountPoint, driver);
        } finally {
            writeLock.unlock();
        }
    }

    private FsController<?> getController0(
            final FsMountPoint mountPoint,
            final FsCompositeDriver driver) {
        if (null == mountPoint.getParent()) {
            final FsModel m = new FsDefaultModel(mountPoint, null);
            return driver.newController(m, null);
        }
        FsFalsePositiveController c = getTarget(controllers.get(mountPoint));
        if (null == c) {
            final FsController<?> p = getController0(mountPoint.getParent(), driver);
            final ScheduledModel m = new ScheduledModel(mountPoint, p.getModel());
            // HC SVNT DRACONES!
            m.setController(c =
                    new FsFalsePositiveController(
                        new FsFinalizeController<FsModel>(
                            driver.newController(m, p))));
        }
        return c;
    }

    @Override
    public int getSize() {
        readLock.lock();
        try {
            return controllers.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return sortedControllers().iterator();
    }

    private Set<FsController<?>> sortedControllers() {
        readLock.lock();
        try {
            final Set<FsController<?>> snapshot
                    = new TreeSet<FsController<?>>(ReverseControllerComparator.INSTANCE);
            for (final Link<FsFalsePositiveController> link : controllers.values()) {
                final FsController<?> controller = getTarget(link);
                if (null != controller)
                    snapshot.add(controller);
            }
            return snapshot;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super IOException, X> handler)
    throws X {
        FsSyncShutdownHook.cancel();
        super.sync(options, handler);
    }

    /**
     * A model which schedules its controller for
     * {@link #sync(BitField) synchronization} by &quot;observing&quot; its
     * {@code touched} property.
     * Extending its sub-class to register for updates to the {@code touched}
     * property is simpler, faster and requires a smaller memory footprint than
     * the alternative observer pattern.
     */
    private final class ScheduledModel extends FsDefaultModel {
        FsFalsePositiveController controller;
        boolean touched;

        ScheduledModel(FsMountPoint mountPoint, FsModel parent) {
            super(mountPoint, parent);
        }

        void setController(final FsFalsePositiveController controller) {
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
                    FsSyncShutdownHook.register(FsDefaultManager.this);
                schedule(touched);
                this.touched = touched;
            }
        }

        void schedule(boolean mandatory) {
            final FsMountPoint mountPoint = getMountPoint();
            final Link<FsFalsePositiveController> link =
                    (mandatory ? STRONG : optionalScheduleType).newLink(controller);
            writeLock.lock();
            try {
                controllers.put(mountPoint, link);
            } finally {
                writeLock.unlock();
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
