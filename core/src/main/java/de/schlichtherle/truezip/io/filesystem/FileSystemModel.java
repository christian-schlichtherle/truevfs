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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;

import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR;
import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.Paths.isRoot;

/**
 * Defines the common properties of a file system.
 * <p>
 * This class is <em>not</em> thread-safe!
 * Multithreading needs to be addressed by client classes.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemModel {
    static final String BANG_SEPARATOR = "!" + SEPARATOR;

    private final URI mountPoint;
    private final FileSystemModel parent;
    private final String parentPath;
    private boolean touched;
    private LinkedHashSet<FileSystemListener> listeners
            = new LinkedHashSet<FileSystemListener>();

    public FileSystemModel( URI mountPoint,
                            final FileSystemModel parent) {
        if (!mountPoint.isAbsolute())
            throw new IllegalArgumentException();
        if (!mountPoint.getRawSchemeSpecificPart().endsWith(SEPARATOR))
            throw new IllegalArgumentException();
        if (null != mountPoint.getRawFragment())
            throw new IllegalArgumentException();
        try {
            if (mountPoint.isOpaque()) {
                if (null == parent)
                    throw new NullPointerException("Missing parent!");
                final String ssp = mountPoint.getSchemeSpecificPart();
                if (!ssp.endsWith(BANG_SEPARATOR))
                    throw new URISyntaxException(   mountPoint.toString(),
                                                    "Doesn't end with the bang separator \""
                                                    + BANG_SEPARATOR + '"');
                final String pmp = parent.getMountPoint().toString();
                if (!ssp.startsWith(pmp))
                    throw new URISyntaxException(   mountPoint.toString(),
                                                    "Parent/member mismatch");
                this.parentPath = ssp.substring(pmp.length(), ssp.length() - 2)
                        + SEPARATOR_CHAR;
            } else {
                if (null != mountPoint.getRawQuery())
                    throw new IllegalArgumentException();
                mountPoint = mountPoint.normalize();
                if (null != parent) {
                    final URI pp = parent.getMountPoint().relativize(mountPoint);
                    if (pp.equals(mountPoint))
                        throw new URISyntaxException(   mountPoint.toString(),
                                                        "Parent/member mismatch");
                    assert null == pp.getScheme();
                    this.parentPath = pp.getPath();
                } else {
                    this.parentPath = null;
                }
            }
            if (null != this.parentPath
                    && (this.parentPath.startsWith(".." + SEPARATOR)
                        || this.parentPath.equals("..")))
                throw new URISyntaxException(   mountPoint.toString(),
                                                "Illegal parent path");
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        this.parent = parent;
        this.mountPoint = mountPoint;

        assert this.mountPoint.getSchemeSpecificPart().endsWith(SEPARATOR);
        assert (null == this.parent && null == this.parentPath)
                ^ (null != this.parent && this.parentPath.endsWith(SEPARATOR));
        assert null == this.parentPath
                || !this.parentPath.startsWith(".." + SEPARATOR);
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

    public final URI resolveURI(String path) {
        return mountPoint.isOpaque()
                ? URI.create(mountPoint + path)
                : mountPoint.resolve(path);
    }

    /**
     * Returns the model of the parentPath file system or {@code null} if and
     * only if this file system is not federated, i.e. if it's not a member of
     * another file system.
     */
    public final FileSystemModel getParent() {
        return parent;
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * this model's file system within its parentPath file system.
     *
     * @param  path a non-{@code null} entry name.
     * @throws RuntimeException if this file system model does not specify a
     *         {@link #getParent() parentPath file system model}.
     */
    public final String parentPath(String path) {
        return isRoot(path)
                ? cutTrailingSeparators(parentPath, SEPARATOR_CHAR)
                : parentPath + path;
    }

    /**
     * Returns {@code true} if and only if the contents of this composite file
     * system have been modified so that it needs
     * {@link FileSystemController#sync synchronization} with its parentPath file
     * system.
     */
    public final boolean isTouched() {
        return touched;
    }

    public final void setTouched(final boolean newTouched) {
        final boolean oldTouched = touched;
        touched = newTouched;
        if (newTouched != oldTouched) {
            final FileSystemEvent event = new FileSystemEvent(this);
            for (FileSystemListener listener : getFileSystemListeners())
                listener.touchChanged(event);
        }
    }

    /**
     * Returns a protective copy of the set of file system listeners.
     * 
     * @return A clone of the set of file system listeners.
     */
    @SuppressWarnings("unchecked")
    final Set<FileSystemListener> getFileSystemListeners() {
        return (Set<FileSystemListener>) listeners.clone();
    }

    /**
     * Adds the given listener to the set of file system listeners.
     *
     * @param  listener the non-{@code null} listener for file system events.
     * @throws NullPointerException if {@code listener} is {@code null}.
     */
    public final void addFileSystemListener(
            final FileSystemListener listener) {
        if (null == listener)
            throw new NullPointerException();
        listeners.add(listener);
    }

    /**
     * Removes the given listener from the set of file system listeners.
     *
     * @param  listener the non-{@code null} listener for file system events.
     * @throws NullPointerException if {@code listener} is {@code null}.
     */
    public final void removeFileSystemListener(
            final FileSystemListener listener) {
        if (null == listener)
            throw new NullPointerException();
        listeners.remove(listener);
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
