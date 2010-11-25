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

import java.net.URI;

import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR;
import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.Paths.isRoot;

/**
 * Defines the common properties of any file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemModel {
    private final URI mountPoint;
    private final FileSystemModel parent;
    private final String parentPath;
    private boolean touched;
    private FileSystemListener listener;

    public FileSystemModel(URI mountPoint) {
        this(mountPoint, null);
    }

    public FileSystemModel(URI mountPoint,
                           final FileSystemModel parent) {
        if (!mountPoint.isAbsolute())
            throw new IllegalArgumentException();
        if (!mountPoint.getPath().endsWith(SEPARATOR))
            mountPoint = URI.create(mountPoint.toString() + SEPARATOR_CHAR);
        this.mountPoint = mountPoint = mountPoint.normalize();
        this.parent = parent;
        if (null != parent) {
            if (mountPoint.isOpaque())
                throw new IllegalArgumentException();
            final URI parentMountPoint = parent.getMountPoint()
                    .relativize(mountPoint);
            if (parentMountPoint.equals(mountPoint))
                throw new IllegalArgumentException("parent/member mismatch");
            this.parentPath = parentMountPoint.getPath();
        } else {
            this.parentPath = null;
        }

        assert mountPoint.isOpaque() || mountPoint.getPath().endsWith(SEPARATOR);
        assert (null == parent && null == parentPath)
                ^ (null != parent && parentPath.endsWith(SEPARATOR));
    }

    /**
     * Returns an absolute, hierarchical and normalized Unique Resource
     * Identifier (URI) of the file system's <i>mount point</i> in the
     * federated file system.
     * The path of this URI ends with a {@code '/'} character so that
     * relative URIs can be resolved against it.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access file system metadata which is stored outside the federated
     * file system, e.g. in-memory stored passwords for RAES encrypted ZIP
     * files.
     * <p>
     * Implementation note: If the returned URI uses the <i>file scheme</i>,
     * its path needs to be {@link java.io.File#getCanonicalPath() canonical}
     * in order to be really unique.
     *
     * @return A non-{@code null} URI for the mount point of the file system.
     */
    public final URI getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the model of the parent file system or {@code null} if and
     * only if this file system is not federated, i.e. if it's not a member of
     * another file system.
     */
    public final FileSystemModel getParent() {
        return parent;
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * this model's file system within its parent file system.
     *
     * @param  path a non-{@code null} common entry name.
     * @throws RuntimeException if this file system model does not specify a
     *         {@link #getParent() parent file system model}.
     */
    public String parentPath(String path) {
        return isRoot(path)
                ? cutTrailingSeparators(parentPath, SEPARATOR_CHAR)
                : parentPath + path;
    }

    /**
     * Returns {@code true} if and only if the contents of this composite file
     * system have been modified so that it needs
     * {@link FileSystemController#sync synchronization} with its parent file
     * system.
     */
    public final boolean isTouched() {
        return touched;
    }

    public final void setTouched(final boolean newTouched) {
        final boolean oldTouched = touched;
        touched = newTouched;
        if (newTouched != oldTouched)
            if (null != listener)
                listener.touchChanged(new FileSystemEvent(this));
    }

    public final void addFileSystemListener(final FileSystemListener listener) {
        if (null != this.listener)
            throw new UnsupportedOperationException("Not supported yet.");
        this.listener = listener;
    }

    public final void removeFileSystemListener(final FileSystemListener listener) {
        this.listener = null;
    }

    @Override
    public final String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[mountPoint=")
                .append(getMountPoint())
                .append(",touched=")
                .append(touched)
                .append("]")
                .toString();
    }
}
