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
 * Addresses an entry in a file system.
 * A path is usually constructed from a {@link URI Uniform Resource Identifier}
 * in order to assert the following additional syntax constraints:
 * <p>
 * If the URI is opaque, its scheme specific part must contain at least one
 * bang slash separator {@code "!/"}.
 * The part <em>up to</em> the last bang slash separator is parsed according
 * to the syntax constraints for the {@link #getMountPoint() mount point}.
 * The part <em>after</em> the last bang slash separator is parsed according
 * to the syntax constraints for the {@link #getEntryName() entry name}.
 * <p>
 * Examples for valid path URIs are:
 * <ul>
 * <li>{@code foo:bar:/baz!/bang} (mountPoint="foo:bar:/baz!/", entryName="bang")
 * <li>{@code foo:/bar/} (there are no constraints for hierarchical URIs)
 * </ul>
 * Examples for invalid path URIs are:
 * <ul>
 * <li>{@code foo:bar} (opaque URI w/o bang slash separator)
 * <li>{@code foo:bar:baz:/bang!/boom} (dito)
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

    private final URI uri;
    private final MountPoint mountPoint;
    private final EntryName entryName;

    /**
     * Equivalent to {@link #create(URI, boolean) create(uri, false)}.
     */
    public static Path create(URI uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new path by parsing the given URI.
     * This static factory method calls
     * {@link #Path(URI, boolean) new Path(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @return A non-{@code null} path.
     */
    public static Path create(URI uri, boolean normalize) {
        try {
            return new Path(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #Path(URI, boolean) new Path(uri, false)}.
     */
    public Path(URI uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new path by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     */
    public Path(URI uri, final boolean normalize)
    throws URISyntaxException {
        if (uri.isOpaque()) {
            final String ssp = uri.getSchemeSpecificPart();
            final int i = ssp.lastIndexOf(BANG_SLASH);
            if (0 > i)
                throw new URISyntaxException(uri.toString(),
                        "Missing separator \"" + BANG_SLASH + '"');
            mountPoint = new MountPoint(
                    new URI(uri.getScheme(), ssp.substring(0, i + 2), null),
                    normalize);
            entryName = new EntryName(
                    new URI(null, ssp.substring(i + 2), uri.getFragment()),
                    normalize);
            if (normalize) {
                final URI nuri = new URI(
                        mountPoint.toString() + entryName.toString());
                uri = uri.equals(nuri) ? uri : nuri;
            }
        } else {
            mountPoint = null;
            entryName = null;
            if (normalize)
                uri = uri.normalize();
        }
        this.uri = uri;

        assert invariants();
    }

    /**
     * Constructs a new entry name by synthesizing its URI from the given
     * mount point and entry name.
     *
     * @param  mountPoint the non-{@code null} {@link #getMountPoint() mount point}.
     * @param  entryName the non-{@code null} {@link #getEntryName() entry name}.
     */
    public Path(final MountPoint mountPoint, final EntryName entryName) {
        try {
            this.uri = new URI(mountPoint.toString() + entryName.toString());
        } catch (URISyntaxException ex) {
            throw (AssertionError) new AssertionError("Check specification of syntax constraints!")
                    .initCause(ex);
        }
        this.mountPoint = mountPoint;
        this.entryName = entryName;

        assert invariants();
    }

    private boolean invariants() {
        assert null != uri;
        if (uri.isOpaque()) {
            assert uri.toString().contains(BANG_SLASH);
            assert null != mountPoint;
        }
        if (null != entryName) {
            assert null != mountPoint;
            assert uri.isOpaque() || 0 != entryName.toString().length();
        } else {
            assert null == mountPoint;
        }
        return true;
    }

    /**
     * Returns the mount point or {@code null} iff the {@link #getUri() URI}
     * is hierarchical.
     *
     * @return The nullable mount point.
     */
    public MountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the entry name or {@code null} iff the {@link #getUri() URI}
     * is hierarchical.
     *
     * @return The nullable entry name.
     */
    public EntryName getEntryName() {
        return entryName;
    }

    /**
     * Returns the non-{@code null} URI.
     *
     * @return The non-{@code null} URI.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns {@code true} iff the given object is a path name and its URI
     * {@link URI#equals(Object) equals} the URI of this path name.
     * Note that this ignores the mount point and entry name.
     */
    @Override
    public boolean equals(final Object that) {
        return this == that
                || that instanceof Path
                    && this.uri.equals(((Path) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(final Path that) {
        return this.uri.compareTo(that.uri);
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    /**
     * Equivalent to calling {@link URI#toString()} on {@link #getUri()}.
     */
    @Override
    public String toString() {
        return uri.toString();
    }
}
