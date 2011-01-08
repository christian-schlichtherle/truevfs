/*
 * Copyright (C) 2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.util.Link.Type;
import java.util.Iterator;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Links;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class FSFederationManager1 extends FSManager1 {

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     * All access to this map must be externally synchronized!
     */
    private final Map<FSMountPoint1, Link<Scheduler>> schedulers
            = new WeakHashMap<FSMountPoint1, Link<Scheduler>>();

    private final Type optionalScheduleType;

    public FSFederationManager1() {
        this(WEAK);
    }

    FSFederationManager1(final Type optionalScheduleType) {
        assert null != optionalScheduleType;
        this.optionalScheduleType = optionalScheduleType;
    }

    @Override
    @NonNull public synchronized FsController<?>
    getController(  @NonNull FSMountPoint1 mountPoint,
                    @NonNull FSDriver1 driver) {
        return getController(mountPoint, driver, null);
    }

    private FsController<?>
    getController(  final FSMountPoint1 mountPoint,
                    final FSDriver1 driver,
                    FsController<?> parent) {
        if (null == mountPoint.getParent()) {
            if (null != parent)
                throw new IllegalArgumentException("Parent/member mismatch!");
            return driver.newController(mountPoint, null);
        }
        Scheduler scheduler = Links.getTarget(schedulers.get(mountPoint));
        if (null == scheduler) {
            if (null == parent)
                parent = getController(mountPoint.getParent(), driver, null);
            scheduler = new Scheduler(driver.newController(mountPoint, parent));
        }
        return scheduler.controller;
    }

    private final class Scheduler implements FsTouchedListener {

        final FSFederationController1 controller;

        Scheduler(final FsController<?> prospect) {
            controller = new FSFederationController1(prospect);
            controller.getModel().addFileSystemTouchedListener(this);
            touchedChanged(null); // setup schedule
        }

        /**
         * Schedules the file system controller for synchronization according
         * to the given touch status.
         */
        @Override
        public void touchedChanged(final FSEvent1 event) {
            final FSModel1 model = controller.getModel();
            assert null == event || event.getSource() == model;
            synchronized (FSFederationManager1.this) {
                schedulers.put(model.getMountPoint(),
                        (model.isTouched() ? STRONG : optionalScheduleType)
                            .newLink(this));
            }
        }
    } // class Scheduler

    @Override
    public synchronized int getSize() {
        return schedulers.size();
    }

    @Override
    public synchronized Iterator<FsController<?>> iterator() {
        return getControllers().iterator();
    }

    private Set<FsController<?>> getControllers() {
        final Set<FsController<?>> snapshot
                = new TreeSet<FsController<?>>(BOTTOM_UP_COMPARATOR);
        for (final Link<Scheduler> link : schedulers.values()) {
            final Scheduler scheduler = Links.getTarget(link);
            final FSFederationController1 controller
                    = null == scheduler ? null : scheduler.controller;
            if (null != controller)
                snapshot.add(controller);
        }
        return snapshot;
    }

    /**
     * Orders file system controllers so that all file systems appear before
     * any of their parent file systems.
     */
    private static final Comparator<FsController<?>> BOTTOM_UP_COMPARATOR
            = new Comparator<FsController<?>>() {
        @Override
        public int compare( FsController<?> l,
                            FsController<?> r) {
            return r.getModel().getMountPoint().hierarchicalize()
                    .compareTo(l.getModel().getMountPoint().hierarchicalize());
        }
    };
}
