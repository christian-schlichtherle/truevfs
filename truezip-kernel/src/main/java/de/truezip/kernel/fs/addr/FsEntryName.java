/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.addr;

import de.truezip.kernel.fs.FsEntry;
import static de.truezip.kernel.fs.addr.FsUriModifier.NULL;
import static de.truezip.kernel.fs.addr.FsUriModifier.PostFix.ENTRY_NAME;
import de.truezip.kernel.util.QuotedUriSyntaxException;
import de.truezip.kernel.util.UriBuilder;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Addresses a file system entry relative to its {@link FsMountPoint mount point}.
 * 
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * An entry name adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must be relative, that is it must not define a scheme component.
 * <li>The URI must not define an authority component.
 * <li>The URI must define a path component.
 * <li>The URI's path must be in normal form, that is its path component must
 *     not contain redundant {@code "."} and {@code ".."} segments.
 * <li>The URI's path component must not equal {@code "."}.
 * <li>The URI's path component must not equal {@code ".."}.
 * <li>The URI's path component must not start with {@code "/"}.
 * <li>The URI's path component must not start with {@code "./"}
 *     (this rule is actually redundant - see #3).
 * <li>The URI's path component must not start with {@code "../"}.
 * <li>The URI's path component must not end with {@code "/"}.
 * <li>The URI must not define a fragment component.
 * </ol>
 * 
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid entry name URIs are:
 * <ul>
 * <li>{@code "foo"}
 * <li>{@code "foo/bar"}
 * </ul>
 * <table border=1 cellpadding=5 summary="">
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
 * </tbody>
 * </table>
 * <p>
 * Examples for invalid entry name URIs are:
 * <table border=1 cellpadding=5 summary="">
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
 * {@code new FsEntryName(e.toUri()).equals(e)}.
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
 */
@Immutable
public final class FsEntryName
implements Serializable, Comparable<FsEntryName> {

    private static final long serialVersionUID = 3453442253468244275L;

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

    private static final String ILLEGAL_PREFIX = ".." + SEPARATOR;

    /**
     * The file system entry name of the root directory,
     * which is an empty URI.
     */
    public static final FsEntryName ROOT;
    static {
        try {
            ROOT = new FsEntryName(new URI(""));
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
    private URI uri; // not final for serialization only!

    /**
     * Constructs a new file system entry name by parsing the given URI.
     * This static factory method calls
     * {@link #FsEntryName(URI, FsUriModifier) new FsEntryName(uri, FsUriModifier.NULL)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #toUri() URI}.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     * @return A new file system entry name.
     */
    public static FsEntryName
    create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI
     * after applying the given URI modifier.
     * This static factory method calls
     * {@link #FsEntryName(URI, FsUriModifier) new FsEntryName(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws NullPointerException if {@code uri} or {@code modifier} are
     *         {@code null}.
     * @throws IllegalArgumentException if {@code uri} still does not conform
     *         to the syntax constraints for file system entry names after its
     *         modification.
     * @return A new file system entry name.
     */
    public static FsEntryName
    create(URI uri, FsUriModifier modifier) {
        try {
            return uri.toString().isEmpty()
                    ? ROOT
                    : new FsEntryName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     *
     * @param  uri the {@link #toUri() URI}.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     */
    public FsEntryName(URI uri) throws URISyntaxException {
        this(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI
     * after applying the given URI modifier.
     *
     * @param  uri the {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws NullPointerException if {@code uri} or {@code modifier} are
     *         {@code null}.
     * @throws URISyntaxException if {@code uri} still does not conform to the
     *         syntax constraints for file system entry names after its
     *         modification.
     */
    public FsEntryName(URI uri, final FsUriModifier modifier)
    throws URISyntaxException {
        parse(modifier.modify(uri, ENTRY_NAME));
    }

    private void writeObject(ObjectOutputStream out)
    throws IOException {
        out.writeObject(uri.toString());
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        try {
            parse(new URI(in.readObject().toString())); // protect against manipulation
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(final URI uri) throws URISyntaxException {
        if (uri.isAbsolute())
            throw new QuotedUriSyntaxException(uri, "Scheme component defined.");
        if (null != uri.getRawAuthority())
            throw new QuotedUriSyntaxException(uri, "Authority component defined.");
        if (null == uri.getRawPath())
            throw new QuotedUriSyntaxException(uri, "Path component undefined.");
        if (null != uri.getRawFragment())
            throw new QuotedUriSyntaxException(uri, "Fragment component defined.");
        this.uri = uri;
        final String p = uri.getRawPath();
        if (p.startsWith(SEPARATOR))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal start of URI path component");
        if (!p.isEmpty() && ILLEGAL_PREFIX.startsWith(p.substring(0,
                Math.min(p.length(), ILLEGAL_PREFIX.length()))))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal start of URI path component");
        if (p.endsWith(SEPARATOR))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal separator \"" + SEPARATOR + "\" at end of URI path");

        assert invariants();
    }

    /**
     * Constructs a new file system entry name by resolving the given member
     * file system entry name against the given parent file system entry name.
     * Note that the URI of the parent file system entry name is always
     * considered to name a directory, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * file system entry names results in {@code "foo/bar"} as the file system
     * entry name URI.
     *
     * @param  parent an entry name for the parent.
     * @param  member an entry name for the member.
     */
    public FsEntryName( final FsEntryName parent,
                        final FsEntryName member) {
        final URI pu = parent.uri;
        final String pup = pu.getRawPath();
        final URI mu = member.uri;
        try {
            uri = pup.isEmpty()
                    ? mu
                    : pup.endsWith(SEPARATOR)
                        ? pu.resolve(mu)
                        : mu.getPath().isEmpty()
                            ? new UriBuilder(pu, true)
                                .query(mu.getRawQuery())
                                .getUri()
                            : new UriBuilder(true)
                                .path(pup + SEPARATOR_CHAR)
                                .getUri()
                                .resolve(mu);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }

        assert invariants();
    }

    private boolean invariants() {
        assert null != toUri();
        assert !toUri().isAbsolute();
        assert null == toUri().getRawAuthority();
        assert null != toUri().getRawPath();
        assert null == toUri().getRawFragment();
        assert toUri().normalize() == toUri();
        String p = toUri().getRawPath();
        assert !"..".equals(p);
        assert !p.startsWith(SEPARATOR);
        assert !p.startsWith("." + SEPARATOR);
        assert !p.startsWith(".." + SEPARATOR);
        assert !p.endsWith(SEPARATOR);
        return true;
    }

    /**
     * Returns {@code true} if and only if the path component of this file
     *         system entry name is empty and no query component is defined.
     * 
     * @return {@code true} if and only if the path component of this file
     *         system entry name is empty and no query component is defined.
     */
    public boolean isRoot() {
        //return toUri().toString().isEmpty();
        final URI uri = toUri();
        final String path = uri.getRawPath();
        if (null != path && !path.isEmpty())
            return false;
        final String query = uri.getRawQuery();
        if (null != query)
            return false;
        return true;
    }

    /**
     * Returns the URI for this entry name.
     *
     * @return The URI for this entry name.
     */
    public URI toUri() {
        return uri;
    }

    /**
     * Returns the path of this entry name.
     * Equivalent to {@link #toUri() toUri()}{@code .getPath()}.
     *
     * @return The path of this entry name.
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * Returns the query of this entry name.
     * Equivalent to {@link #toUri() toUri()}{@code .getQuery()}.
     *
     * @return The query of this entry name.
     */
    public @CheckForNull String getQuery() {
        return uri.getQuery();
    }

    /**
     * Returns the fragment of this entry name.
     * Equivalent to {@link #toUri() toUri()}{@code .getFragment()}.
     *
     * @return The fragment of this entry name.
     */
    public @CheckForNull String getFragment() {
        return uri.getFragment();
    }

    /**
     * Returns {@code true} iff the given object is a entry name
     * and its URI {@link URI#equals(Object) equals} the URI of this entry name.
     * 
     * @param that the object to compare.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof FsEntryName
                    && this.uri.equals(((FsEntryName) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     * 
     * @param that the entry name to compare.
     */
    @Override
    public int compareTo(FsEntryName that) {
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
     * Equivalent to calling {@link URI#toString()} on {@link #toUri()}.
     */
    @Override
    public String toString() {
        return uri.toString();
    }
}