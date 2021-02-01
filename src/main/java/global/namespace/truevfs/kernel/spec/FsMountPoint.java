/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec;

import global.namespace.truevfs.comp.shed.QuotedUriSyntaxException;
import global.namespace.truevfs.comp.shed.UriBuilder;

import java.beans.ConstructorProperties;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static global.namespace.truevfs.kernel.spec.FsUriModifier.NULL;
import static global.namespace.truevfs.kernel.spec.FsUriModifier.PostFix.MOUNT_POINT;

/**
 * Addresses a file system mount point.
 *
 * <h3><a name="specification">Specification</a></h3>
 * <p>
 * A mount point adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must be absolute, that is it must define a scheme component.
 * <li>The URI must not define a query component.
 * <li>The URI must not define a fragment component.
 * <li>If the URI is opaque, its scheme specific part must end with the mount
 *     point separator {@code "!/"}.
 *     The scheme specific part <em>before</em> the mount point separator is
 *     parsed according the syntax constraints for a {@link FsNodePath} and the
 *     following additional syntax constraints:
 *     The path component must be absolute.
 *     If its opaque, it's node name must not be empty.
 *     Finally, its set as the value of the {@link #getPath() path} component
 *     property.
 * <li>Otherwise, if the URI is hierarchical, its path component must be in
 *     normal form and end with a {@code "/"}.
 *     The {@link #getPath() path} component property of the mount point is set
 *     to {@code null} in this case.
 * </ol>
 *
 * <h3><a name="examples">Examples</a></h3>
 * <p>
 * Examples for <em>valid</em> mount point URIs:
 * <table border=1 cellpadding=5 summary="">
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
 *   <td>{@code "foo:/bar/"}</td>
 *   <td>{@code "foo"}</td>
 *   <td>{@code null}</td>
 *   <td>{@code null}</td>
 * </tr>
 * <tr>
 *   <td>{@code "foo:bar:/baz!/"}</td>
 *   <td>{@code "foo"}</td>
 *   <td>{@code "bar:/baz"}</td>
 *   <td>{@code "bar:/"}</td>
 * </tr>
 * <tr>
 *   <td>{@code "foo:bar:baz:/bang!/boom!/"}</td>
 *   <td>{@code "foo"}</td>
 *   <td>{@code "bar:baz:/bang!/boom"}</td>
 *   <td>{@code "baz:/bang"}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * <sup>*</sup> the property is {@code null} and hence its URI is not available.
 * <p>
 * Examples for <em>invalid</em> mount point URIs:
 * <table border=1 cellpadding=5 summary="">
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
 *   <td>empty node name in path component after mount point {@code "bar:baz:/bang!/"}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <h3><a name="identities">Identities</a></h3>
 * <p>
 * For any mount point {@code m}, it's generally true that
 * {@code new FsMountPoint(m.getUri()).equals(m)}.
 * <p>
 * For any mount point {@code m} with an opaque URI, it's generally true that
 * {@code new FsMountPoint(m.getScheme(), m.getPath()).equals(m)}.
 *
 * <h3><a name="serialization">Serialization</a></h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @author Christian Schlichtherle
 * @see FsNodePath
 * @see FsNodeName
 * @see FsScheme
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class FsMountPoint implements Serializable, Comparable<FsMountPoint> {

    private static final long serialVersionUID = 0;

    /**
     * The separator which is used to split opaque path names into
     * {@link FsMountPoint mount points} and {@link FsNodeName node names}.
     * This is identical to the separator in the class
     * {@link java.net.JarURLConnection}.
     */
    public static final String SEPARATOR = "!" + FsNodeName.SEPARATOR;

    private URI uri; // not final for serialization only!

    private transient Optional<FsNodePath> path = Optional.empty();

    private transient volatile Optional<FsScheme> scheme = Optional.empty();

    private transient volatile Optional<URI> hierarchical = Optional.empty();

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsMountPoint create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     * This static factory method calls
     * {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param uri      the {@link #getUri() URI}.
     * @param modifier the URI modifier.
     * @return A new mount point.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *                                  syntax constraints for mount points.
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
     * {@link #FsMountPoint(FsScheme, FsNodePath) new FsMountPoint(scheme, path)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param scheme the {@link #getScheme() scheme}.
     * @param path   the {@link #getPath() path}.
     * @return A new mount point.
     * @throws IllegalArgumentException if the composed mount point URI would
     *                                  not conform to the syntax constraints for mount points.
     */
    public static FsMountPoint
    create(FsScheme scheme, FsNodePath path) {
        try {
            return new FsMountPoint(scheme, path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, FsUriModifier.NULL)}.
     */
    @ConstructorProperties("uri")
    public FsMountPoint(URI uri) throws URISyntaxException {
        parse(uri, NULL);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     *
     * @param uri      the {@link #getUri() URI}.
     * @param modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *                            syntax constraints for mount points.
     */
    public FsMountPoint(URI uri, FsUriModifier modifier) throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Constructs a new opaque mount point by composing its URI from the given
     * scheme and path.
     *
     * @param scheme the non-{@code null} {@link #getScheme() scheme}.
     * @param path   the non-{@code null} {@link #getPath() path}.
     * @throws URISyntaxException if the composed mount point URI would not
     *                            conform to the syntax constraints for mount points.
     */
    public FsMountPoint(final FsScheme scheme, final FsNodePath path) throws URISyntaxException {
        final URI pu = path.getUri();
        if (!pu.isAbsolute()) {
            throw new QuotedUriSyntaxException(pu, "Path not absolute");
        }
        final String penup = path.getNodeName().getUri().getPath();
        if (0 == penup.length()) {
            throw new QuotedUriSyntaxException(pu, "Empty node name");
        }
        this.uri = new UriBuilder(true)
                .scheme(scheme.toString())
                .path(pu.getScheme() + ':' + pu.getRawSchemeSpecificPart() + SEPARATOR)
                .toUriChecked();
        this.scheme = Optional.of(scheme);
        this.path = Optional.of(path);

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

    private void parse(URI uri, final FsUriModifier modifier) throws URISyntaxException {
        uri = modifier.modify(uri, MOUNT_POINT);
        if (null != uri.getRawQuery())
            throw new QuotedUriSyntaxException(uri, "Query component not allowed");
        if (null != uri.getRawFragment())
            throw new QuotedUriSyntaxException(uri, "Fragment component not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getRawSchemeSpecificPart();
            final int i = ssp.lastIndexOf(SEPARATOR);
            if (ssp.length() - 2 != i)
                throw new QuotedUriSyntaxException(uri,
                        "Doesn't end with mount point separator \"" + SEPARATOR + '"');
            path = Optional.of(new FsNodePath(new URI(ssp.substring(0, i)), modifier));
            final URI pu = path.get().getUri();
            if (!pu.isAbsolute())
                throw new QuotedUriSyntaxException(uri, "Path not absolute");
            if (0 == path.get().getNodeName().getPath().length())
                throw new QuotedUriSyntaxException(uri, "Empty URI path of node name of node path");
            if (NULL != modifier) {
                URI nuri = new UriBuilder(true)
                        .scheme(uri.getScheme())
                        .path(pu.getScheme() + ':' + pu.getRawSchemeSpecificPart() + SEPARATOR)
                        .toUriChecked();
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else {
            if (!uri.isAbsolute())
                throw new QuotedUriSyntaxException(uri, "Not absolute");
            if (!uri.getRawPath().endsWith(FsNodeName.SEPARATOR))
                throw new QuotedUriSyntaxException(uri,
                        "Path component doesn't end with separator \"" + FsNodeName.SEPARATOR + '"');
            path = Optional.empty();
        }
        this.uri = uri;

        assert invariants();
    }

    private boolean invariants() {
        assert null != getUri();
        assert getUri().isAbsolute();
        assert null == getUri().getRawQuery();
        assert null == getUri().getRawFragment();
        if (getUri().isOpaque()) {
            assert getUri().getRawSchemeSpecificPart().endsWith(SEPARATOR);
            final Optional<? extends FsNodePath> path = getPath();
            assert path.isPresent();
            assert path.get().getUri().isAbsolute();
            assert null == path.get().getUri().getRawFragment();
            assert 0 != path.get().getNodeName().getUri().getRawPath().length();
        } else {
            assert getUri().normalize() == getUri();
            assert getUri().getRawPath().endsWith(FsNodeName.SEPARATOR);
            assert !getPath().isPresent();
        }
        return true;
    }

    /**
     * Returns the URI for this mount point.
     *
     * @return The URI for this mount point.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns a URI which is recursively transformed from the URI of this mount point so that it's absolute and
     * hierarchical.
     * If this mount point is already in absolute and hierarchical form, its URI gets returned.
     * <p>
     * For example, the mount point URIs {@code zip:file:/archive!/} and {@code tar:file:/archive!/} would both produce
     * the same hierarchical URI {@code file:/archive}.
     *
     * @return A URI which is recursively transformed from the URI of this mount point so that it's absolute and
     * hierarchical.
     */
    public URI toHierarchicalUri() {
        final Optional<URI> hierarchical = this.hierarchical;
        return hierarchical.orElseGet(() -> (this.hierarchical = Optional.of(uri.isOpaque()
                ? path.get().toHierarchicalUri()
                : uri)).get());
    }

    /**
     * Returns the scheme component.
     *
     * @return The scheme component.
     */
    public FsScheme getScheme() {
        final Optional<FsScheme> s = scheme;
        return s.orElseGet(() -> (scheme = Optional.of(FsScheme.create(uri.getScheme()))).get());
    }

    /**
     * Returns the optional path component or empty iff this mount point's {@link #getUri URI} doesn't name a parent
     * mount point, that is if and only if it's hierarchical.
     *
     * @return The optional path component.
     */
    public Optional<FsNodePath> getPath() {
        return path;
    }

    /**
     * Returns the optional parent component, that is the mount point of the parent file system, or empty iff this mount
     * point's {@link #getUri URI} doesn't name a parent mount point, that is if and only if it's hierarchical.
     *
     * @return The optional parent component.
     */
    public Optional<? extends FsMountPoint> getParent() {
        assert !path.isPresent() || path.get().getMountPoint().isPresent();
        return path.flatMap(FsNodePath::getMountPoint);
    }

    /**
     * Resolves the given node name against this mount point.
     *
     * @param name a node name relative to this mount point.
     * @return A new path with an absolute URI.
     */
    public FsNodePath resolve(FsNodeName name) {
        return new FsNodePath(Optional.of(this), name);
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
     * Returns {@code true} iff the given object is a mount point and its URI
     * {@link URI#equals(Object) equals} the URI of this mount point.
     * Note that this ignores the scheme and path.
     */
    @Override
    public boolean equals(Object that) {
        return this == that || that instanceof FsMountPoint && this.uri.equals(((FsMountPoint) that).uri);
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
