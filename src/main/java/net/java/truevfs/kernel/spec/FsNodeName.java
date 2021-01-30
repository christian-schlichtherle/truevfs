/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.beans.ConstructorProperties;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.java.truecommons.shed.QuotedUriSyntaxException;
import net.java.truecommons.shed.UriBuilder;
import static net.java.truevfs.kernel.spec.FsUriModifier.NULL;
import static net.java.truevfs.kernel.spec.FsUriModifier.PostFix.NODE_NAME;

/**
 * Addresses a file system node relative to its {@link FsMountPoint mount point}.
 * 
 * <h3><a name="specification">Specification</a></h3>
 * <p>
 * An node name adds the following syntax constraints to a
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
 * <h3><a name="examples">Examples</a></h3>
 * <p>
 * Examples for <em>valid</em> node name URIs:
 * </p>
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 *   <th>{@link #getUri() uri} property</th>
 *   <th>{@link #isRoot() root} property</th>
 *   <th>{@link #getPath() path} property</th>
 *   <th>{@link #getQuery() query} property</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code ""}</td>
 *   <td>{@code true}</td>
 *   <td>{@code ""}</td>
 *   <td>{@code null}</td>
 * </tr>
 * <tr>
 *   <td>{@code "foo"}</td>
 *   <td>{@code false}</td>
 *   <td>{@code "foo"}</td>
 *   <td>{@code null}</td>
 * </tr>
 * <tr>
 *   <td>{@code "foo/bar"}</td>
 *   <td>{@code false}</td>
 *   <td>{@code "foo/bar"}</td>
 *   <td>{@code null}</td>
 * </tr>
 * <tr>
 *   <td>{@code "foo?bar"}</td>
 *   <td>{@code false}</td>
 *   <td>{@code "foo"}</td>
 *   <td>{@code "bar"}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * Examples for <em>invalid</em> node name URIs:
 * </p>
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
 * <tr>
 *   <td>{@code foo#bar}</td>
 *   <td>fragment defined</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <h3><a name="identities">Identities</a></h3>
 * <p>
 * For any node name {@code e}, it's generally true that
 * {@code new FsNodeName(e.getUri()).equals(e)}.
 * 
 * <h3><a name="serialization">Serialization</a></h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see    FsNodePath
 * @see    FsMountPoint
 * @see    FsScheme
 * @see    FsNode#getName()
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsNodeName
implements Serializable, Comparable<FsNodeName> {

    private static final long serialVersionUID = 3453442253468244275L;

    /**
     * The separator string for file names in an node name,
     * which is {@value}.
     *
     * @see #SEPARATOR_CHAR
     */
    public static final String SEPARATOR = "/";

    /**
     * The separator character for file names in an node name,
     * which is {@value}.
     *
     * @see #SEPARATOR
     */
    public static final char SEPARATOR_CHAR = '/';

    private static final String DOT_DOT_SEPARATOR = ".." + SEPARATOR;

    /**
     * The file system node name of the root directory,
     * which is an empty URI.
     */
    public static final FsNodeName ROOT;
    static {
        try {
            ROOT = new FsNodeName(new URI(""));
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    @SuppressFBWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
    private URI uri; // not final for serialization only!

    /**
     * Constructs a new file system node name by parsing the given URI.
     * This static factory method calls
     * {@link #FsNodeName(URI, FsUriModifier) new FsNodeName(uri, FsUriModifier.NULL)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for file system node names.
     * @return A new file system node name.
     */
    public static FsNodeName
    create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new file system node name by parsing the given URI
     * after applying the given URI modifier.
     * This static factory method calls
     * {@link #FsNodeName(URI, FsUriModifier) new FsNodeName(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws NullPointerException if {@code uri} or {@code modifier} are
     *         {@code null}.
     * @throws IllegalArgumentException if {@code uri} still does not conform
     *         to the syntax constraints for file system node names after its
     *         modification.
     * @return A new file system node name.
     */
    public static FsNodeName
    create(URI uri, FsUriModifier modifier) {
        try {
            return uri.toString().isEmpty()
                    ? ROOT
                    : new FsNodeName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new file system node name by parsing the given URI.
     *
     * @param  uri the {@link #getUri() URI}.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for file system node names.
     */
    @ConstructorProperties("uri")
    public FsNodeName(URI uri) throws URISyntaxException {
        this(uri, NULL);
    }

    /**
     * Constructs a new file system node name by parsing the given URI
     * after applying the given URI modifier.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws NullPointerException if {@code uri} or {@code modifier} are
     *         {@code null}.
     * @throws URISyntaxException if {@code uri} still does not conform to the
     *         syntax constraints for file system node names after its
     *         modification.
     */
    public FsNodeName(URI uri, final FsUriModifier modifier)
    throws URISyntaxException {
        parse(modifier.modify(uri, NODE_NAME));
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
            throw new QuotedUriSyntaxException(uri, "Scheme component defined");
        if (null != uri.getRawAuthority())
            throw new QuotedUriSyntaxException(uri, "Authority component defined");
        if (null == uri.getRawPath())
            throw new QuotedUriSyntaxException(uri, "Path component undefined");
        if (null != uri.getRawFragment())
            throw new QuotedUriSyntaxException(uri, "Fragment component defined");
        this.uri = uri;
        final String p = uri.getRawPath();
        if (p.startsWith(SEPARATOR))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal start of path component");
        if (!p.isEmpty() && DOT_DOT_SEPARATOR.startsWith(p.substring(0,
                Math.min(p.length(), DOT_DOT_SEPARATOR.length()))))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal start of path component");
        if (p.endsWith(SEPARATOR))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal separator \"" + SEPARATOR + "\" at end of path component");

        assert invariants();
    }

    /**
     * Constructs a new file system node name by resolving the given member
     * file system node name against the given parent file system node name.
     * Note that the URI of the parent file system node name is always
     * considered to name a directory, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * file system node names results in {@code "foo/bar"} as the file system
     * node name URI.
     *
     * @param  parent an node name for the parent.
     * @param  member an node name for the member.
     */
    public FsNodeName( final FsNodeName parent,
                        final FsNodeName member) {
        final URI pu = parent.uri;
        final String pup = pu.getRawPath();
        final URI mu = member.uri;
        try {
            uri = pup.isEmpty()
                    ? mu
                    : pup.endsWith(SEPARATOR)
                        ? pu.resolve(mu)
                        : mu.getPath().isEmpty()
                            ? new UriBuilder(true)
                                .uri(pu)
                                .query(mu.getRawQuery())
                                .toUriChecked()
                            : new UriBuilder(true)
                                .path(pup + SEPARATOR_CHAR)
                                .toUriChecked()
                                .resolve(mu);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }

        assert invariants();
    }

    private boolean invariants() {
        assert null != getUri();
        assert !getUri().isAbsolute();
        assert null == getUri().getRawAuthority();
        assert null != getUri().getRawPath();
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

    /**
     * Returns {@code true} if and only if the path component of this file
     *         system node name is empty and no query component is defined.
     * 
     * @return {@code true} if and only if the path component of this file
     *         system node name is empty and no query component is defined.
     */
    public boolean isRoot() {
        //return getUri().toString().isEmpty();
        final URI uri = getUri();
        final String path = uri.getRawPath();
        if (null != path && !path.isEmpty())
            return false;
        final String query = uri.getRawQuery();
        return null == query;
    }

    /**
     * Returns the URI for this node name.
     *
     * @return The URI for this node name.
     */
    public URI getUri() { return uri; }

    /**
     * Returns the path component of this node name.
     * Equivalent to {@link #getUri() getUri()}{@code .getPath()}.
     *
     * @return The path component of this node name.
     */
    public String getPath() { return uri.getPath(); }

    /**
     * Returns the query component of this node name.
     * Equivalent to {@link #getUri() getUri()}{@code .getQuery()}.
     *
     * @return The query component of this node name.
     */
    public @CheckForNull String getQuery() { return uri.getQuery(); }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     * 
     * @param that the node name to compare.
     */
    @Override
    public int compareTo(FsNodeName that) {
        return this.uri.compareTo(that.uri);
    }

    /**
     * Returns {@code true} iff the given object is a node name
     * and its URI {@link URI#equals(Object) equals} the URI of this node name.
     * 
     * @param that the object to compare.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof FsNodeName
                    && this.uri.equals(((FsNodeName) that).uri);
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() { return uri.hashCode(); }

    /**
     * Equivalent to calling {@link URI#toString()} on {@link #getUri()}.
     */
    @Override
    public String toString() { return uri.toString(); }
}