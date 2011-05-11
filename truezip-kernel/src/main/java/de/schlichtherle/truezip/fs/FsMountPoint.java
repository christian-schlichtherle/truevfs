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

import de.schlichtherle.truezip.entry.EntryName;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsPath.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.PostFix.*;

/**
 * Addresses the mount point of a file system.
 * 
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * A mount point adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must be absolute.
 * <li>The URI must not have a fragment.
 * <li>If the URI is opaque, its scheme specific part must end with the mount
 *     point separator {@code "!/"}.
 *     The scheme specific part <em>before</em> the mount point separator is
 *     parsed according the syntax constraints for a {@link FsPath} and the
 *     following additional syntax constraints:
 *     The path must be absolute.
 *     If its opaque, it's entry name must not be empty.
 *     Finally, its set as the value of the component property
 *     {@link #getPath() path}.
 * <li>Otherwise, if the URI is hierarchical, its path must be in normal form
 *     and end with a {@code "/"}.
 *     The {@link #getPath() path} component property of the mount point is set
 *     to {@code null} in this case.
 * </ol>
 * 
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid mount point URIs are:
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 *   <th>{@link #getUri() uri} property</th>
 *   <th>{@link #getScheme() scheme} property</th>
 *   <th>{@link #getPath() path} URI</th>
 *   <th>{@link #getParent() parent} URI</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo:/bar/}</td>
 *   <td>{@code foo}</td>
 *   <td>(n/a)<sup>*</sup></td>
 *   <td>(n/a)<sup>*</sup></td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:/baz!/}</td>
 *   <td>{@code foo}</td>
 *   <td>{@code bar:/baz}</td>
 *   <td>{@code bar:/}</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:baz:/bang!/boom!/}</td>
 *   <td>{@code foo}</td>
 *   <td>{@code bar:baz:/bang!/boom}</td>
 *   <td>{@code baz:/bang}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * <sup>*</sup> the component property is {@code null} and hence its URI is not
 * available.
 * <p>
 * Examples for invalid mount point URIs are:
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 *   <th>URI</th>
 *   <th>Issue</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code /foo}</td>
 *   <td>not an absolute URI</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar}</td>
 *   <td>missing slash at end of hierarchical URI</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar/#baz}</td>
 *   <td>fragment component defined</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:/baz!/bang}</td>
 *   <td>missing mount point separator {@code "!/"} at end</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:baz:/bang!/!/}</td>
 *   <td>empty entry name in path component after mount point {@code "bar:baz:/bang!/"}</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <a name="identities"/><h3>Identities</h3>
 * <p>
 * For any mount point {@code m}, it's generally true that
 * {@code new FsMountPoint(m.getUri()).equals(m)}.
 * <p>
 * For any mount point {@code m} with an opaque URI, it's generally true that
 * {@code new FsMountPoint(m.getScheme(), m.getPath()).equals(m)}.
 * 
 * <a name="serialization"/><h3>Serialization</h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     FsPath
 * @see     FsEntryName
 * @see     FsScheme
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS", "SE_TRANSIENT_FIELD_NOT_RESTORED" })
public final class FsMountPoint implements Serializable, Comparable<FsMountPoint> {

    private static final long serialVersionUID = 5723957985634276648L;

    /**
     * The separator which is used to split opaque path names into
     * {@link FsMountPoint mount points} and {@link EntryName entry names}.
     * This is identical to the separator in the class
     * {@link java.net.JarURLConnection}.
     */
    public static final String SEPARATOR = "!" + FsEntryName.SEPARATOR;

    private URI uri; // not final for serialization only!

    private transient @Nullable FsPath path;

    private transient volatile @Nullable FsScheme scheme;

    private transient volatile @Nullable FsMountPoint hierarchical;

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsMountPoint
    create(String uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new mount point by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FsMountPoint(String, FsUriModifier) new FsMountPoint(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     */
    public static FsMountPoint
    create(String uri, FsUriModifier modifier) {
        try {
            return new FsMountPoint(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsMountPoint
    create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     * This static factory method calls
     * {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     */
    public static FsMountPoint
    create(URI uri, FsUriModifier modifier) {
        try {
            return new FsMountPoint(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new mount point by composing its URI from the given scheme
     * and path.
     * This static factory method calls
     * {@link #FsMountPoint(FsScheme, FsPath) new FsMountPoint(scheme, path)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  scheme the {@link #getScheme() scheme}.
     * @param  path the {@link #getPath() path}.
     * @throws IllegalArgumentException if the composed mount point URI would
     *         not conform to the syntax constraints for mount points.
     * @return A new mount point.
     */
    public static FsMountPoint
    create(FsScheme scheme, FsPath path) {
        try {
            return new FsMountPoint(scheme, path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsMountPoint(String, FsUriModifier) new FsMountPoint(uri, FsUriModifier.NULL)}.
     */
    public FsMountPoint(String uri) throws URISyntaxException {
        parse(new URI(uri), NULL);
    }

    /**
     * Constructs a new path by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public FsMountPoint(String uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(new URI(uri), modifier);
    }

    /**
     * Equivalent to {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, FsUriModifier.NULL)}.
     */
    public FsMountPoint(URI uri) throws URISyntaxException {
        parse(uri, NULL);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     *
     * @param  uri the {@link #getUri() URI}.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public FsMountPoint(URI uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Constructs a new opaque mount point by composing its URI from the given
     * scheme and path.
     *
     * @param  scheme the non-{@code null} {@link #getScheme() scheme}.
     * @param  path the non-{@code null} {@link #getPath() path}.
     * @throws URISyntaxException if the composed mount point URI would not
     *         conform to the syntax constraints for mount points.
     */
    public FsMountPoint(final FsScheme scheme,
                        final FsPath path)
    throws URISyntaxException {
        final URI pathUri = path.getUri();
        if (!pathUri.isAbsolute())
            throw new URISyntaxException(quote(pathUri), "Path not absolute");
        final String pathEntryNameUriPath = path.getEntryName().getUri().getPath();
        if (0 == pathEntryNameUriPath.length())
            throw new URISyntaxException(quote(pathUri), "Empty entry name");
        this.uri = new URI(new StringBuilder(scheme.toString())
                .append(':')
                .append(path.toString())
                .append(SEPARATOR)
                .toString());
        this.scheme = scheme;
        this.path = path;

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
        uri = modifier.modify(uri, MOUNT_POINT);
        if (null != uri.getRawFragment())
            throw new URISyntaxException(quote(uri), "Fragment not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getRawSchemeSpecificPart();
            final int i = ssp.lastIndexOf(SEPARATOR);
            if (ssp.length() - 2 != i)
                throw new URISyntaxException(quote(uri),
                        "Doesn't end with mount point separator \"" + SEPARATOR + '"');
            path = new FsPath(ssp.substring(0, i), modifier);
            final URI pathUri = path.getUri();
            if (!pathUri.isAbsolute())
                throw new URISyntaxException(quote(uri), "Path not absolute");
            if (0 == path.getEntryName().getPath().length())
                throw new URISyntaxException(quote(uri), "Empty URI path of entry name of path");
            if (NULL != modifier) {
                URI nuri = new URI(new StringBuilder(uri.getScheme())
                        .append(':')
                        .append(pathUri.toString())
                        .append(SEPARATOR)
                        .toString());
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else {
            if (!uri.isAbsolute())
                throw new URISyntaxException(quote(uri), "Not absolute");
            if (!uri.getRawPath().endsWith(FsEntryName.SEPARATOR))
                throw new URISyntaxException(quote(uri),
                        "URI path doesn't end with separator \"" + FsEntryName.SEPARATOR + '"');
            path = null;
        }
        this.uri = uri;

        assert invariants();
    }

    private static String quote(Object s) {
        return "\"" + s + "\"";
    }

    private boolean invariants() {
        assert null != getUri();
        assert getUri().isAbsolute();
        assert null == getUri().getRawFragment();
        if (getUri().isOpaque()) {
            assert getUri().getRawSchemeSpecificPart().endsWith(SEPARATOR);
            assert null != getPath();
            assert getPath().getUri().isAbsolute();
            assert null == getPath().getUri().getRawFragment();
            assert 0 != getPath().getEntryName().getUri().getRawPath().length();
        } else {
            assert getUri().normalize() == getUri();
            assert getUri().getRawPath().endsWith(FsEntryName.SEPARATOR);
            assert null == getPath();
        }
        return true;
    }

    /**
     * Returns the URI of this mount point.
     *
     * @return The URI of this mount point.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns the scheme component.
     *
     * @return The scheme component.
     */
    public FsScheme getScheme() {
        return null != scheme ? scheme : (scheme = FsScheme.create(uri.getScheme()));
    }

    /**
     * Returns the path component
     * or {@code null} iff this mount point's {@link #getUri URI} doesn't name
     * a parent mount point, i.e. if and only if it's hierarchical.
     *
     * @return The nullable path component.
     */
    public @Nullable FsPath getPath() {
        return path;
    }

    /**
     * Returns the parent component, i.e. the mount point of the parent file
     * system,
     * or {@code null} iff this mount point's {@link #getUri URI} doesn't name
     * a parent mount point, i.e. if and only if it's hierarchical.
     * 
     * @return The nullable parent component.
     */
    public @Nullable FsMountPoint getParent() {
        assert null == path || null != path.getMountPoint();
        return null == path ? null : path.getMountPoint();
    }

    /**
     * Resolves the given entry name against this mount point.
     *
     * @param  entryName an entry name relative to this mount point.
     * @return A new path with an absolute URI.
     */
    public FsPath
    resolve(FsEntryName entryName) {
        return new FsPath(this, entryName);
    }

    /**
     * Returns a mount point which has its URI converted from the URI of
     * this mount point so that it's absolute and hierarchical.
     * If this mount point is already in hierarchical form, it's returned.
     * <p>
     * Note that this function is idempotent, so calling it repeatedly will
     * produce the same result again.
     * However, this function is not injective, so two different mount points
     * may produce equal results.
     * For example, the mount point URIs {@code zip:file:/archive!/} and
     * {@code tar:file:/archive!/} would both produce the hierarchicalized
     * mount point with the URI {@code file:/archive/}.
     *
     * @return A mount point which has its URI converted from the URI of
     *         this mount point so that it's absolute and hierarchical.
     */
    public FsMountPoint hierarchicalize() {
        if (null != hierarchical)
            return hierarchical;
        if (uri.isOpaque()) {
            final URI uri = path.hierarchicalize().getUri();
            try {
                return hierarchical = new FsMountPoint(
                        new UriBuilder(uri)
                            .path(uri.getPath() + FsEntryName.SEPARATOR)
                            .getUri());
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            return hierarchical = this;
        }
    }

    /**
     * Returns {@code true} iff the given object is a mount point and its URI
     * {@link URI#equals(Object) equals} the URI of this mount point.
     * Note that this ignores the scheme and path.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof FsMountPoint
                    && this.uri.equals(((FsMountPoint) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(FsMountPoint that) {
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
