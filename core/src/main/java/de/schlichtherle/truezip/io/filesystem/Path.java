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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import static de.schlichtherle.truezip.io.filesystem.FileSystemEntry.SEPARATOR;

/**
 * Addresses an entry in a federated file system.
 * Every path instance has a {@link #getPathName() path name}.
 * The path name is a {@link URI Uniform Resource Identifier} which conforms
 * to the following additional syntax constraints for Paths:
 * <p>
 * If the path name is opaque, its scheme specific part must contain at least
 * one bang slash separator {@code "!/"}.
 * The part <em>after</em> the last bang slash separator is parsed as a
 * relative URI to form the {@link #getEntryName() entry name}.
 * The part <em>before</em> the last bang slash separator is recursively parsed
 * as a Path again to form the {@link #getMountPoint() mount point}.
 * <p>
 * Examples for valid path names are:
 * <ul>
 * <li>{@code foo:bar:/baz!/bang} (mountPoint.pathName="bar:/baz", entryname="bang")
 * <li>{@code foo:/bar/} (there are no constraints for hierarchical URIs.
 * </ul>
 * Examples for invalid path names are:
 * <ul>
 * <li>{@code foo:bar} (opaque URI w/o bang slash separator)
 * <li>{@code foo:bar:baz:/bang!/} (dito)
 * </ul>
 * <p>
 * Note that this class is immutable and final, hence thread-safe, too.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class Path implements Serializable, Comparable<Path> {

    private static final long serialVersionUID = 5798435461242930648L;

    /** The separator which is used to split opaque path names into segments. */
    public static final String BANG_SLASH = "!" + SEPARATOR;

    private final URI pathName, entryName;
    private final Path mountPoint;

    /**
     * Constructs a new Path.
     * This static factory method calls
     * {@link #Path(URI) new Path(path)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  pathName the non-{@code null} {@link #getPathName() path name}.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws URISyntaxException if {@code pathName} does not conform to
     *         the additional syntax constraints for Paths.
     * @return A non-{@code null} Path.
     */
    public static Path create(URI pathName) {
        try {
            return new Path(pathName);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new Path.
     *
     * @param  pathName the non-{@code null} {@link #getPathName() path name}.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws URISyntaxException if {@code pathName} does not conform to
     *         the additional syntax constraints for Paths.
     */
    public Path(URI pathName) throws URISyntaxException {
        this(pathName, null);
    }

    /**
     * Constructs a new path.
     * <p>
     * If the given path name is opaque, its parent path name is parsed
     * according to the syntax specification for paths.
     * Then, if {@code parent} is {@code null}, the result is used to compute
     * the {@link #getMountPoint() mount point} and
     * {@link #getEntryName() entry name}.
     * Otherwise, the computed result must compare {@link #equals equal} to the
     * given {@link #getMountPoint() mount point}.
     * <p>
     * If the given path name is hierarchical and the given parent path is
     * not {@code null}, the parent path's path name must be an ancestor
     * of the given path name, i.e. the member name must not be empty.
     *
     * @param  pathName the non-{@code null} {@link #getPathName() path name}.
     * @param  mountPoint the nullable {@link #getMountPoint() mount point}.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws URISyntaxException if {@code pathName} does not conform to
     *         the additional syntax constraints for paths
     *         or {@code mountPoint} is not a valid mount point.
     */
    Path(final URI pathName, Path mountPoint) throws URISyntaxException {
        final URI entryName;
        if (pathName.isOpaque()) {
            final String ssp = pathName.getSchemeSpecificPart();
            final int i = ssp.lastIndexOf(BANG_SLASH);
            if (0 > i)
                throw new URISyntaxException(pathName.toString(),
                        "Missing separator \"" + BANG_SLASH + '"');
            final URI mountPointPathName = new URI(ssp.substring(0, i));
            entryName = new URI(null, ssp.substring(i + 2), pathName.getFragment());
            if (null == mountPoint)
                mountPoint = new Path(mountPointPathName);
            else if (!mountPoint.getPathName().equals(mountPointPathName))
                throw new URISyntaxException(pathName.toString(),
                        mountPoint.toString() + ": not a parent of");
        } else if (null != mountPoint)
            throw new IllegalArgumentException();
        else
            entryName = null;
        this.pathName = pathName;
        this.mountPoint = mountPoint;
        this.entryName = entryName;

        assert invariants();
    }

    private boolean invariants() {
        assert null != pathName;
        if (pathName.isOpaque()) {
            assert pathName.toString().contains(BANG_SLASH);
            assert null != mountPoint;
        }
        if (null != entryName) {
            assert null != mountPoint;
            assert !entryName.isAbsolute();
            assert pathName.isOpaque() || 0 != entryName.toString().length();
        } else {
            assert null == mountPoint;
        }
        return true;
    }

    //boolean isMountPoint();

    /**
     * Returns the nullable mount point.
     * Iff the path name provided to the constructor is hierarchical,
     * {@code null} is returned.
     * If the path name provided to the constructor is opaque, it must specify
     * a mount point is returned which has been computed from the path name.
     * {@code null} is returned.
     *
     * @return The nullable mount point.
     */
    public Path getMountPoint() {
        return mountPoint;
    }

    //boolean isEntry();

    /**
     * Returns the nullable entry name.
     * If this path has a {@link #getMountPoint() mount point}, then this
     * path's path name relative to the mount point's path name is returned.
     * Otherwise, {@code null} is returned.
     *
     * @return The nullable entry name.
     */
    public URI getEntryName() {
        return entryName;
    }

    /**
     * Returns the non-{@code null} path name which was provided to the
     * constructor.
     *
     * @return The non-{@code null} path name.
     */
    public URI getPathName() {
        return pathName;
    }

    /**
     * Returns {@code true} iff the given object is a path and its path name
     * {@link URI#equals(Object) equals} this path's path name.
     * Note that this ignores the mount point and entry name.
     */
    @Override
    public boolean equals(final Object that) {
        return this == that
                || that instanceof Path
                    && this.getPathName().equals(((Path) that).getPathName());
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(final Path that) {
        return this.getPathName().compareTo(that.getPathName());
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return getPathName().hashCode();
    }

    /**
     * Equivalent to calling {@link URI#toString()} on {@link #getPathName()}.
     */
    @Override
    public String toString() {
        return getPathName().toString();
    }
}
