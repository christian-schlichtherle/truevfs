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

import de.schlichtherle.truezip.io.entry.EntryName;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.io.fs.FSEntryName.SEPARATOR;

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
 * <li>{@code foo}
 * <li>{@code foo/bar}
 * </ul>
 * Examples for invalid file system entry name URIs are:
 * <ul>
 * <li>{@code /foo} (leading slash separator not allowed)
 * <li>{@code foo/} (trailing slash separator not allowed)
 * <li>{@code foo/.} (not normalized)
 * <li>{@code foo/..} (not normalized)
 * </ul>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     FSEntry#getName()
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FSEntryName extends EntryName {
    private static final long serialVersionUID = 2212342253466752478L;

    /**
     * The file system entry name of the root directory,
     * which is an empty URI.
     */
    public static final FSEntryName ROOT
            = FSEntryName.create(URI.create(""));

    /**
     * Equivalent to {@link #create(String, boolean) create(uri, false)}.
     */
    public static @NonNull FSEntryName
    create(@NonNull String uri) {
        return create(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new file system entry name by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FSEntryName(String, boolean) new FSEntryName(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new file system entry name.
     */
    public static @NonNull FSEntryName
    create(@NonNull String uri, @NonNull FsUriModifier modifier) {
        try {
            return new FSEntryName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(String, String, boolean) create(path, query, false)}.
     */
    public static @NonNull FSEntryName
    create(@NonNull String path, @CheckForNull String query) {
        return create(path, query, FsUriModifier.NONE);
    }

    /**
     * Constructs a new file system entry name by constructing a new URI from
     * the given path and query elements and parsing the result.
     * This static factory method calls
     * {@link #FSEntryName(URI, boolean) new FSEntryName(new URI(null, null, path, query, null), normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  path the {@link #getPath() path}.
     * @param  query the {@link #getQuery() query}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new file system entry name.
     */
    public static @NonNull FSEntryName
    create(@NonNull String path, @CheckForNull String query, @NonNull FsUriModifier modifier) {
        try {
            return new FSEntryName(new URI(null, null, path, query, null), modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** Equivalent to {@link #create(URI, boolean) create(uri, false)}. */
    public static @NonNull FSEntryName
    create(@NonNull URI uri) {
        return create(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     * This static factory method calls
     * {@link #FSEntryName(URI, boolean) new FSEntryName(uri, normalize)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new file system entry name.
     */
    public static @NonNull FSEntryName
    create(@NonNull URI uri, @NonNull FsUriModifier modifier) {
        try {
            return new FSEntryName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FSEntryName(String, boolean) new FSEntryName(uri, false)}.
     */
    public FSEntryName(@NonNull String uri) throws URISyntaxException {
        this(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new file system entry name by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     */
    public FSEntryName(@NonNull String uri, @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        this(new URI(uri), modifier);
    }

    /**
     * Equivalent to {@link #FSEntryName(URI, boolean) new FSEntryName(uri, false)}.
     */
    public FSEntryName(@NonNull URI uri) throws URISyntaxException {
        this(uri, FsUriModifier.NONE);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  normalize whether or not the URI shall get normalized before
     *         parsing it.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     */
    public FSEntryName(@NonNull URI uri, final @NonNull FsUriModifier modifier)
    throws URISyntaxException {
        super(uri = modifier.modify(uri));

        final String p = uri.getRawPath();
        if (       "..".equals(p)
                || p.startsWith(SEPARATOR)
                || p.startsWith("." + SEPARATOR)
                || p.startsWith(".." + SEPARATOR))
            throw new URISyntaxException(quote(uri),
                    "Illegal start of URI path");
        if (p.endsWith(SEPARATOR))
            throw new URISyntaxException(quote(uri),
                    "Illegal separator \"" + SEPARATOR + "\" at end of URI path");

        assert invariants();
    }

    private static @NonNull String quote(@NonNull Object s) {
        return "\"" + s + "\"";
    }

    /**
     * Constructs a new file system entry name by resolving the given member
     * file system entry name against the given parent file system entry name.
     * Note that the URI of the parent file system entry name is considered to
     * name a directory even if it's not ending with a
     * {@link FSEntryName#SEPARATOR}, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * file system entry names respectively will result in the URI
     * {@code "foo/bar"} for the resulting file system entry name.
     *
     * @param  parent an entry name for the parent.
     * @param  member an entry name for the member.
     */
    FSEntryName(final @NonNull FSEntryName parent,
                final @NonNull FSEntryName member) {
        super(parent, member);

        assert invariants();
    }

    private boolean invariants() {
        assert null != getUri();
        //assert !getUri().isAbsolute();
        //assert null == getUri().getRawAuthority();
        //assert null == getUri().getRawFragment();
        assert getUri().normalize() == getUri();
        String p = getUri().getRawPath();
        assert !"..".equals(p);
        assert !p.startsWith(SEPARATOR);
        assert !p.startsWith("." + SEPARATOR);
        assert !p.startsWith(".." + SEPARATOR);
        assert !p.endsWith(SEPARATOR);
        return true;
    }

    public boolean isRoot() {
        //return getUri().toString().isEmpty();
        final URI uri = getUri();
        final String path = uri.getRawPath();
        if (null != path && !path.isEmpty())
            return false;
        final String query = uri.getRawQuery();
        if (null != query && !query.isEmpty())
            return false;
        return true;
    }
}
