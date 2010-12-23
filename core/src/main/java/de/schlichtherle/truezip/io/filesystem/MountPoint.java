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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.io.filesystem.FileSystemEntryName.*;
import static de.schlichtherle.truezip.io.filesystem.Path.MOUNT_POINT_SEPARATOR;

/**
 * Addresses the mount point of a file system.
 * A mount point is usually constructed from a
 * {@link URI Uniform Resource Identifier} in order to assert the following
 * additional syntax constraints:
 * <ol>
 * <li>The URI must be absolute.
 * <li>The URI must not have a fragment.
 * <li>If the URI is opaque, its scheme specific part must end with the mount
 *     point separator {@code "!/"}.
 *     The scheme specific part <em>before</em> this mount point separator is
 *     parsed according the syntax constraints for a {@link Path} and the
 *     following additional syntax constraints:
 *     The path must be absolute.
 *     If its opaque, it's entry name must not be empty.
 * <li>If the URI is hierarchical, its path must be in normal form and end with
 *     a {@link FileSystemEntryName#SEPARATOR}.
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
 * <li>{@code /foo} (not absolute)
 * <li>{@code foo} (dito)
 * <li>{@code foo:/bar/#baz} (fragment)
 * <li>{@code foo:bar:/baz!/bang} (doesn't end with mount point separator)
 * <li>{@code foo:bar:baz:/bang!/!/} (empty entry name in bar:baz:/bang!/)
 * </ul>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     Path
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS", "SE_TRANSIENT_FIELD_NOT_RESTORED" })
public final class MountPoint implements Serializable, Comparable<MountPoint> {

    private static final long serialVersionUID = 5723957985634276648L;

    @NonNull
    private URI uri; // not final for serialization only!

    @Nullable
    private transient Path path;

    private volatile transient Scheme scheme;

    /**
     * Equivalent to {@link #create(URI, boolean) create(uri, false)}.
     */
    @NonNull
    public static MountPoint create(@NonNull String uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new mount point by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #MountPoint(String, boolean) new MountPoint(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     */
    @NonNull
    public static MountPoint create(@NonNull String uri, boolean normalize) {
        try {
            return new MountPoint(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, boolean) create(uri, false)}.
     */
    @NonNull
    public static MountPoint create(@NonNull URI uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     * This static factory method calls
     * {@link #MountPoint(URI, boolean) new MountPoint(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     */
    @NonNull
    public static MountPoint create(@NonNull URI uri, boolean normalize) {
        try {
            return new MountPoint(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new mount point by synthesizing its URI from the given
     * scheme and path.
     * This static factory method calls
     * {@link #MountPoint(Scheme, Path) new MountPoint(scheme, path)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  scheme the {@link #getScheme() scheme}.
     * @param  path the {@link #getPath() path}.
     * @throws IllegalArgumentException if the synthesized mount point URI
     *         would not conform to the syntax constraints for mount points.
     * @return A new mount point.
     */
    @NonNull
    public static MountPoint create(@NonNull Scheme scheme, @NonNull Path path) {
        try {
            return new MountPoint(scheme, path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #MountPoint(String, boolean) new MountPoint(uri, false)}.
     */
    public MountPoint(@NonNull String uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new path by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public MountPoint(@NonNull String uri, boolean normalize)
    throws URISyntaxException {
        parse(uri, normalize);
    }

    /**
     * Equivalent to {@link #MountPoint(URI, boolean) new MountPoint(uri, false)}.
     */
    public MountPoint(@NonNull URI uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public MountPoint(@NonNull URI uri, boolean normalize)
    throws URISyntaxException {
        parse(uri, normalize);
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
    public MountPoint(@NonNull final Scheme scheme, @NonNull final Path path)
    throws URISyntaxException {
        final URI pathUri = path.getUri();
        if (!pathUri.isAbsolute())
            throw new URISyntaxException(pathUri.toString(), "Path not absolute");
        final String pathEntryNameUriPath = path.getEntryName().getUri().getPath();
        if (0 == pathEntryNameUriPath.length())
            throw new URISyntaxException(pathUri.toString(), "Empty entry name");
        this.uri = new URI(new StringBuilder(scheme.toString())
                .append(':')
                .append(path.toString())
                .append(MOUNT_POINT_SEPARATOR)
                .toString());
        this.path = path;
        this.scheme = scheme;

        assert invariants();
    }

    private void writeObject(@NonNull ObjectOutputStream out)
    throws IOException {
        out.writeObject(uri.toString());
    }

    private void readObject(@NonNull ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        try {
            parse(in.readObject().toString(), false);
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    private void parse(@NonNull String uri, final boolean normalize)
    throws URISyntaxException {
        parse(new URI(uri), normalize);
    }

    private void parse(@NonNull URI uri, final boolean normalize)
    throws URISyntaxException {
        if (null != uri.getRawFragment())
            throw new URISyntaxException(uri.toString(), "Fragment not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getRawSchemeSpecificPart();
            final int i = ssp.lastIndexOf(MOUNT_POINT_SEPARATOR);
            if (ssp.length() - 2 != i)
                throw new URISyntaxException(uri.toString(),
                        "Doesn't end with mount point separator \"" + MOUNT_POINT_SEPARATOR + '"');
            path = new Path(ssp.substring(0, i), normalize);
            final URI pathUri = path.getUri();
            if (!pathUri.isAbsolute())
                throw new URISyntaxException(uri.toString(), "Path not absolute");
            if (0 == path.getEntryName().getPath().length())
                throw new URISyntaxException(uri.toString(), "Empty entry name");
            if (normalize) {
                final URI nuri = new URI(new StringBuilder(uri.getScheme())
                        .append(':')
                        .append(pathUri.toString())
                        .append(MOUNT_POINT_SEPARATOR)
                        .toString());
                if (!uri.equals(nuri))
                    uri = nuri;
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

    private boolean invariants() {
        assert null != getUri();
        assert getUri().isAbsolute();
        assert null == getUri().getRawFragment();
        if (getUri().isOpaque()) {
            assert getUri().getRawSchemeSpecificPart().endsWith(MOUNT_POINT_SEPARATOR);
            assert null != getPath();
            assert getPath().getUri().isAbsolute();
            assert null == getPath().getUri().getRawFragment();
            assert 0 != getPath().getEntryName().getUri().getRawPath().length();
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
    @NonNull
    public Scheme getScheme() {
        return null != scheme ? scheme : (scheme = Scheme.create(uri.getScheme()));
    }

    /**
     * Returns the path or {@code null} iff the {@link #getUri() URI}
     * is hierarchical.
     *
     * @return The nullable path.
     */
    @Nullable
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
    @Nullable
    public MountPoint getParent() {
        assert null == path || null != path.getMountPoint();
        return null == path ? null : path.getMountPoint();
    }

    /**
     * Resolves the given entry name against the entry name of this mount
     * point in its parent file system.
     *
     * @param  entryName an entry name relative to this mount point.
     * @throws NullPointerException if {@code entryName} is {@code null} or if
     *         this mount point does not name a parent mount point.
     * @return A new entry name relative to the parent mount point.
     * @see    #getParent
     */
    @NonNull
    public FileSystemEntryName resolveParent(@NonNull FileSystemEntryName entryName) {
        return new FileSystemEntryName(path.getEntryName(), entryName);
    }

    /**
     * Resolves the given entry name against the path of this mount point.
     *
     * @param  entryName an entry name relative to this mount point.
     * @return A new path with an absolute URI.
     */
    @NonNull
    public Path resolvePath(@NonNull FileSystemEntryName entryName) {
        return new Path(this, entryName);
    }

    /**
     * Returns a mount point which has its URI converted from the URI of
     * this mount point so that it's absolute and hierarchical.
     * If this mount point is already in hierarchical form, it's returned.
     * <p>
     * For example, the mount point URIs {@code zip:file:/archive!/} and
     * {@code tar:file:/archive!/} will both produce an equal mount point
     * with the absolute, hierarchical URI {@code file:/archive/}.
     *
     * @return A mount point which has its URI converted from the URI of
     *         this mount point so that it's absolute and hierarchical.
     */
    @NonNull
    public MountPoint hierarchicalize() {
        if (uri.isOpaque()) {
            final URI uri = path.hierarchicalize().getUri();
            try {
                return new MountPoint(new URI(
                        uri.getScheme(), uri.getAuthority(),
                        uri.getPath() + SEPARATOR_CHAR,
                        uri.getQuery(), null));
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            return this;
        }
    }

    /**
     * Returns the non-{@code null} URI.
     *
     * @return The non-{@code null} URI.
     */
    @NonNull
    public URI getUri() {
        return uri;
    }

    /**
     * Returns {@code true} iff the given object is a mount point and its URI
     * {@link URI#equals(Object) equals} the URI of this mount point.
     * Note that this ignores the scheme and path.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof MountPoint
                    && this.uri.equals(((MountPoint) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(@NonNull MountPoint that) {
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
    @NonNull
    public String toString() {
        return uri.toString();
    }
}
