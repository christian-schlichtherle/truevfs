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

import java.util.Set;
import java.util.LinkedHashSet;

import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR;

/**
 * Defines the common properties of a file system.
 * <p>
 * This class is <em>not</em> thread-safe!
 * Multithreading needs to be addressed by client classes.
 *
 * @see     FileSystemController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemModel {
    static final String BANG_SEPARATOR = "!" + SEPARATOR;

    private final MountPoint mountPoint;
    private final FileSystemModel parent;
    private boolean touched;
    private LinkedHashSet<FileSystemTouchedListener> touchedListeners
            = new LinkedHashSet<FileSystemTouchedListener>();

    public FileSystemModel( MountPoint mountPoint) {
        this(mountPoint, null);
    }

    public FileSystemModel( final MountPoint mountPoint,
                            final FileSystemModel parent) {
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
     * Returns the non-{@code null} mount point of this file system model.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access file system metadata which is stored outside the federated
     * file system, e.g. in-memory stored passwords for RAES encrypted ZIP
     * files.
     *
     * @return The non-{@code null} mount point of this file system model.
     */
    public final MountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the model of the parent file system or {@code null} if and
     * only if the file system is not federated, i.e. if it's not a member of
     * another file system.
     *
     * @return The nullable parent file system model.
     */
    public final FileSystemModel getParent() {
        return parent;
    }

    /**
     * Resolves the given entry name against the entry name of the file system
     * in its parent file system.
     *
     * @param  entryName a non-{@code null} entry name relative to the file
     *         system's mount point.
     * @throws NullPointerException if {@code entryName} is {@code null} or if
     *         the file system is not federated, i.e. if it's not a member of
     *         another file system.
     * @return a non-{@code null} entry name relative to the parent file
     *         system's mount point.
     * @see    #getParent
     */
    public final FileSystemEntryName resolveParent(FileSystemEntryName entryName) {
        return mountPoint.resolveParent(entryName);
    }

    /**
     * Resolves the given entry name against the file system's mount point.
     *
     * @param  entryName a non-{@code null} entry name relative to the file
     *         system's mount point.
     * @throws NullPointerException if {@code entryName} is {@code null}.
     * @return A non-{@code null} path with an absolute URI.
     */
    public final Path resolveAbsolute(FileSystemEntryName entryName) {
        return mountPoint.resolveAbsolute(entryName);
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
            for (FileSystemTouchedListener listener : getFileSystemTouchedListeners())
                listener.touchedChanged(event);
        }
    }

    /**
     * Returns a protective copy of the set of file system touched listeners.
     * 
     * @return A clone of the set of file system listeners.
     */
    @SuppressWarnings("unchecked")
    final Set<FileSystemTouchedListener> getFileSystemTouchedListeners() {
        return (Set<FileSystemTouchedListener>) touchedListeners.clone();
    }

    /**
     * Adds the given listener to the set of file system touched listeners.
     *
     * @param  listener the non-{@code null} listener for file system events.
     * @throws NullPointerException if {@code listener} is {@code null}.
     */
    public final void addFileSystemTouchedListener(
            final FileSystemTouchedListener listener) {
        if (null == listener)
            throw new NullPointerException();
        touchedListeners.add(listener);
    }

    /**
     * Removes the given listener from the set of file system touched listeners.
     *
     * @param  listener the non-{@code null} listener for file system events.
     * @throws NullPointerException if {@code listener} is {@code null}.
     */
    public final void removeFileSystemTouchedListener(
            final FileSystemTouchedListener listener) {
        if (null == listener)
            throw new NullPointerException();
        touchedListeners.remove(listener);
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
