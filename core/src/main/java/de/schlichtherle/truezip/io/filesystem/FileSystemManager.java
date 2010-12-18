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
package de.schlichtherle.truezip.io.filesystem;

import java.util.Iterator;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Links;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.filesystem.SyncOption.*;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class FileSystemManager extends NewClass {

    /**
     * Orders file system controllers so that all file systems appear before
     * any of their parent file systems.
     */
    private static final Comparator<FileSystemController<?>> BOTTOM_UP_COMPARATOR
            = new Comparator<FileSystemController<?>>() {
        @Override
        public int compare( FileSystemController<?> l,
                            FileSystemController<?> r) {
            return r.getModel().getMountPoint().hierarchicalize()
                    .compareTo(l.getModel().getMountPoint().hierarchicalize());
        }
    };

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     * All access to this map must be externally synchronized!
     */
    private final Map<MountPoint, Link<Scheduler>> schedulers
            = new WeakHashMap<MountPoint, Link<Scheduler>>();

    @Override
    @NonNull
    public synchronized FileSystemController<?> getController(
            @NonNull MountPoint mountPoint,
            @NonNull FileSystemDriver<?> driver) {
        return getController(mountPoint, driver, null);
    }

    private FileSystemController<?> getController(
            final MountPoint mountPoint,
            final FileSystemDriver<?> driver,
            FileSystemController<?> parent) {
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

    private final class Scheduler implements FileSystemTouchedListener {

        final ManagedFileSystemController controller;

        Scheduler(final FileSystemController<?> prospect) {
            controller = new ManagedFileSystemController(prospect);
            controller.getModel().addFileSystemTouchedListener(this);
            touchedChanged(null); // setup schedule
        }

        /**
         * Schedules the file system controller for synchronization according
         * to the given touch status.
         */
        @Override
        public void touchedChanged(final FileSystemEvent event) {
            final FileSystemModel model = controller.getModel();
            assert null == event || event.getSource() == model;
            synchronized (FileSystemManager.this) {
                schedulers.put(model.getMountPoint(),
                        (model.isTouched() ? STRONG : WEAK).newLink(this));
            }
        }
    } // class Scheduler

    public <X extends IOException>
    void sync(  @NonNull final BitField<SyncOption> options,
                @NonNull final ExceptionBuilder<? super IOException, X> builder,
                @Nullable final MountPoint prefix)
    throws X {
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT)
                || options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();
        // The general algorithm is to sort the mount points in descending
        // order of their pathnames and then traverse the set in reverse
        // order to call the sync() method on each respective file system
        // controller.
        // This ensures that a member file system will always be synced
        // before its parent file system.
        for (final FileSystemController<?> controller
                : getControllers(BOTTOM_UP_COMPARATOR, prefix)) {
            try {
                controller.sync(options, builder);
            } catch (IOException ex) {
                // Syncing the file system resulted in an I/O exception for
                // some reason.
                // We are bullheaded and store the exception for later
                // throwing and continue updating the rest.
                builder.warn(ex);
            }
        }
        builder.check();
    }

    @Override
    public int size() {
        return schedulers.size();
    }

    @Override
    public Iterator<FileSystemController<?>> iterator() {
        return getControllers(BOTTOM_UP_COMPARATOR, null).iterator();
    }

    /**
     * Returns a new set with all federated file systems managed by this
     * instance.
     *
     * @return A new set with all federated file systems managed by this
     *         instance.
     */
    private Set<FileSystemController<?>> getControllers() {
        return getControllers(null, null);
    }

    /**
     * Returns a new set with all federated file systems managed by this
     * instance which have a mount point which starts with the given
     * {@code prefix}.
     *
     * @return A new set with all federated file systems managed by this
     *         instance which have a mount point which starts with the given
     *         {@code prefix}.
     */
    private Set<FileSystemController<?>> getControllers(@Nullable MountPoint prefix) {
        return getControllers(null, prefix);
    }

    private synchronized Set<FileSystemController<?>> getControllers(
            @NonNull final Comparator<? super FileSystemController<?>> comparator,
            @Nullable final MountPoint prefix) {
        final Set<FileSystemController<?>> snapshot = null != comparator
                ? new TreeSet<FileSystemController<?>>(comparator)
                : new HashSet<FileSystemController<?>>((int) (schedulers.size() / .75f) + 1);
        for (final Link<Scheduler> link : schedulers.values()) {
            final Scheduler scheduler = Links.getTarget(link);
            final ManagedFileSystemController controller
                    = null == scheduler ? null : scheduler.controller;
            if (null != controller)
                if (null == prefix
                        || controller.getModel().getMountPoint().hierarchicalize().toString()
                            .startsWith(prefix.hierarchicalize().toString()))
                    snapshot.add(controller);
        }
        return snapshot;
    }
}
