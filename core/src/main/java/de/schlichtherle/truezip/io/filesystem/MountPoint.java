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
import static de.schlichtherle.truezip.io.filesystem.FileSystemEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.filesystem.Path.BANG_SLASH;

/**
 * Addresses the mount point of a file system.
 * A mount point is usually constructed from a
 * {@link URI Uniform Resource Identifier} in order to assert the following
 * additional syntax constraints:
 * <ol>
 * <li>The URI must be absolute.
 * <li>The URI must not have a fragment.
 * <li>If the URI is opaque, its scheme specific part must end with the bang
 *     slash separator {@code "!/"}.
 *     The scheme specific part <em>before</em> this bang slash separator is
 *     parsed according the syntax constraints for a {@link Path} and the
 *     following additional syntax constraints:
 *     The path must be absolute.
 *     If its opaque, it's entry name must not be empty.
 * <li>If the URI is hierarchical, its path must be in normal form and end with
 *     a {@link FileSystemEntry#SEPARATOR}.
 * </ol>
 * <p>
 * Examples for valid mount point URIs are:
 * <ul>
 * <li>{@code foo:/bar/}
 * <li>{@code foo:bar:/baz!/}
 * <li>{@code foo:bar:baz:/bang!/boom!/}
 * </ul>
 * Examples for invalid mount point URIs are:
 * <ul>
 * <li>{@code foo} (not absolute)
 * <li>{@code foo:/bar/#baz} (fragment)
 * <li>{@code foo:bar:/baz!/bang} (doesn't end with bang slash separator)
 * <li>{@code foo:bar:baz:/bang!/!/} (empty entry name in bar:baz:/bang!/)
 * </ul>
 * <p>
 * Note that this class is immutable and final, hence thread-safe, too.
 *
 * @see     Path
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MountPoint implements Serializable, Comparable<MountPoint> {

    private static final long serialVersionUID = 5723957985634276648L;

    private final URI uri;
    private final Path path;
    private volatile transient Scheme scheme;
    private volatile transient URI hierarchical;

    /**
     * Equivalent to {@link #create(URI, boolean) create(uri, false)}.
     */
    public static MountPoint create(URI uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     * This static factory method calls
     * {@link #MountPoint(URI, boolean) new MountPoint(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A non-{@code null} mount point.
     */
    public static MountPoint create(URI uri, boolean normalize) {
        try {
            return new MountPoint(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #MountPoint(URI, boolean) new MountPoint(uri, false)}.
     */
    public MountPoint(URI uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public MountPoint(URI uri, final boolean normalize)
    throws URISyntaxException {
        if (null != uri.getRawFragment())
            throw new URISyntaxException(uri.toString(), "Fragment not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getSchemeSpecificPart();
            final int i = ssp.lastIndexOf(BANG_SLASH);
            if (ssp.length() - 2 != i)
                throw new URISyntaxException(uri.toString(),
                        "Doesn't end with separator \"" + BANG_SLASH + '"');
            path = new Path(new URI(ssp.substring(0, i)), normalize);
            final URI pathUri = path.getUri();
            if (!pathUri.isAbsolute())
                throw new URISyntaxException(uri.toString(), "Path not absolute");
            final String pathEntryNameUriPath
                    = path.getEntryName().getUri().getPath();
            if (0 == pathEntryNameUriPath.length()
                    || pathEntryNameUriPath.endsWith(SEPARATOR))
                throw new URISyntaxException(uri.toString(), "Illegal entry name");
            if (normalize) {
                final URI nuri = new URI(
                        uri.getScheme(), pathUri.toString() + BANG_SLASH, null);
                uri = uri.equals(nuri) ? uri : nuri;
            } else if (pathUri.normalize() != pathUri)
                throw new URISyntaxException(uri.toString(),
                        "URI path not in normal form");
        } else {
            if (!uri.isAbsolute())
                throw new URISyntaxException(uri.toString(), "Not absolute");
            if (normalize)
                uri = uri.normalize();
            else if (uri.normalize() != uri)
                throw new URISyntaxException(uri.toString(),
                        "URI path not in normal form");
            if (!uri.getRawPath().endsWith(SEPARATOR))
                throw new URISyntaxException(uri.toString(),
                        "URI path doesn't end with separator \"" + SEPARATOR + '"');
            path = null;
        }
        this.uri = uri;

        assert invariants();
    }

    /**
     * Constructs a new mount point by synthesizing its URI from the given
     * scheme and path.
     *
     * @param  scheme the non-{@code null} {@link #getScheme() scheme}.
     * @param  path the non-{@code null} {@link #getPath() path}.
     * @throws IllegalArgumentException if the synthesized mount point URI
     *         would not conform to the syntax constraints for mount points.
     * @return A non-{@code null} mount point.
     */
    public static MountPoint create(Scheme scheme, Path path) {
        try {
            return new MountPoint(scheme, path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new mount point by synthesizing its URI from the given
     * scheme and path.
     *
     * @param  scheme the non-{@code null} {@link #getScheme() scheme}.
     * @param  path the non-{@code null} {@link #getPath() path}.
     * @throws URISyntaxException if the synthesized mount point URI
     *         would not conform to the syntax constraints for mount points.
     */
    public MountPoint(final Scheme scheme, final Path path)
    throws URISyntaxException {
        final URI pathUri = path.getUri();
        if (!pathUri.isAbsolute())
            throw new URISyntaxException(pathUri.toString(), "Path not absolute");
        final String pathEntryNameUriPath = path.getEntryName().getUri().getPath();
        if (0 == pathEntryNameUriPath.length()
                || pathEntryNameUriPath.endsWith(SEPARATOR))
            throw new URISyntaxException(pathUri.toString(), "Illegal entry name");
        this.uri = new URI(scheme.toString(), path.toString() + BANG_SLASH, null);
        this.path = path;
        this.scheme = scheme;

        assert invariants();
    }

    private boolean invariants() {
        assert null != getUri();
        assert getUri().isAbsolute();
        assert null == getUri().getRawFragment();
        if (getUri().isOpaque()) {
            assert getUri().getRawSchemeSpecificPart().endsWith(BANG_SLASH);
            assert null != getPath();
            assert getPath().getUri().isAbsolute();
            assert null == getPath().getUri().getRawFragment();
            assert 0 != getPath().getEntryName().getUri().getPath().length();
            assert !getPath().getEntryName().getUri().getPath().endsWith(SEPARATOR);
            assert 0 != getPath().getEntryName().toString().length();
        } else {
            assert getUri().normalize() == getUri();
            assert getUri().getRawPath().endsWith(SEPARATOR);
            assert null == getPath();
        }
        return true;
    }

    /**
     * Returns the non-{@code null} URI scheme.
     *
     * @return The non-{@code null} URI scheme.
     */
    public Scheme getScheme() {
        return null != scheme
                ? scheme
                : (scheme = Scheme.create(uri.getScheme()));
    }

    /**
     * Returns the path or {@code null} iff the {@link #getUri() URI}
     * is hierarchical.
     *
     * @return The nullable path.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the nullable parent mount point, i.e. the mount point of the
     * parent file system or {@code null} iff this mount point does not name
     * a parent mount point.
     * 
     * @return The nullable parent mount point.
     */
    public MountPoint getParent() {
        return null == path ? null : path.getMountPoint();
    }

    /**
     * Resolves the given entry name against the entry name of this mount
     * point in its parent file system.
     *
     * @param  entryName a non-{@code null} entry name relative to this mount
     *         point.
     * @throws NullPointerException if {@code entryName} is {@code null} or if
     *         this mount point does not name a parent mount point.
     * @return A non-{@code null} entry name relative to the parent mount
     *         point.
     * @see    #getParent
     */
    public FileSystemEntryName resolveParent(FileSystemEntryName entryName) {
        return new FileSystemEntryName(path.getEntryName(), entryName);
    }

    /**
     * Resolves the given entry name against the path of this mount point.
     *
     * @param  entryName a non-{@code null} entry name relative to this mount
     *         point.
     * @throws NullPointerException if {@code entryName} is {@code null}.
     * @return A non-{@code null} path with an absolute URI.
     */
    public Path resolveAbsolute(FileSystemEntryName entryName) {
        return new Path(this, entryName);
    }

    URI hierarchicalize() {
        if (null == path)
            return uri;
        if (null != hierarchical)
            return hierarchical;
        URI entry = path.getEntryName().getUri();
        if (!entry.getRawPath().endsWith(SEPARATOR)) {
            try {
                entry = new URI(null, null, entry.getPath() + SEPARATOR_CHAR, entry.getQuery(), null);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }
        return hierarchical = path.getMountPoint().hierarchicalize().resolve(entry);
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
     * Returns {@code true} iff the given object is a mount point and its URI
     * {@link URI#equals(Object) equals} the URI of this mount point.
     * Note that this ignores the scheme and path.
     */
    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof MountPoint
                    && this.uri.equals(((MountPoint) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(MountPoint that) {
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
