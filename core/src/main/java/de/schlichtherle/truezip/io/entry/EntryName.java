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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR;

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
 * Note that this class is designed to be immutable and hence thread-safe.
 * However, although all its visible state is set in the constructors and all
 * methods are declared final, this class is not declared final itself solely
 * in order to enable subclassing for the purpose of adding even more
 * constraints while still being able to use this class as a polymorph base
 * class.
 *
 * @see     Entry#getName()
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class EntryName implements Serializable, Comparable<EntryName> {

    private static final long serialVersionUID = 2927354934726235478L;

    /** Represents an entry name with an empty URI. */
    //public static final EntryName ROOT = EntryName.create(URI.create(Entry.ROOT));

    private final URI uri;

    /**
     * Equivalent to {@link #create(String, String, boolean) create(path, null, false)}.
     */
    public static EntryName create(String path) {
        return create(path, null, false);
    }

    /**
     * Equivalent to {@link #create(String, String, boolean) create(path, null, normalize)}.
     */
    public static EntryName create(String path, boolean normalize) {
        return create(path, null, normalize);
    }

    /**
     * Constructs a new entry name by constructing a new URI from
     * the given path and query elements and parsing the result.
     * This static factory method calls
     * {@link #EntryName(URI, boolean) new EntryName(new URI(null, null, path, query, null), normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  path the non-{@code null} {@link #getPath() path}.
     * @param  query the nullable {@link #getQuery() query}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A non-{@code null} entry name.
     */
    public static EntryName create(String path, String query, boolean normalize) {
        try {
            return new EntryName(new URI(null, null, path, query, null), normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** Equivalent to {@link #create(URI, boolean) create(uri, false)}. */
    public static EntryName create(URI uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new entry name by parsing the given URI.
     * This static factory method calls
     * {@link #EntryName(URI, boolean) new EntryName(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A non-{@code null} entry name.
     */
    public static EntryName create(URI uri, boolean normalize) {
        try {
            return new EntryName(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #EntryName(URI, boolean) new EntryName(uri, false)}.
     */
    public EntryName(URI uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new entry name by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     */
    public EntryName(URI uri, final boolean normalize)
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
     * {@link Entry#SEPARATOR}, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * entry names respectively will result in the URI
     * {@code "foo/bar"} for the resulting entry name.
     *
     * @param  parent a non-{@code null} entry name.
     * @param  member a non-{@code null} entry name.
     * @throws NullPointerException if any parameter is {@code null}.
     */
    public EntryName(final EntryName parent, final EntryName member) {
        final URI parentUri = parent.uri;
        final URI memberUri = member.uri;
        try {
            uri = 0 == parentUri.getPath().length()
                    ? memberUri
                    : 0 == memberUri.getPath().length()
                        ? parentUri
                        : new URI(null, null, parentUri.getPath() + SEPARATOR, parentUri.getQuery(), null)
                            .resolve(memberUri);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
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
     * Returns the non-{@code null} path of this entry name.
     * Equivalent to {@link #getUri()}{@code .getPath()}.
     *
     * @return The non-{@code null} path of this entry name.
     */
    public final String getPath() {
        return uri.getPath();
    }

    /**
     * Returns the nullable query of this entry name.
     * Equivalent to {@link #getUri()}{@code .getQuery()}.
     *
     * @return The nullable query of this entry name.
     */
    public final String getQuery() {
        return uri.getQuery();
    }

    /**
     * Returns the non-{@code null} URI of this entry name.
     *
     * @return The non-{@code null} URI of this entry name.
     */
    public final URI getUri() {
        return uri;
    }

    /**
     * Returns {@code true} iff the given object is a entry name
     * and its URI {@link URI#equals(Object) equals} the URI of this entry name.
     */
    @Override
    public final boolean equals(Object that) {
        return this == that
                || that instanceof EntryName
                    && this.uri.equals(((EntryName) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public final int compareTo(EntryName that) {
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
    public final String toString() {
        return uri.toString();
    }
}
