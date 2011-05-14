/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import de.schlichtherle.truezip.util.Link.Type;
import java.util.Iterator;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Links;
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
public final class FsDefaultManager extends FsManager {

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     * All access to this map must be externally synchronized!
     */
    private final Map<FsMountPoint, Link<ScheduledModel>> schedulers
            = new WeakHashMap<FsMountPoint, Link<ScheduledModel>>();

    private final Type optionalScheduleType;

    public FsDefaultManager() {
        this(WEAK);
    }

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
            return driver.newController(new FsDefaultModel(mountPoint, null), null);
        }
        ScheduledModel model = Links.getTarget(schedulers.get(mountPoint));
        if (null == model) {
            if (null == parent)
                parent = getController(mountPoint.getParent(), null, driver);
            model = new ScheduledModel(mountPoint, parent, driver);
        }
        return model.controller;
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
        final FsFederatingController controller;

        @SuppressWarnings("LeakingThisInConstructor")
        ScheduledModel( final FsMountPoint mountPoint,
                        final @CheckForNull FsController<?> parent,
                        final FsCompositeDriver driver) {
            super(mountPoint, null == parent ? null : parent.getModel());
            schedule(false);
            this.controller = new FsFederatingController(
                    driver.newController(this, parent));
        }

        /**
         * Schedules the file system controller for synchronization according
         * to the given touch status.
         */
        @Override
        public void setTouched(boolean touched) {
            if (touched == super.isTouched())
                return;
            super.setTouched(touched);
            schedule(touched);
        }

        void schedule(boolean unconditional) {
            synchronized (FsDefaultManager.this) {
                schedulers.put(getMountPoint(),
                        (unconditional ? STRONG : optionalScheduleType)
                            .newLink(this));
            }
        }
    } // class ScheduledModel

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
        for (final Link<ScheduledModel> link : schedulers.values()) {
            final ScheduledModel model = Links.getTarget(link);
            final FsFederatingController controller
                    = null == model ? null : model.controller;
            if (null != controller)
                snapshot.add(controller);
        }
        return snapshot;
    }

    /**
     * Orders file system controllers so that all file systems appear before
     * any of their parent file systems.
     */
    private static final Comparator<FsController<?>>
            BOTTOM_UP_COMPARATOR = new Comparator<FsController<?>>() {
        @Override
        public int compare( FsController<?> l,
                            FsController<?> r) {
            return r.getModel().getMountPoint().getHierarchicalUri()
                    .compareTo(l.getModel().getMountPoint().getHierarchicalUri());
        }
    };
}
