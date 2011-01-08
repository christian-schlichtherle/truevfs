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
import de.schlichtherle.truezip.io.entry.EntryName;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.io.fs.FsEntryName.*;

/**
 * Addresses an entry in a file system.
 * A path is usually constructed from a {@link URI Uniform Resource Identifier}
 * in order to assert the following additional syntax constraints:
 * <p>
 * <ol>
 * <li>The URI must not have a fragment.
 * <li>If the URI is opaque, its scheme specific part must contain at least
 *     one mount point separator {@code "!/"}.
 *     The part <em>up to</em> the last mount point separator is parsed
 *     according to the syntax constraints for a {@link FsMountPoint} and set as
 *     the value of the property {@link #getMountPoint() mount point}.
 *     The part <em>after</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsEntryName} and set as
 *     the value of the property {@link #getEntryName() entry name}.
 * <li>If the URI is absolute, it's resolved with ".", parsed according to
 *     the syntax constraints for a {@link FsMountPoint} and set as the value of
 *     the property {@link #getMountPoint() mount point}.
 *     The value of the property {@link #getEntryName() entry name} is then set
 *     to the URI relativized to this {@link #getMountPoint() mount point}.
 * <li>Otherwise, the value of the property
 *     {@link #getMountPoint() mount point} is set to {@code null} and the URI
 *     is parsed according to the syntax constraints for an {@link FsEntryName}
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
 * <li>{@code /foo} (leading slash separator not allowed if not absolute)
 * <li>{@code foo/} (trailing slash separator not allowed if not absolute)
 * <li>{@code foo:bar} (opaque URI w/o mount point separator)
 * <li>{@code foo:bar:baz:/bang!/boom} (dito)
 * </ul>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class FsPath implements Serializable, Comparable<FsPath> {

    private static final long serialVersionUID = 5798435461242930648L;

    /**
     * The separator which is used to split opaque path names into
     * {@link FsMountPoint mount points} and {@link EntryName entry names}.
     */
    public static final String MOUNT_POINT_SEPARATOR = "!" + SEPARATOR;

    private @NonNull URI uri; // not final for serialization only!

    private transient @Nullable FsMountPoint mountPoint;

    private transient @NonNull FsEntryName entryName;

    private transient volatile @Nullable FsPath hierarchical;

    /**
     * Equivalent to {@link #create(String, FsUriModifier) create(uri, FsUriModifier.NONE)}.
     */
    public static @NonNull FsPath
    create(@NonNull String uri) {
        return create(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new path by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FsPath(String, FsUriModifier) new FsPath(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @return A new path.
     */
    public static @NonNull FsPath
    create(@NonNull String uri, @NonNull FsUriModifier modifier) {
        try {
            return new FsPath(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NONE)}.
     */
    public static @NonNull FsPath
    create(@NonNull URI uri) {
        return create(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new path by parsing the given URI.
     * This static factory method calls
     * {@link #FsPath(URI, FsUriModifier) new FsPath(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @return A new path.
     */
    public static @NonNull FsPath
    create(@NonNull URI uri, @NonNull FsUriModifier modifier) {
        try {
            return new FsPath(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsPath(String, FsUriModifier) new FsPath(uri, FsUriModifier.NONE)}.
     */
    public FsPath(@NonNull String uri) throws URISyntaxException {
        parse(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new path by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     */
    public FsPath(@NonNull String uri, @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Equivalent to {@link #FsPath(URI, FsUriModifier) new FsPath(uri, FsUriModifier.NONE)}.
     */
    public FsPath(@NonNull URI uri) throws URISyntaxException {
        parse(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new path by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     */
    public FsPath(@NonNull URI uri, @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Constructs a new path by synthesizing its URI from the given
     * nullable mount point and entry name.
     *
     * @param  mountPoint the nullable {@link #getMountPoint() mount point}.
     * @param  entryName the {@link #getEntryName() entry name}.
     * @throws URISyntaxException if the synthesized path URI
     *         would not conform to the syntax constraints for paths.
     */
    public FsPath(  final @CheckForNull FsMountPoint mountPoint,
                    final @NonNull FsEntryName entryName) {
        if (null == mountPoint) {
            this.uri = entryName.getUri();
        } else if (mountPoint.getUri().isOpaque()) {
            try {
                this.uri = new URI(mountPoint.toString() + entryName.toString());
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            this.uri = mountPoint.getUri().resolve(entryName.getUri());
        }
        this.mountPoint = mountPoint;
        this.entryName = entryName;

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
            final String ssp = uri.getSchemeSpecificPart();
            final int i = ssp.lastIndexOf(MOUNT_POINT_SEPARATOR);
            if (0 > i)
                throw new URISyntaxException(quote(uri),
                        "Missing mount point separator \"" + MOUNT_POINT_SEPARATOR + '"');
            mountPoint = new FsMountPoint(
                    new URI(uri.getScheme(), ssp.substring(0, i + 2), null),
                    modifier);
            entryName = new FsEntryName(
                    new URI(null, ssp.substring(i + 2), uri.getFragment()),
                    modifier);
            if (FsUriModifier.NONE != modifier) {
                final URI nuri = new URI(   mountPoint.toString()
                                            + entryName.toString());
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else if (uri.isAbsolute()) {
            uri = modifier.modify(uri);
            mountPoint = new FsMountPoint(uri.resolve("."), FsUriModifier.NONE);
            entryName = new FsEntryName(mountPoint.getUri().relativize(uri), FsUriModifier.NONE);
        } else {
            mountPoint = null;
            entryName = new FsEntryName(uri, modifier);
            uri = entryName.getUri();
        }
        this.uri = uri;

        assert invariants();
    }

    private static String quote(Object s) {
        return "\"" + s + "\"";
    }

    private boolean invariants() {
        assert null != getUri();
        assert null == getUri().getRawFragment();
        assert (null != getMountPoint()) == getUri().isAbsolute();
        assert null != getEntryName();
        if (getUri().isOpaque()) {
            assert getUri().getRawSchemeSpecificPart().contains(MOUNT_POINT_SEPARATOR);
            assert getUri().equals(URI.create(  getMountPoint().getUri().toString()
                                                + getEntryName().getUri().toString()));
        } else if (getUri().isAbsolute()) {
            assert getUri().normalize() == getUri();
            assert getUri().equals(getMountPoint().getUri().resolve(getEntryName().getUri()));
        } else {
            assert getUri().normalize() == getUri();
            assert getEntryName().getUri() == getUri();
        }
        return true;
    }

    /**
     * Returns the mount point or {@code null} iff this path's
     * {@link #getUri() URI} is not absolute.
     *
     * @return The nullable mount point.
     */
    public @Nullable FsMountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the entry name.
     * This may be empty, but is never {@code null}.
     *
     * @return The entry name.
     */
    public @NonNull FsEntryName getEntryName() {
        return entryName;
    }

    /**
     * Returns the URI of this path.
     *
     * @return The URI of this path.
     */
    public @NonNull URI getUri() {
        return uri;
    }

    /**
     * Resolves the given entry name against this path.
     *
     * @param  entryName an entry name relative to this path.
     * @return A new path with an absolute URI.
     */
    public @NonNull FsPath
    resolve(final @NonNull FsEntryName entryName) {
        return new FsPath(
                this.mountPoint,
                new FsEntryName(this.entryName, entryName));
    }

    /**
     * Returns a hierarchical URI for this path.
     * If this path's {@link #getUri() URI} is opaque, the
     * {@link FsMountPoint#hierarchicalize() hierarchical URI} of its
     * {@link #getMountPoint() mount point} with its
     * {@link #getEntryName() entry name} resolved against it is returned.
     * Otherwise, this path is returned.
     * <p>
     * Note that this function is idempotent, so calling it repeatedly will
     * produce the same result again.
     * However, this function is not injective, so two different paths
     * may produce equal results.
     * For example, the path URIs {@code zip:file:/archive!/entry} and
     * {@code tar:file:/archive!/entry} both have the same hierarchical URI
     * {@code file:/archive/entry}.
     *
     * @return A hierarchical URI for this path.
     */
    public @NonNull FsPath hierarchicalize() {
        return null != hierarchical
                ? hierarchical
                : (hierarchical = !uri.isOpaque()
                    ? this
                    : 0 == entryName.toString().length()
                        ? FsPath.create(mountPoint.hierarchicalize().getUri())
                        : mountPoint.hierarchicalize().resolve(entryName));
    }

    /**
     * Returns {@code true} iff the given object is a path name and its URI
     * {@link URI#equals(Object) equals} the URI of this path name.
     * Note that this ignores the mount point and entry name.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof FsPath
                    && this.uri.equals(((FsPath) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(@NonNull FsPath that) {
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
