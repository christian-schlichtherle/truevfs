/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import de.schlichtherle.truezip.entry.EntryName;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.PostFix.*;

/**
 * Addresses an entry in a file system.
 * A path is usually constructed from a {@link URI Uniform Resource Identifier}
 * in order to assert the following additional syntax constraints:
 * <p>
 * <ol>
 * <li>If the URI is opaque, its scheme specific part must contain at least
 *     one mount point separator {@value de.schlichtherle.truezip.fs.FsPath#MOUNT_POINT_SEPARATOR}.
 *     The part <em>up to</em> the last mount point separator is parsed
 *     according to the syntax constraints for a {@link FsMountPoint} and set
 *     as the value of the property {@link #getMountPoint() mount point}.
 *     The part <em>after</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsEntryName} and set as
 *     the value of the property {@link #getEntryName() entry name}.
 * <li>Otherwise, if the URI is absolute, it's resolved with {@code "."},
 *     parsedaccording to the syntax constraints for a {@link FsMountPoint} and
 *     set as the value of the property {@link #getMountPoint() mount point}.
 *     The value of the property {@link #getEntryName() entry name} is then set
 *     to the URI relativized to this {@link #getMountPoint() mount point}.
 * <li>Otherwise, the value of the property
 *     {@link #getMountPoint() mount point} is set to {@code null} and the URI
 *     is parsed according to the syntax constraints for an {@link FsEntryName}
 *     and set as the value of the property {@link #getEntryName() entry name}.
 * </ol>
 * For opaque URIs, the constraints build a close subset of the syntax allowed
 * with a {@link java.net.JarURLConnection}, so that any opaque URL
 * {@link #getUri() obtained} from an instance of this class could be used to
 * create a {@link java.net.JarURLConnection} object.
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
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class FsPath implements Serializable, Comparable<FsPath> {

    private static final long serialVersionUID = 5798435461242930648L;

    /**
     * The separator which is used to split opaque path names into
     * {@link FsMountPoint mount points} and {@link EntryName entry names}.
     * This is identical to the separator in the class
     * {@link java.net.JarURLConnection}.
     */
    public static final String MOUNT_POINT_SEPARATOR = "!" + SEPARATOR;

    private static final URI DOT = URI.create(".");

    private URI uri; // not final for serialization only!

    private transient @Nullable FsMountPoint mountPoint;

    private transient FsEntryName entryName;

    private transient volatile @Nullable FsPath hierarchical;

    /**
     * Equivalent to {@link #create(String, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsPath
    create(String uri) {
        return create(uri, NULL);
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
    public static FsPath
    create(String uri, FsUriModifier modifier) {
        try {
            return new FsPath(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsPath
    create(URI uri) {
        return create(uri, NULL);
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
    public static FsPath
    create(URI uri, FsUriModifier modifier) {
        try {
            return new FsPath(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsPath(String, FsUriModifier) new FsPath(uri, FsUriModifier.NULL)}.
     */
    public FsPath(String uri) throws URISyntaxException {
        parse(new URI(uri), NULL);
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
    public FsPath(String uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(new URI(uri), modifier);
    }

    /**
     * Equivalent to {@link #FsPath(URI, FsUriModifier) new FsPath(file.toURI(), FsUriModifier.CANONICALIZE)}.
     * Note that this constructor is expected not to throw any exceptions.
     */
    public FsPath(File file) {
        try {
            parse(file.toURI(), CANONICALIZE);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Equivalent to {@link #FsPath(URI, FsUriModifier) new FsPath(uri, FsUriModifier.NULL)}.
     */
    public FsPath(URI uri) throws URISyntaxException {
        parse(uri, NULL);
    }

    /**
     * Constructs a new path by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     */
    public FsPath(URI uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Constructs a new path by composing its URI from the given nullable mount
     * point and entry name.
     *
     * @param  mountPoint the nullable {@link #getMountPoint() mount point}.
     * @param  entryName the {@link #getEntryName() entry name}.
     * @throws URISyntaxException if the composed path URI would not conform
     *         to the syntax constraints for paths.
     */
    public FsPath(  final @CheckForNull FsMountPoint mountPoint,
                    final FsEntryName entryName) {
        if (null == mountPoint) {
            this.uri = entryName.getUri();
        } else if (mountPoint.getUri().isOpaque()) {
            try {
                this.uri = new URI(mountPoint.toString() + entryName);
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

    private void writeObject(ObjectOutputStream out)
    throws IOException {
        out.writeObject(uri.toString());
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        try {
            parse(new URI(in.readObject().toString()), NULL);
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(URI uri, final FsUriModifier modifier)
    throws URISyntaxException {
        uri = modifier.modify(uri, PATH);
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
            if (NULL != modifier) {
                URI nuri = new URI(mountPoint.toString() + entryName);
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else if (uri.isAbsolute()) {
            mountPoint = new FsMountPoint(uri.resolve(DOT), NULL);
            entryName = new FsEntryName(mountPoint.getUri().relativize(uri), NULL);
        } else {
            mountPoint = null;
            entryName = new FsEntryName(uri, NULL);
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
    public FsEntryName getEntryName() {
        return entryName;
    }

    /**
     * Returns the URI of this path.
     *
     * @return The URI of this path.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Resolves the given entry name against this path.
     *
     * @param  entryName an entry name relative to this path.
     * @return A new path with an absolute URI.
     */
    public FsPath
    resolve(final FsEntryName entryName) {
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
    public FsPath hierarchicalize() {
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
    public int compareTo(FsPath that) {
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
