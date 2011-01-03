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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Set;
import java.util.LinkedHashSet;
import net.jcip.annotations.ThreadSafe;

/**
 * Defines the common properties of a file system.
 *
 * @see     FileSystemController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class FileSystemModel {

    private final MountPoint mountPoint;
    private final FileSystemModel parent;
    private volatile boolean touched;
    private Set<FileSystemTouchedListener> touchedListeners
            = new LinkedHashSet<FileSystemTouchedListener>();

    public FileSystemModel( MountPoint mountPoint) {
        this(mountPoint, null);
    }

    public FileSystemModel( @NonNull final MountPoint mountPoint,
                            @CheckForNull final FileSystemModel parent) {
        if (!equals(mountPoint.getParent(),
                    (null == parent ? null : parent.getMountPoint())))
            throw new IllegalArgumentException("Parent/Member mismatch!");
        this.mountPoint = mountPoint;
        this.parent = parent;
    }

    private static boolean equals(Object o1, Object o2) {
        return o1 == o2 || null != o1 && o1.equals(o2);
    }

    /**
     * Returns the mount point of this file system model.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access file system metadata which is stored outside the file system,
     * e.g. in-memory stored passwords for RAES encrypted ZIP files.
     *
     * @return The mount point of this file system model.
     */
    @NonNull
    public final MountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the model of the parent file system or {@code null} if and
     * only if the file system is not federated, i.e. if it's not a member of
     * a parent file system.
     *
     * @return The nullable parent file system model.
     */
    @Nullable
    public final FileSystemModel getParent() {
        return parent;
    }

    /**
     * Returns {@code true} if and only if the contents of the federated file
     * system have been modified so that it needs
     * {@link FileSystemController#sync synchronization} with its parent file
     * system.
     */
    public final boolean isTouched() {
        return touched;
    }

    /**
     * Sets the value of the property {@code touched} to the new value and
     * notifies all listeners if it has effectively changed.
     */
    public final void setTouched(final boolean newTouched) {
        final boolean oldTouched = touched;
        touched = newTouched;
        if (newTouched != oldTouched) {
            final FileSystemEvent event = new FileSystemEvent(this);
            for (final FileSystemTouchedListener listener
                    : getFileSystemTouchedListeners())
                listener.touchedChanged(event);
        }
    }

    /**
     * Returns a protective copy of the set of file system touched listeners.
     *
     * @return A clone of the set of file system touched listeners.
     */
    @NonNull
    final synchronized Set<FileSystemTouchedListener> getFileSystemTouchedListeners() {
        return new LinkedHashSet<FileSystemTouchedListener>(touchedListeners);
    }

    /**
     * Adds the given listener to the set of file system touched listeners.
     *
     * @param listener the listener for file system touched events.
     */
    public final synchronized void addFileSystemTouchedListener(
            @NonNull FileSystemTouchedListener listener) {
        if (null == listener)
            throw new NullPointerException();
        touchedListeners.add(listener);
    }

    /**
     * Removes the given listener from the set of file system touched listeners.
     *
     * @param listener the listener for file system touched events.
     */
    public final synchronized void removeFileSystemTouchedListener(
            @Nullable FileSystemTouchedListener listener) {
        touchedListeners.remove(listener);
    }

    /**
     * Two file system models are considered equal if and only if their mount
     * points are equal.
     * This can't get overriden.
     */
    @Override
    public final boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof FileSystemModel
                    && this.mountPoint.equals(((FileSystemModel) that).mountPoint);
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return mountPoint.hashCode();
    }

    @Override
    public final String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[mountPoint=")
                .append(getMountPoint())
                .append(",parent=")
                .append(getParent())
                .append(",touched=")
                .append(touched)
                .append("]")
                .toString();
    }
}
