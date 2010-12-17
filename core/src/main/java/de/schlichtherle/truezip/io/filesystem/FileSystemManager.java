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

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Links;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.filesystem.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * A container which manages the lifecycle of controllers for federated file
 * systems. A file system is federated if and only if it's a member of a parent
 * file system.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
// FIXME: Enable decorators by making this an interface or abstract class.
@ThreadSafe
public class FileSystemManager {

    public static final Comparator<FileSystemController<?>> REVERSE_CONTROLLERS
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

    /**
     * Returns a file system controller for the given mount point.
     * If and only if the given mount point addresses a federated file system,
     * the returned file system controller is remembered for life cycle
     * management, i.e. future lookup and {@link #sync synchronization}
     * operations.
     *
     * @param  mountPoint the non-{@code null} mount point of the file system.
     * @param  driver the non-{@code null} file system driver which will be
     *         used to create a new file system controller if required.
     * @param  parent the nullable controller for the parent file system.
     * @return A non-{@code null} file system controller.
     * @throws NullPointerException if {@code mountPoint} is {@code null}
     */
    public FileSystemController<?> getController(
            final MountPoint mountPoint,
            final FileSystemDriver driver,
            FileSystemController<?> parent) {
        if (null == mountPoint.getParent()) {
            if (null != parent)
                throw new IllegalArgumentException("Parent/member mismatch!");
            return driver.newController(mountPoint, null);
        }
        // This is faster than calling a synchronized method, why?
        synchronized (this) {
            return getController0(mountPoint, driver, parent);
        }
    }

    private /*synchronized*/ FileSystemController<?> getController0(
            final MountPoint mountPoint,
            final FileSystemDriver driver,
            FileSystemController<?> parent) {
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

    /**
     * Writes all changes to the contents of the federated file systems who's
     * mount point starts with the given {@code prefix} to their respective
     * parent file system.
     * This will reset the state of the respective file system controllers.
     * This method is thread-safe.
     *
     * @param  prefix the prefix of the mount point of the federated file
     *         systems which shall get synchronized to their respective parent
     *         file system.
     *         This may be {@code null} in order to select all federated file
     *         systems.
     * @throws SyncWarningException if the configuration uses the
     *         {@link SyncExceptionBuilder} and <em>only</em>
     *         warning conditions occured throughout the course of this method.
     *         This implies that the respective file system has been
     *         synchronized with constraints, e.g. a failure to set the last
     *         modification time of the parent file system entry to the last
     *         modification time of the (virtual) root directory of its file
     *         system.
     * @throws SyncException if the configuration uses the
     *         {@link SyncExceptionBuilder} and any error
     *         condition occured throughout the course of this method.
     *         This implies loss of data!
     * @throws NullPointerException if {@code builder} or {@code options} is
     *         {@code null}.
     * @throws IllegalArgumentException if the configuration property
     *         {@code FORCE_CLOSE_INPUT} is {@code false} and
     *         {@code FORCE_CLOSE_OUTPUT} is {@code true}.
     */
    public <E extends IOException>
    void sync(  final MountPoint prefix,
                final ExceptionBuilder<? super IOException, E> builder,
                final BitField<SyncOption> options)
    throws E {
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT)
                || options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();
        // The general algorithm is to sort the mount points in descending
        // order of their pathnames and then traverse the array in reverse
        // order to call the sync() method on each respective file system
        // controller.
        // This ensures that a member file system will always be synced
        // before its parent file system.
        for (final FileSystemController<?> controller
                : getControllers(prefix, REVERSE_CONTROLLERS)) {
            try {
                controller.sync(builder, options);
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

    final synchronized Set<FileSystemController<?>> getControllers(
            final MountPoint prefix,
            final Comparator<? super FileSystemController<?>> comparator) {
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
