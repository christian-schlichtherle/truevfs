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

import de.schlichtherle.truezip.util.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.PostFix.*;

/**
 * Addresses an entry in a file system.
 * 
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * A path adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>If the URI is opaque, its scheme specific part must contain at least
 *     one mount point separator {@code "!/"}.
 *     The part <em>up to</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsMountPoint} and set
 *     as the value of the component property
 *     {@link #getMountPoint() mountPoint}.
 *     The part <em>after</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsEntryName} and set
 *     as the value of the component property {@link #getEntryName() entryName}.
 * <li>Otherwise, if the URI is absolute, it's resolved with {@code "."},
 *     parsed according to the syntax constraints for an {@link FsMountPoint}
 *     and set as the value of the component property
 *     {@link #getMountPoint() mountPoint}.
 *     The URI relativized to this mount point is parsed according to the
 *     syntax constraints for an {@link FsEntryName} and set as the value of
 *     the component property {@link #getEntryName() entryName}.
 * <li>Otherwise, the value of the component property
 *     {@link #getMountPoint() mountPoint} is set to {@code null} and the URI
 *     is parsed according to the syntax constraints for an {@link FsEntryName}
 *     and set as the value of the component property
 *     {@link #getEntryName() entryName}.
 * </ol>
 * For opaque URIs of the form {@code jar:<url>!/<entry>}, these constraints
 * build a close subset of the syntax allowed by a
 * {@link java.net.JarURLConnection}.
 * 
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid path URIs are:
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 *   <th>{@link #getUri() uri} property</th>
 *   <th>{@link #getMountPoint() mountPoint} URI</th>
 *   <th>{@link #getEntryName() entryName} URI</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo}</td>
 *   <td>(null)</td>
 *   <td>{@code foo}</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar}</td>
 *   <td>{@code foo:/}</td>
 *   <td>{@code bar}</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar/}</td>
 *   <td>{@code foo:/bar}</td>
 *   <td>(empty - not null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:/baz!/bang}</td>
 *   <td>{@code foo:bar:/baz!/}</td>
 *   <td>{@code bang}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * Examples for invalid path URIs are:
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
 *   <td>leading slash separator not allowed if URI is not absolute</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/}</td>
 *   <td>trailing slash separator not allowed if URI is not absolute</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar}</td>
 *   <td>missing mount point separator in opaque URI</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:baz:/bang!/boom}</td>
 *   <td>dito for {@code bar:baz:/bang}</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <a name="identities"/><h3>Identities</h3>
 * <p>
 * For any path {@code p}, it's generally true that
 * {@code new FsPath(p.getUri()).equals(p)}.
 * <p>
 * Furthermore, it's generally true that
 * {@code new FsPath(p.getMountPoint(), p.getEntryName()).equals(p)}.
 * 
 * <a name="serialization"/><h3>Serialization</h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     FsMountPoint
 * @see     FsEntryName
 * @see     FsScheme
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class FsPath implements Serializable, Comparable<FsPath> {

    private static final long serialVersionUID = 5798435461242930648L;

    private static final URI DOT = URI.create(".");

    private URI uri; // not final for serialization only!

    private transient @Nullable FsMountPoint mountPoint;

    private transient FsEntryName entryName;

    private transient volatile @Nullable URI hierarchical;

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
        URI mountPointUri;
        if (null == mountPoint) {
            this.uri = entryName.getUri();
        } else if ((mountPointUri = mountPoint.getUri()).isOpaque()) {
            try {
                this.uri = new URI(mountPointUri.toString() + entryName);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            this.uri = mountPointUri.resolve(entryName.getUri());
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
            final int i = ssp.lastIndexOf(FsMountPoint.SEPARATOR);
            if (0 > i)
                throw new URISyntaxException(quote(uri),
                        "Missing mount point separator \"" + FsMountPoint.SEPARATOR + '"');
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
            assert getUri().getRawSchemeSpecificPart().contains(FsMountPoint.SEPARATOR);
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
     * Returns the URI of this path.
     *
     * @return The URI of this path.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns a URI which is recursively transformed from the URI of this
     * path so that it's absolute and hierarchical.
     * If this path is already in hierarchical form, its URI is returned.
     * <p>
     * For example, the path URIs {@code zip:file:/archive!/entry} and
     * {@code tar:file:/archive!/entry} would both produce the hierarchical URI
     * {@code file:/archive/entry}.
     *
     * @return A URI which is recursively transformed from the URI of this
     *         path so that it's absolute and hierarchical.
     */
    public URI getHierarchicalUri() {
        if (null != hierarchical)
            return hierarchical;
        if (uri.isOpaque()) {
            final URI mpu = mountPoint.getHierarchicalUri();
            final URI enu = entryName.getUri();
            try {
                return hierarchical = enu.toString().isEmpty()
                        ? mpu
                        : new UriBuilder(mpu)
                            .path(mpu.getPath() + FsEntryName.SEPARATOR)
                            .getUri()
                            .resolve(enu);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            return hierarchical = uri;
        }
    }

    /**
     * Returns the mount point component or {@code null} iff this path's
     * {@link #getUri() URI} is not absolute.
     *
     * @return The nullable mount point.
     */
    public @Nullable FsMountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the entry name component.
     * This may be empty, but is never {@code null}.
     *
     * @return The entry name.
     */
    public FsEntryName getEntryName() {
        return entryName;
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
