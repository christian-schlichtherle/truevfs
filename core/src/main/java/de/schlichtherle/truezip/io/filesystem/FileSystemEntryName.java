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
 * Addresses an entry in a file system relative to its mount point.
 * A file system entry name is usually constructed from a
 * {@link URI Uniform Resource Identifier} in order to assert the following
 * additional syntax constraints:
 * <ol>
 * <li>The URI must be relative, i.e. it must not have a scheme.
 * <li>The URI must not have an authority.
 * <li>The URI must not have a fragment.
 * <li>The URI's path must be in normal form, i.e. it must not contain
 *     redundant {@code "."} and {@code ".."} segments.
 * <li>The URI's path must not equal {@code "."}.
 * <li>The URI's path must not equal {@code ".."}.
 * <li>The URI's path must not start with {@code "/"}.
 * <li>The URI's path must not start with {@code "./"}.
 * <li>The URI's path must not start with {@code "../"}.
 * <li>The URI's path must not end with {@code "/"}.
 * </ol>
 * <p>
 * Examples for valid file system entry name URIs are:
 * <ul>
 * <li>{@code foo:bar:/baz!/bang} (mountPoint="bar:/baz", entryName="bang")
 * <li>{@code foo:/bar/} (there are no constraints for hierarchical URIs.
 * </ul>
 * Examples for invalid path URIs are:
 * <ul>
 * <li>{@code foo:bar} (opaque URI w/o bang slash separator)
 * <li>{@code foo:bar:baz:/bang!/} (dito)
 * </ul>
 * <p>
 * Note that this class is immutable and final, hence thread-safe, too.
 *
 * @see     FileSystemEntry#getName()
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileSystemEntryName implements Serializable, Comparable<FileSystemEntryName> {

    private static final long serialVersionUID = 2212342253466752478L;

    /** Represents a file system entry name with an empty URI. */
    public static final FileSystemEntryName ROOT
            = FileSystemEntryName.create(URI.create(FileSystemEntry.ROOT));

    private final URI uri;

    /**
     * Equivalent to {@link #create(String, String, boolean) create(path, null, false)}.
     */
    public static FileSystemEntryName create(String path) {
        return create(path, null, false);
    }

    /**
     * Equivalent to {@link #create(String, String, boolean) create(path, null, normalize)}.
     */
    public static FileSystemEntryName create(String path, boolean normalize) {
        return create(path, null, normalize);
    }

    /**
     * Constructs a new file system entry name by constructing a new URI from
     * the given path and query elements and parsing the result.
     * This static factory method calls
     * {@link #FileSystemEntryName(URI, boolean) new FileSystemEntryName(new URI(null, null, path, query, null), normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     * @return A non-{@code null} file system entry name.
     */
    public static FileSystemEntryName create(String path, String query, boolean normalize) {
        try {
            return new FileSystemEntryName(new URI(null, null, path, query, null), normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** Equivalent to {@link #create(URI, boolean) create(uri, false)}. */
    public static FileSystemEntryName create(URI uri) {
        return create(uri, false);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     * This static factory method calls
     * {@link #FileSystemEntryName(URI, boolean) new FileSystemEntryName(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     * @return A non-{@code null} file system entry name.
     */
    public static FileSystemEntryName create(URI uri, boolean normalize) {
        try {
            return new FileSystemEntryName(uri, normalize);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FileSystemEntryName(URI, boolean) new FileSystemEntryName(uri, false)}.
     */
    public FileSystemEntryName(URI uri) throws URISyntaxException {
        this(uri, false);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #getUri() URI}.
     * @param  normalize whether or not the given URI shall get normalized
     *         before parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     */
    public FileSystemEntryName(URI uri, final boolean normalize)
    throws URISyntaxException {
        if (uri.isAbsolute())
            throw new URISyntaxException(uri.toString(), "Scheme not allowed");
        if (uri.getRawAuthority() != null)
            throw new URISyntaxException(uri.toString(), "Authority not allowed");
        if (null != uri.getRawFragment())
            throw new URISyntaxException(uri.toString(), "Fragment not allowed");
        if (normalize)
            uri = uri.normalize();
        else if (uri.normalize() != uri)
            throw new URISyntaxException(uri.toString(), "URI path not in normal form");
        final String p = uri.getRawPath();
        if (    "..".equals(p)
                || p.startsWith(SEPARATOR)
                || p.startsWith("." + SEPARATOR)
                || p.startsWith(".." + SEPARATOR))
            throw new URISyntaxException(uri.toString(),
                    "Illegal start of URI path");
        if (p.endsWith(SEPARATOR))
            throw new URISyntaxException(uri.toString(),
                    "Illegal separator \"" + SEPARATOR + "\" at end of URI path");
        this.uri = uri;

        assert invariants();
    }

    /**
     * Constructs a new file system entry name by resolving the given member
     * file system entry name against the given parent file system entry name.
     * Note that the URI of the parent file system entry name is considered to
     * name a directory even if it's not ending with a
     * {@link FileSystemEntry#SEPARATOR}, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * file system entry names respectively will result in the URI
     * {@code "foo/bar"} for the resulting file system entry name.
     *
     * @param  parent a non-{@code null} file system entry name.
     * @param  member a non-{@code null} file system entry name.
     * @throws NullPointerException if any parameter is {@code null}.
     */
    FileSystemEntryName(final FileSystemEntryName parent, final FileSystemEntryName member) {
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
        assert getUri().normalize() == getUri();
        String p = getUri().getRawPath();
        assert !"..".equals(p);
        assert !p.startsWith(SEPARATOR);
        assert !p.startsWith("." + SEPARATOR);
        assert !p.startsWith(".." + SEPARATOR);
        assert !p.endsWith(SEPARATOR);
        return true;
    }

    /** Equivalent to {@link #getUri()}{@code .getPath()}. */
    public String getPath() {
        return uri.getPath();
    }

    /** Equivalent to {@link #getUri()}{@code .getQuery()}. */
    public String getQuery() {
        return uri.getQuery();
    }

    /**
     * Returns the non-{@code null} URI of this file system entry name.
     *
     * @return The non-{@code null} URI of this file system entry name.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns {@code true} iff the given object is a file system entry name
     * and its URI {@link URI#equals(Object) equals} the URI of this file
     * system entry name.
     */
    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof FileSystemEntryName
                    && this.uri.equals(((FileSystemEntryName) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(FileSystemEntryName that) {
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
