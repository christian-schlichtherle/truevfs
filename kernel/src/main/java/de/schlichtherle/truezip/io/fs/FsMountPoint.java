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
package de.schlichtherle.truezip.io.fs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.io.fs.FsEntryName.*;
import static de.schlichtherle.truezip.io.fs.FsPath.MOUNT_POINT_SEPARATOR;

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
 *     parsed according the syntax constraints for a {@link FsPath} and the
 *     following additional syntax constraints:
 *     The path must be absolute.
 *     If its opaque, it's entry name must not be empty.
 * <li>If the URI is hierarchical, its path must be in normal form and end with
 *     a {@link FsEntryName#SEPARATOR}.
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
 * @see     FsPath
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS", "SE_TRANSIENT_FIELD_NOT_RESTORED" })
public final class FsMountPoint implements Serializable, Comparable<FsMountPoint> {

    private static final long serialVersionUID = 5723957985634276648L;

    private @NonNull URI uri; // not final for serialization only!

    private transient @Nullable FsPath path;

    private transient volatile @Nullable FsScheme scheme;

    private transient volatile @Nullable FsMountPoint hierarchical;

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NONE)}.
     */
    public static @NonNull FsMountPoint
    create(@NonNull String uri) {
        return create(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new mount point by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FsMountPoint(String, FsUriModifier) new FsMountPoint(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     */
    public static @NonNull FsMountPoint
    create(@NonNull String uri, @NonNull FsUriModifier modifier) {
        try {
            return new FsMountPoint(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NONE)}.
     */
    public static @NonNull FsMountPoint
    create(@NonNull URI uri) {
        return create(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     * This static factory method calls
     * {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     */
    public static @NonNull FsMountPoint
    create(@NonNull URI uri, @NonNull FsUriModifier modifier) {
        try {
            return new FsMountPoint(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new mount point by synthesizing its URI from the given
     * scheme and path.
     * This static factory method calls
     * {@link #FsMountPoint(FsScheme, FsPath) new FsMountPoint(scheme, path)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  scheme the {@link #getScheme() scheme}.
     * @param  path the {@link #getPath() path}.
     * @throws IllegalArgumentException if the synthesized mount point URI
     *         would not conform to the syntax constraints for mount points.
     * @return A new mount point.
     */
    public static @NonNull FsMountPoint
    create(@NonNull FsScheme scheme, @NonNull FsPath path) {
        try {
            return new FsMountPoint(scheme, path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsMountPoint(String, FsUriModifier) new FsMountPoint(uri, FsUriModifier.NONE)}.
     */
    public FsMountPoint(@NonNull String uri) throws URISyntaxException {
        parse(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new path by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public FsMountPoint(@NonNull String uri, @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Equivalent to {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, FsUriModifier.NONE)}.
     */
    public FsMountPoint(@NonNull URI uri) throws URISyntaxException {
        parse(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public FsMountPoint(@NonNull URI uri, @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
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
    public FsMountPoint(final @NonNull FsScheme scheme,
                        final @NonNull FsPath path)
    throws URISyntaxException {
        final URI pathUri = path.getUri();
        if (!pathUri.isAbsolute())
            throw new URISyntaxException(quote(pathUri), "Path not absolute");
        final String pathEntryNameUriPath = path.getEntryName().getUri().getPath();
        if (0 == pathEntryNameUriPath.length())
            throw new URISyntaxException(quote(pathUri), "Empty entry name");
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
            parse(in.readObject().toString(), FsUriModifier.NONE);
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(@NonNull String uri, @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        parse(new URI(uri), modifier);
    }

    private void parse(@NonNull URI uri, final @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        if (null != uri.getRawFragment())
            throw new URISyntaxException(quote(uri), "Fragment not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getRawSchemeSpecificPart();
            final int i = ssp.lastIndexOf(MOUNT_POINT_SEPARATOR);
            if (ssp.length() - 2 != i)
                throw new URISyntaxException(quote(uri),
                        "Doesn't end with mount point separator \"" + MOUNT_POINT_SEPARATOR + '"');
            path = new FsPath(ssp.substring(0, i), modifier);
            final URI pathUri = path.getUri();
            if (!pathUri.isAbsolute())
                throw new URISyntaxException(quote(uri), "Path not absolute");
            if (0 == path.getEntryName().getPath().length())
                throw new URISyntaxException(quote(uri), "Empty entry name");
            if (FsUriModifier.NONE != modifier) {
                final URI nuri = new URI(new StringBuilder(uri.getScheme())
                        .append(':')
                        .append(pathUri.toString())
                        .append(MOUNT_POINT_SEPARATOR)
                        .toString());
                if (!uri.equals(nuri))
                    uri = nuri;
            } else if (pathUri.normalize() != pathUri)
                throw new URISyntaxException(quote(uri),
                        "URI path not in normal form");
        } else {
            uri = modifier.modify(uri);
            if (!uri.isAbsolute())
                throw new URISyntaxException(quote(uri), "Not absolute");
            if (!uri.getRawPath().endsWith(SEPARATOR))
                throw new URISyntaxException(quote(uri),
                        "URI path doesn't end with separator \"" + SEPARATOR + '"');
            path = null;
        }
        this.uri = uri;

        assert invariants();
    }

    private static String quote(Object s) {
        return "\"" + s + "\"";
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
     * Returns the URI scheme.
     *
     * @return The URI scheme.
     */
    public @NonNull FsScheme getScheme() {
        return null != scheme ? scheme : (scheme = FsScheme.create(uri.getScheme()));
    }

    /**
     * Returns the path or {@code null} iff this mount point's
     * {@link #getUri URI} doesn't name a parent mount point, i.e. if it's
     * hierarchical.
     *
     * @return The nullable path.
     */
    public @Nullable FsPath getPath() {
        return path;
    }

    /**
     * Returns the non-{@code null} URI.
     *
     * @return The non-{@code null} URI.
     */
    public @NonNull URI getUri() {
        return uri;
    }

    /**
     * Returns the nullable parent mount point, i.e. the mount point of the
     * parent file system or {@code null} iff this mount point's
     * {@link #getUri URI} doesn't name a parent mount point, i.e. if it's
     * hierarchical.
     * 
     * @return The nullable parent mount point.
     */
    public @Nullable FsMountPoint getParent() {
        assert null == path || null != path.getMountPoint();
        return null == path ? null : path.getMountPoint();
    }

    /**
     * Resolves the given entry name against this mount point.
     *
     * @param  entryName an entry name relative to this mount point.
     * @return A new path with an absolute URI.
     */
    public @NonNull FsPath
    resolve(@NonNull FsEntryName entryName) {
        return new FsPath(this, entryName);
    }

    /**
     * Returns a mount point which has its URI converted from the URI of
     * this mount point so that it's absolute and hierarchical.
     * If this mount point is already in hierarchical form, it's returned.
     * <p>
     * Note that this function is idempotent, so calling it repeatedly will
     * produce the same result again.
     * However, this function is not injective, so two different mount points
     * may produce equal results.
     * For example, the mount point URIs {@code zip:file:/archive!/} and
     * {@code tar:file:/archive!/} will both produce an equal mount point
     * with the absolute, hierarchical URI {@code file:/archive/}.
     *
     * @return A mount point which has its URI converted from the URI of
     *         this mount point so that it's absolute and hierarchical.
     */
    public @NonNull FsMountPoint hierarchicalize() {
        if (null != hierarchical)
            return hierarchical;
        if (uri.isOpaque()) {
            final URI uri = path.hierarchicalize().getUri();
            try {
                return hierarchical = new FsMountPoint(new URI(
                        uri.getScheme(), uri.getAuthority(),
                        uri.getPath() + SEPARATOR_CHAR,
                        uri.getQuery(), null));
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            return hierarchical = this;
        }
    }

    /**
     * Returns {@code true} iff the given object is a mount point and its URI
     * {@link URI#equals(Object) equals} the URI of this mount point.
     * Note that this ignores the scheme and path.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof FsMountPoint
                    && this.uri.equals(((FsMountPoint) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(@NonNull FsMountPoint that) {
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
    public @NonNull String toString() {
        return uri.toString();
    }
}
