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
package de.schlichtherle.truezip.io.entry;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

/**
 * Addresses an entry in an entry container.
 * An entry name is usually constructed from a
 * {@link URI Uniform Resource Identifier} in order to assert the following
 * additional syntax constraints:
 * <ol>
 * <li>The URI must be relative, i.e. it must not have a scheme.
 * <li>The URI must not have an authority.
 * <li>The URI must not have a fragment.
 * </ol>
 * <p>
 * Examples for valid entry name URIs are:
 * <ul>
 * <li>{@code /foo}
 * <li>{@code foo/bar}
 * <li>{@code foo}
 * <li>{@code foo/}
 * <li>{@code foo/.}
 * <li>{@code foo/..}
 * <li>{@code ../foo}
 * </ul>
 * Examples for invalid entry name URIs are:
 * <ul>
 * <li>{@code foo:/bar} (not relative)
 * <li>{@code //foo/bar} (authority defined)
 * <li>{@code foo#bar} (fragment defined)
 * </ul>
 * <p>
 * Although this class is declared to be immutable, it's not declared to be
 * final solely in order to enable subclassing for the purpose of adding even
 * more constraints in the sub class constructor while still being able to use
 * references to this base class polymorphically.
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     Entry#getName()
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public class EntryName implements Serializable, Comparable<EntryName> {

    private static final long serialVersionUID = 2927354934726235478L;

    /**
     * The separator string for base names in an entry name,
     * which is {@value}.
     *
     * @see #SEPARATOR_CHAR
     */
    public static final String SEPARATOR = "/";

    /**
     * The separator character for base names in an entry name,
     * which is {@value}.
     *
     * @see #SEPARATOR
     */
    public static final char SEPARATOR_CHAR = '/';

    @NonNull
    private URI uri; // not final for serialization only!

    /**
     * Equivalent to {@link #create(String, boolean) create(uri, false)}.
     */
    public static @NonNull EntryName create(@NonNull String uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new entry name by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #EntryName(String, boolean) new EntryName(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new entry name.
     */
    public static @NonNull EntryName
    create(@NonNull String uri, boolean normalize) {
        try {
            return new EntryName(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(String, String, boolean) create(path, query, false)}.
     */
    public static @NonNull EntryName
    create(@NonNull String path, @CheckForNull String query) {
        return create(path, query, false);
    }

    /**
     * Constructs a new entry name by constructing a new URI from
     * the given path and query elements and parsing the result.
     * This static factory method calls
     * {@link #EntryName(URI, boolean) new EntryName(new URI(null, null, path, query, null), normalize)}
     * and returns the result.
     *
     * @param  path the {@link #getPath() path}.
     * @param  query the {@link #getQuery() query}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @return A new entry name.
     */
    public static @NonNull EntryName
    create(@NonNull String path, @CheckForNull String query, boolean normalize) {
        try {
            return new EntryName(new URI(null, null, path, query, null), normalize);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /** Equivalent to {@link #create(URI, boolean) create(uri, false)}. */
    public static @NonNull EntryName create(@NonNull URI uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new entry name by parsing the given URI.
     * This static factory method calls
     * {@link #EntryName(URI, boolean) new EntryName(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new entry name.
     */
    public static @NonNull EntryName
    create(@NonNull URI uri, boolean normalize) {
        try {
            return new EntryName(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #EntryName(String, boolean) new EntryName(uri, false)}.
     */
    public EntryName(@NonNull String uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new entry name by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     */
    public EntryName(@NonNull String uri, boolean normalize) throws URISyntaxException {
        this(new URI(uri), false);
    }

    /**
     * Equivalent to {@link #EntryName(URI, boolean) new EntryName(uri, false)}.
     */
    public EntryName(@NonNull URI uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new entry name by parsing the given URI.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     */
    public EntryName(@NonNull URI uri, final boolean normalize)
    throws URISyntaxException {
        if (uri.isAbsolute())
            throw new URISyntaxException(uri.toString(), "Scheme not allowed");
        if (uri.getRawAuthority() != null)
            throw new URISyntaxException(uri.toString(), "Authority not allowed");
        if (null != uri.getRawFragment())
            throw new URISyntaxException(uri.toString(), "Fragment not allowed");
        this.uri = normalize ? uri.normalize() : uri;

        assert invariants();
    }

    /**
     * Constructs a new entry name by resolving the given member
     * entry name against the given parent entry name.
     * Note that the URI of the parent entry name is considered to
     * name a directory even if it's not ending with a
     * {@link #SEPARATOR_CHAR}, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * entry names respectively will result in the URI
     * {@code "foo/bar"} for the resulting entry name.
     *
     * @param  parent an entry name for the parent.
     * @param  member an entry name for the member.
     */
    public EntryName(   @NonNull final EntryName parent,
                        @NonNull final EntryName member) {
        final URI parentUri = parent.uri;
        final String parentUriPath = parentUri.getPath();
        final URI memberUri = member.uri;
        try {
            uri = 0 == parentUriPath.length()
                    ? memberUri
                    : parentUriPath.endsWith(SEPARATOR)
                        ? parentUri.resolve(memberUri)
                        : 0 == memberUri.getPath().length()
                            ? new URI(  null, null,
                                        parentUriPath,
                                        memberUri.getQuery(),
                                        null)
                            : new URI(  null, null,
                                        parentUriPath + SEPARATOR_CHAR,
                                        null, // query is irrelevant!
                                        null).resolve(memberUri);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }

        assert invariants();
    }

    private void writeObject(@NonNull ObjectOutputStream out)
    throws IOException {
        out.writeObject(uri.toString());
    }

    private void readObject(@NonNull ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        try {
            uri = new URI(in.readObject().toString());
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }

        assert invariants();
    }

    private boolean invariants() {
        assert null != getUri();
        assert !getUri().isAbsolute();
        assert null == getUri().getRawAuthority();
        assert null == getUri().getRawFragment();
        return true;
    }

    /**
     * Returns the path of this entry name.
     * Equivalent to {@link #getUri() getUri()}{@code .getPath()}.
     *
     * @return The path of this entry name.
     */
    public final @NonNull String getPath() {
        return uri.getPath();
    }

    /**
     * Returns the query of this entry name.
     * Equivalent to {@link #getUri() getUri()}{@code .getQuery()}.
     *
     * @return The query of this entry name.
     */
    public final @CheckForNull String getQuery() {
        return uri.getQuery();
    }

    /**
     * Returns the URI of this entry name.
     *
     * @return The URI of this entry name.
     */
    public final @NonNull URI getUri() {
        return uri;
    }

    /**
     * Returns {@code true} iff the given object is a entry name
     * and its URI {@link URI#equals(Object) equals} the URI of this entry name.
     */
    @Override
    public final boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof EntryName
                    && this.uri.equals(((EntryName) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public final int compareTo(@NonNull EntryName that) {
        return this.uri.compareTo(that.uri);
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public final int hashCode() {
        return uri.hashCode();
    }

    /**
     * Equivalent to calling {@link URI#toString()} on {@link #getUri()}.
     */
    @Override
    public final @NonNull String toString() {
        return uri.toString();
    }
}
