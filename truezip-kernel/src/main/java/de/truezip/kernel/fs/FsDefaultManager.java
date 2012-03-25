/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.fs.addr.FsMountPoint;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.Link;
import de.truezip.kernel.util.Link.Type;
import static de.truezip.kernel.util.Link.Type.STRONG;
import static de.truezip.kernel.util.Link.Type.WEAK;
import static de.truezip.kernel.util.Links.getTarget;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FsDefaultManager extends FsManager {

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     * All access to this map must be externally synchronized!
     */
    private final Map<FsMountPoint, Link<FsController<?>>> schedulers
            = new WeakHashMap<FsMountPoint, Link<FsController<?>>>();

    private final Type optionalScheduleType;

    FsDefaultManager() { this(WEAK); }

    /** Provided for unit testing. */
    FsDefaultManager(final Type optionalScheduleType) {
        assert null != optionalScheduleType;
        this.optionalScheduleType = optionalScheduleType;
    }

    @Override
    public synchronized FsController<?>
    getController(  FsMountPoint mountPoint,
                    FsCompositeDriver driver) {
        return getController(mountPoint, null, driver);
    }

    private FsController<?>
    getController(  final FsMountPoint mountPoint,
                    @CheckForNull FsController<?> parent,
                    final FsCompositeDriver driver) {
        if (null == mountPoint.getParent()) {
            if (null != parent)
                throw new IllegalArgumentException("Parent/member mismatch!");
            final FsModel model = new FsDefaultModel(mountPoint, null);
            return driver.newController(model, null);
        }
        FsController<?> controller = getTarget(schedulers.get(mountPoint));
        if (null == controller) {
            if (null == parent)
                parent = getController(mountPoint.getParent(), null, driver);
            final ScheduledModel model = new ScheduledModel(
                    mountPoint, parent.getModel());
            model.setController(controller = driver.newController(model, parent));
        }
        return controller;
    }
    
    @Override
    public synchronized int getSize() {
        return schedulers.size();
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return getControllers().iterator();
    }

    private synchronized Set<FsController<?>> getControllers() {
        final Set<FsController<?>> snapshot
                = new TreeSet<FsController<?>>(FsControllerComparator.REVERSE);
        for (final Link<FsController<?>> link : schedulers.values()) {
            final FsController<?> controller = getTarget(link);
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
    private final class ScheduledModel extends FsDefaultModel {
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
            synchronized (FsDefaultManager.this) {
                schedulers.put(getMountPoint(), (Link<FsController<?>>)
                        (mandatory ? STRONG : optionalScheduleType)
                            .newLink(controller));
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