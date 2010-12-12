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
 * <ol>
 * <li>The URI must not have a fragment.
 * <li>If the URI is opaque, its scheme specific part must contain at least
 *     one bang slash separator {@code "!/"}.
 *     The part <em>up to</em> the last bang slash separator is parsed
 *     according to the syntax constraints for a {@link MountPoint} and set as
 *     the value of the property {@link #getMountPoint() mount point}.
 *     The part <em>after</em> the last bang slash separator is parsed
 *     according to the syntax constraints for an {@link EntryName} and set as
 *     the value of the property {@link #getEntryName() entry name}.
 * <li>If the URI is absolute, it's resolved with ".", parsed according to
 *     the syntax constraints for a {@link MountPoint} and set as the value of
 *     the property {@link #getMountPoint() mount point}.
 *     The value of the property {@link #getEntryName() entry name} is then set
 *     to the URI relativized to this {@link #getMountPoint() mount point}.
 * <li>Otherwise, the value of the property
 *     {@link #getMountPoint() mount point} is set to {@code null} and the URI
 *     is parsed according to the syntax constraints for an {@link EntryName}
 *     and set as the value of the property {@link #getEntryName() entry name}.
 * </ol>
 * <p>
 * Examples for valid path URIs are:
 * <ul>
 * <li>{@code foo:bar:/baz!/bang} (mountPoint="foo:bar:/baz!/", entryName="bang")
 * <li>{@code foo:/bar} (mountPoint="foo:/", entryName="bar")
 * <li>{@code foo:/bar/} (mountPoint="foo:/bar/", entryName="")
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
        if (null != uri.getRawFragment())
            throw new URISyntaxException(uri.toString(), "Fragment not allowed");
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
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else if (uri.isAbsolute()) {
            if (normalize)
                uri = uri.normalize();
            else if (uri.normalize() != uri)
                throw new URISyntaxException(uri.toString(),
                        "Path not in normal form");
            mountPoint = new MountPoint(uri.resolve("."));
            entryName = new EntryName(mountPoint.getUri().relativize(uri));
        } else {
            mountPoint = null;
            entryName = new EntryName(uri, normalize);
            if (normalize)
                uri = entryName.getUri();
        }
        this.uri = uri;

        assert invariants();
    }

    /**
     * Constructs a new path by synthesizing its URI from the given
     * mount point and entry name.
     *
     * @param  mountPoint the nullable {@link #getMountPoint() mount point}.
     * @param  entryName the non-{@code null} {@link #getEntryName() entry name}.
     * @throws URISyntaxException if the synthesized path URI
     *         would not conform to the syntax constraints for paths.
     */
    public Path(final MountPoint mountPoint, final EntryName entryName) {
        if (null == mountPoint) {
            this.uri = entryName.getUri();
        } else if (mountPoint.getUri().isOpaque()) {
            try {
                this.uri = new URI(mountPoint.toString() + entryName.toString());
            } catch (URISyntaxException ex) {
                throw (AssertionError) new AssertionError("Check specification of syntax constraints!")
                        .initCause(ex);
            }
        } else {
            this.uri = mountPoint.getUri().resolve(entryName.getUri());
        }
        this.mountPoint = mountPoint;
        this.entryName = entryName;

        assert invariants();
    }

    private boolean invariants() {
        assert null != uri;
        assert null == uri.getRawFragment();
        assert (null != mountPoint) == uri.isAbsolute();
        assert null != entryName;
        if (uri.isOpaque()) {
            assert uri.getRawSchemeSpecificPart().contains(BANG_SLASH);
            assert uri.equals(URI.create(   mountPoint.getUri().toString()
                                            + entryName.getUri().toString()));
        } else if (uri.isAbsolute()) {
            assert uri.normalize() == uri;
            assert uri.equals(mountPoint.getUri().resolve(entryName.getUri()));
        } else {
            assert uri.normalize() == uri;
            assert entryName.getUri() == uri;
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
    public boolean equals(Object that) {
        return this == that
                || that instanceof Path
                    && this.uri.equals(((Path) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(Path that) {
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
