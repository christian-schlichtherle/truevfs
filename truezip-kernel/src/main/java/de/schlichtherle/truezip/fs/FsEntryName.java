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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import de.schlichtherle.truezip.entry.EntryName;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.PostFix.*;

/**
 * Addresses an entry in a file system relative to its mount point.
 * 
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * An entry name adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must be relative, i.e. it must not name a scheme.
 * <li>The URI must not have an authority.
 * <li>The URI's path must be in normal form, i.e. its path must not contain
 *     redundant {@code "."} and {@code ".."} segments.
 * <li>The URI's path must not equal {@code "."}.
 * <li>The URI's path must not equal {@code ".."}.
 * <li>The URI's path must not start with {@code "/"}.
 * <li>The URI's path must not start with {@code "./"}
 *     (this rule is actually redundant - see #3).
 * <li>The URI's path must not start with {@code "../"}.
 * <li>The URI's path must not end with {@code "/"}.
 * </ol>
 * 
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid entry name URIs are:
 * <ul>
 * <li>{@code "foo"}
 * <li>{@code "foo/bar"}
 * </ul>
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 *   <th>{@link #getUri() uri} property</th>
 *   <th>{@link #getPath() path} property</th>
 *   <th>{@link #getQuery() query} property</th>
 *   <th>{@link #getFragment() fragment} property</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo}</td>
 *   <td>{@code foo}</td>
 *   <td>(null)</td>
 *   <td>(null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/bar}</td>
 *   <td>{@code foo/bar}</td>
 *   <td>(null)</td>
 *   <td>(null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo?bar}</td>
 *   <td>{@code foo}</td>
 *   <td>{@code bar}</td>
 *   <td>(null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo#bar}</td>
 *   <td>{@code foo}</td>
 *   <td>(null)</td>
 *   <td>{@code bar}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * Examples for invalid entry name URIs are:
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 *   <th>URI</th>
 *   <th>Issue</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo:/bar}</td>
 *   <td>not a relative URI</td>
 * </tr>
 * <tr>
 *   <td>{@code //foo/bar}</td>
 *   <td>authority component defined</td>
 * </tr>
 * <tr>
 *   <td>{@code /foo}</td>
 *   <td>leading slash not allowed</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/}</td>
 *   <td>trailing slash not allowed</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/.}</td>
 *   <td>not a normalized URI</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <a name="identities"/><h3>Identities</h3>
 * <p>
 * For any entry name {@code e}, it's generally true that
 * {@code new FsEntryName(e.getUri()).equals(e)}.
 * 
 * <a name="serialization"/><h3>Serialization</h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     FsPath
 * @see     FsMountPoint
 * @see     FsScheme
 * @see     FsEntry#getName()
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class FsEntryName extends EntryName {
    private static final long serialVersionUID = 2212342253466752478L;

    /**
     * The file system entry name of the root directory,
     * which is an empty URI.
     */
    public static final FsEntryName ROOT
            = FsEntryName.create(URI.create(""));

    /**
     * Equivalent to {@link #create(String, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsEntryName
    create(String uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FsEntryName(String, FsUriModifier) new FsEntryName(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new file system entry name.
     */
    public static FsEntryName
    create(String uri, FsUriModifier modifier) {
        try {
            return new FsEntryName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}. */
    public static FsEntryName
    create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     * This static factory method calls
     * {@link #FsEntryName(URI, FsUriModifier) new FsEntryName(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new file system entry name.
     */
    public static FsEntryName
    create(URI uri, FsUriModifier modifier) {
        try {
            return new FsEntryName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsEntryName(String, FsUriModifier) new FsEntryName(uri, FsUriModifier.NULL)}.
     */
    public FsEntryName(String uri) throws URISyntaxException {
        this(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     */
    public FsEntryName(String uri, FsUriModifier modifier)
    throws URISyntaxException {
        this(new URI(uri), modifier);
    }

    /**
     * Equivalent to {@link #FsEntryName(URI, FsUriModifier) new FsEntryName(uri, FsUriModifier.NULL)}.
     */
    public FsEntryName(URI uri) throws URISyntaxException {
        this(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     */
    public FsEntryName(URI uri, final FsUriModifier modifier)
    throws URISyntaxException {
        super(uri = modifier.modify(uri, ENTRY_NAME));
        parse(uri);
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            parse(getUri()); // protect against manipulation
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(final URI uri) throws URISyntaxException {
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

    private static String quote(Object s) {
        return "\"" + s + "\"";
    }

    /**
     * Constructs a new file system entry name by resolving the given member
     * file system entry name against the given parent file system entry name.
     * Note that the URI of the parent file system entry name is considered to
     * name a directory even if it's not ending with a
     * {@link FsEntryName#SEPARATOR}, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * file system entry names respectively will result in the URI
     * {@code "foo/bar"} for the resulting file system entry name.
     *
     * @param  parent an entry name for the parent.
     * @param  member an entry name for the member.
     */
    FsEntryName(final FsEntryName parent,
                final FsEntryName member) {
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
