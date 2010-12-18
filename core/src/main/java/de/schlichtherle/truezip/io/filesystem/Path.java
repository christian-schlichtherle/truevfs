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
import de.schlichtherle.truezip.io.entry.EntryName;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.io.filesystem.FileSystemEntryName.SEPARATOR;

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
 *     according to the syntax constraints for a {@link MountPoint} and set as
 *     the value of the property {@link #getMountPoint() mount point}.
 *     The part <em>after</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FileSystemEntryName} and set as
 *     the value of the property {@link #getEntryName() entry name}.
 * <li>If the URI is absolute, it's resolved with ".", parsed according to
 *     the syntax constraints for a {@link MountPoint} and set as the value of
 *     the property {@link #getMountPoint() mount point}.
 *     The value of the property {@link #getEntryName() entry name} is then set
 *     to the URI relativized to this {@link #getMountPoint() mount point}.
 * <li>Otherwise, the value of the property
 *     {@link #getMountPoint() mount point} is set to {@code null} and the URI
 *     is parsed according to the syntax constraints for an {@link FileSystemEntryName}
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
public final class Path implements Serializable, Comparable<Path> {

    private static final long serialVersionUID = 5798435461242930648L;

    /**
     * The separator which is used to split opaque path names into
     * {@link MountPoint mount points} and {@link EntryName entry names}.
     */
    public static final String MOUNT_POINT_SEPARATOR = "!" + SEPARATOR;

    @NonNull
    private URI uri; // not final for serialization only!

    @CheckForNull
    private transient MountPoint mountPoint;

    @NonNull
    private transient FileSystemEntryName entryName;

    /**
     * Equivalent to {@link #create(String, boolean) create(uri, false)}.
     */
    public static Path create(@NonNull String uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new path by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #Path(String, boolean) new Path(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @return A new path.
     */
    public static Path create(@NonNull String uri, boolean normalize) {
        try {
            return new Path(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, boolean) create(uri, false)}.
     */
    public static Path create(@NonNull URI uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new path by parsing the given URI.
     * This static factory method calls
     * {@link #Path(URI, boolean) new Path(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @return A new path.
     */
    public static Path create(@NonNull URI uri, boolean normalize) {
        try {
            return new Path(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #Path(String, boolean) new Path(uri, false)}.
     */
    public Path(@NonNull String uri) throws URISyntaxException {
        parse(uri, false);
    }

    /**
     * Constructs a new path by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     */
    public Path(@NonNull String uri, boolean normalize) throws URISyntaxException {
        parse(uri, normalize);
    }

    /**
     * Equivalent to {@link #Path(URI, boolean) new Path(uri, false)}.
     */
    public Path(@NonNull URI uri) throws URISyntaxException {
        parse(uri, false);
    }

    /**
     * Constructs a new path by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     */
    public Path(@NonNull URI uri, boolean normalize)
    throws URISyntaxException {
        parse(uri, normalize);
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
    public Path(@CheckForNull final MountPoint mountPoint,
                @NonNull final FileSystemEntryName entryName) {
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
            parse(in.readObject().toString(), false);
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    private void parse(@NonNull String uri, boolean normalize)
    throws URISyntaxException {
        parse(new URI(uri), normalize);
    }

    private void parse(@NonNull URI uri, final boolean normalize)
    throws URISyntaxException {
        if (null != uri.getRawFragment())
            throw new URISyntaxException(uri.toString(), "Fragment not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getSchemeSpecificPart();
            final int i = ssp.lastIndexOf(MOUNT_POINT_SEPARATOR);
            if (0 > i)
                throw new URISyntaxException(uri.toString(),
                        "Missing mount point separator \"" + MOUNT_POINT_SEPARATOR + '"');
            mountPoint = new MountPoint(
                    new URI(uri.getScheme(), ssp.substring(0, i + 2), null),
                    normalize);
            entryName = new FileSystemEntryName(
                    new URI(null, ssp.substring(i + 2), uri.getFragment()),
                    normalize);
            if (normalize) {
                final URI nuri = new URI(   mountPoint.toString()
                                            + entryName.toString());
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else if (uri.isAbsolute()) {
            if (normalize)
                uri = uri.normalize();
            else if (uri.normalize() != uri)
                throw new URISyntaxException(uri.toString(),
                        "URI path not in normal form");
            mountPoint = new MountPoint(uri.resolve("."));
            entryName = new FileSystemEntryName(mountPoint.getUri().relativize(uri));
        } else {
            mountPoint = null;
            entryName = new FileSystemEntryName(uri, normalize);
            if (normalize)
                uri = entryName.getUri();
        }
        this.uri = uri;

        assert invariants();
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
     * Returns the mount point or {@code null} iff the {@link #getUri() URI}
     * is hierarchical.
     *
     * @return The nullable mount point.
     */
    @CheckForNull
    public MountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the entry name.
     * This may be empty, but is never {@code null}.
     *
     * @return The entry name.
     */
    @NonNull
    public FileSystemEntryName getEntryName() {
        return entryName;
    }

    /**
     * Returns the URI of this path.
     *
     * @return The URI of this path.
     */
    @NonNull
    public URI getUri() {
        return uri;
    }

    /**
     * Returns {@code true} iff the given object is a path name and its URI
     * {@link URI#equals(Object) equals} the URI of this path name.
     * Note that this ignores the mount point and entry name.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof Path
                    && this.uri.equals(((Path) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(@NonNull Path that) {
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
