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
package de.schlichtherle.truezip.entry;

import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

/**
 * Addresses an entry in an entry container.
 * Although this class is declared to be immutable, it's not declared to be
 * final solely for the purpose of adding more constraints in subclass
 * constructors  so that their instances can be safely used as polymorphic
 * instances of this class.
 * 
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * An entry name adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must be relative, i.e. it must not name a scheme.
 * <li>The URI must not have an authority.
 * </ol>
 * 
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid entry name URIs are:
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 *   <th>{@link #toUri() uri} property</th>
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
 * <tr>
 *   <td>{@code ../foo/./#bar}</td>
 *   <td>{@code ../foo/./}</td>
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
 * </tbody>
 * </table>
 * 
 * <a name="identities"/><h3>Identities</h3>
 * <p>
 * For any entry name {@code e}, it's generally true that
 * {@code new EntryName(e.toUri()).equals(e)}.
 * 
 * <a name="serialization"/><h3>Serialization</h3>
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
@DefaultAnnotation(NonNull.class)
public class EntryName implements Serializable, Comparable<EntryName> {

    private static final long serialVersionUID = 2927354934726235478L;

    /**
     * The separator string for file names in an entry name,
     * which is {@value}.
     *
     * @see #SEPARATOR_CHAR
     */
    public static final String SEPARATOR = "/";

    /**
     * The separator character for file names in an entry name,
     * which is {@value}.
     *
     * @see #SEPARATOR
     */
    public static final char SEPARATOR_CHAR = '/';

    private URI uri; // not final for serialization only!

    /**
     * Constructs a new entry name by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #EntryName(String) new EntryName(uri)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new entry name.
     */
    public static EntryName
    create(String uri) {
        try {
            return new EntryName(uri);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new entry name by parsing the given URI.
     * This static factory method calls
     * {@link #EntryName(URI) new EntryName(uri)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #toUri() URI}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new entry name.
     */
    public static EntryName
    create(URI uri) {
        try {
            return new EntryName(uri);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new entry name by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     */
    public EntryName(String uri) throws URISyntaxException {
        parse(new URI(uri));
    }

    /**
     * Constructs a new entry name by parsing the given URI.
     *
     * @param  uri the {@link #toUri() URI}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     */
    public EntryName(URI uri)
    throws URISyntaxException {
        parse(uri);
    }

    /**
     * Constructs a new entry name by resolving the given member
     * entry name against the given parent entry name.
     * Note that the URI of the parent entry name is considered to
     * name a directory even if it's not ending with a
     * {@link #SEPARATOR_CHAR}, so calling this constructor with
     * {@code foo} and {@code bar} as the URIs for the parent and member
     * entry names will produce the URI {@code foo/bar} for the resulting
     * entry name.
     *
     * @param  parent an entry name for the parent.
     * @param  member an entry name for the member.
     */
    public EntryName(   final EntryName parent,
                        final EntryName member) {
        final URI parentUri = parent.uri;
        final String parentUriPath = parentUri.getPath();
        final URI memberUri = member.uri;
        try {
            uri = parentUriPath.isEmpty()
                    ? memberUri
                    : parentUriPath.endsWith(SEPARATOR)
                        ? parentUri.resolve(memberUri)
                        : memberUri.getPath().isEmpty()
                            ? new UriBuilder(parentUri)
                                .query(memberUri.getQuery())
                                .fragment(memberUri.getFragment())
                                .getUri()
                            : new UriBuilder()
                                .path(parentUriPath + SEPARATOR_CHAR)
                                .getUri()
                                .resolve(memberUri);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }

        assert invariants();
    }

    private void writeObject(ObjectOutputStream out)
    throws IOException {
        out.writeObject(uri.toString());
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        try {
            parse(new URI(in.readObject().toString()));
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(final URI uri) throws URISyntaxException {
        if (uri.isAbsolute())
            throw new URISyntaxException(quote(uri), "Scheme defined");
        if (uri.getRawAuthority() != null)
            throw new URISyntaxException(quote(uri), "Authority defined");
        /*if (null != uri.getRawFragment())
            throw new URISyntaxException(quote(uri), "Fragment defined");*/
        this.uri = uri;

        assert invariants();
    }

    private static String quote(Object s) {
        return "\"" + s + "\"";
    }

    private boolean invariants() {
        assert null != toUri();
        assert !toUri().isAbsolute();
        assert null == toUri().getRawAuthority();
        //assert null == toUri().getRawFragment();
        return true;
    }

    /**
     * Returns the URI for this entry name.
     *
     * @return The URI for this entry name.
     * @since  TrueZIP 7.1.1
     */
    public final URI toUri() {
        return uri;
    }

    /**
     * @deprecated
     * @see #toUri()
     */
    public final URI getUri() {
        return uri;
    }

    /**
     * Returns the path of this entry name.
     * Equivalent to {@link #toUri() toUri()}{@code .getPath()}.
     *
     * @return The path of this entry name.
     */
    public final String getPath() {
        return uri.getPath();
    }

    /**
     * Returns the query of this entry name.
     * Equivalent to {@link #toUri() toUri()}{@code .getQuery()}.
     *
     * @return The query of this entry name.
     */
    public final @CheckForNull String getQuery() {
        return uri.getQuery();
    }

    /**
     * Returns the fragment of this entry name.
     * Equivalent to {@link #toUri() toUri()}{@code .getFragment()}.
     *
     * @return The fragment of this entry name.
     */
    public final @CheckForNull String getFragment() {
        return uri.getFragment();
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
     * Equivalent to calling {@link URI#toString()} on {@link #toUri()}.
     */
    @Override
    public final String toString() {
        return uri.toString();
    }
}
