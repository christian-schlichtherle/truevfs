/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.shed.QuotedUriSyntaxException;
import global.namespace.truevfs.comp.shed.UriBuilder;

import javax.annotation.CheckForNull;
import java.beans.ConstructorProperties;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static global.namespace.truevfs.kernel.api.FsUriModifier.CANONICALIZE;
import static global.namespace.truevfs.kernel.api.FsUriModifier.NULL;
import static global.namespace.truevfs.kernel.api.FsUriModifier.PostFix.NODE_PATH;

/**
 * Addresses a file system node.
 * The purpose of a file system path is to parse a {@link URI} and decompose it
 * into a file system {@link #getMountPoint() mount point} and
 * {@linkplain #getNodeName() node name}.
 *
 * <h3><a name="specification">Specification</a></h3>
 * <p>
 * A path adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must not define a fragment component.
 * <li>If the URI is opaque, its scheme specific part must contain at least
 *     one mount point separator {@code "!/"}.
 *     The part <em>up to</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsMountPoint} and set
 *     as the value of the {@link #getMountPoint() mountPoint} property.
 *     The part <em>after</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsNodeName} and set
 *     as the value of the {@linkplain #getNodeName() node name} property.
 * <li>Otherwise, if the URI is absolute, it's resolved with {@code "."},
 *     parsed according to the syntax constraints for an {@link FsMountPoint}
 *     and set as the value of the {@link #getMountPoint() mountPoint} property.
 *     The URI relativized to this mount point is parsed according to the
 *     syntax constraints for an {@link FsNodeName} and set as the value of
 *     the {@linkplain #getNodeName() node name} property.
 * <li>Otherwise, the value of the {@link #getMountPoint() mountPoint} property
 *     is set to {@code null} and the URI is parsed according to the syntax
 *     constraints for an {@link FsNodeName} and set as the value of the
 *     {@linkplain #getNodeName() node name} property.
 * </ol>
 * For opaque URIs of the form {@code jar:<url>!/<node>}, these constraints
 * build a close subset of the syntax allowed by a
 * {@link java.net.JarURLConnection}.
 *
 * <h3><a name="examples">Examples</a></h3>
 * <p>
 * Examples for <em>valid</em> node path URIs:
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 *   <th>{@link #getUri() uri} property</th>
 *   <th>{@link #getMountPoint() mountPoint} URI</th>
 *   <th>{@link #getNodeName() nodeName} URI</th>
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
 * Examples for <em>invalid</em> node path URIs:
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
 * <h3><a name="identities">Identities</a></h3>
 * <p>
 * For any path {@code p}, it's generally true that
 * {@code new FsNodePath(p.getUri()).equals(p)}.
 * <p>
 * Furthermore, it's generally true that
 * {@code new FsNodePath(p.getMountPoint(), p.getNodeName()).equals(p)}.
 *
 * <h3><a name="serialization">Serialization</a></h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @author Christian Schlichtherle
 * @see FsMountPoint
 * @see FsNodeName
 * @see FsScheme
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class FsNodePath implements Serializable, Comparable<FsNodePath> {

    private static final long serialVersionUID = 0;

    private static final URI DOT = URI.create(".");

    private URI uri; // not final for serialization only!

    private transient Optional<FsMountPoint> mountPoint = Optional.empty();

    private transient FsNodeName nodeName;

    private transient volatile Optional<URI> hierarchical = Optional.empty();

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsNodePath create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new path by parsing the given URI.
     * This static factory method calls
     * {@link #FsNodePath(URI, FsUriModifier) new FsNodePath(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param uri      the {@link #getUri() URI}.
     * @param modifier the URI modifier.
     * @return A new path.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *                                  syntax constraints for paths.
     */
    public static FsNodePath create(URI uri, FsUriModifier modifier) {
        try {
            return new FsNodePath(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsNodePath(URI, FsUriModifier) new FsNodePath(file.toURI(), FsUriModifier.CANONICALIZE)}.
     * Note that this constructor is expected not to throw any exceptions.
     */
    public FsNodePath(File file) {
        try {
            parse(file.toURI(), CANONICALIZE);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Equivalent to {@link #FsNodePath(URI, FsUriModifier) new FsNodePath(uri, FsUriModifier.NULL)}.
     */
    @ConstructorProperties("uri")
    public FsNodePath(URI uri) throws URISyntaxException {
        parse(uri, NULL);
    }

    /**
     * Constructs a new path by parsing the given URI.
     *
     * @param uri      the non-{@code null} {@link #getUri() URI}.
     * @param modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *                            syntax constraints for paths.
     */
    public FsNodePath(URI uri, FsUriModifier modifier) throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Constructs a new path by composing its URI from the given optional mount
     * point and node name.
     *
     * @param mountPoint the optional {@link #getMountPoint() mount point}.
     * @param nodeName   the {@link #getNodeName() node name}.
     */
    public FsNodePath(final Optional<FsMountPoint> mountPoint, final FsNodeName nodeName) {
        URI mpu;
        if (!mountPoint.isPresent()) {
            this.uri = nodeName.getUri();
        } else if (nodeName.isRoot()) {
            this.uri = mountPoint.get().getUri();
        } else if ((mpu = mountPoint.get().getUri()).isOpaque()) {
            try {
                // Compute mountPoint + nodeName, but ensure that all URI
                // components are properly quoted.
                final String mpussp = mpu.getRawSchemeSpecificPart();
                final int mpusspl = mpussp.length();
                final URI enu = nodeName.getUri();
                final String enup = enu.getRawPath();
                final int enupl = enup.length();
                final String enuq = enu.getRawQuery();
                final int enuql = null == enuq ? 0 : enuq.length() + 1;
                final StringBuilder ssp =
                        new StringBuilder(mpusspl + enupl + enuql)
                                .append(mpussp)
                                .append(enup);
                if (null != enuq) {
                    ssp.append('?').append(enuq);
                }
                this.uri = new UriBuilder(true)
                        .scheme(mpu.getScheme())
                        .path(ssp.toString())
                        .fragment(enu.getRawFragment())
                        .toUriChecked();
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            this.uri = mpu.resolve(nodeName.getUri());
        }
        this.mountPoint = mountPoint;
        this.nodeName = nodeName;

        assert invariants();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(uri.toString());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            parse(new URI(in.readObject().toString()), NULL);
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(URI uri, final FsUriModifier modifier) throws URISyntaxException {
        uri = modifier.modify(uri, NODE_PATH);
        if (null != uri.getRawFragment()) {
            throw new QuotedUriSyntaxException(uri, "Fragment component not allowed");
        }
        if (uri.isOpaque()) {
            final String ssp = uri.getRawSchemeSpecificPart();
            final int i = ssp.lastIndexOf(FsMountPoint.SEPARATOR);
            if (0 > i){
                throw new QuotedUriSyntaxException(uri,
                        "Missing mount point separator \"" + FsMountPoint.SEPARATOR + '"');
            }
            mountPoint = Optional.of(new FsMountPoint(
                    new UriBuilder(true)
                            .scheme(uri.getScheme())
                            .path(ssp.substring(0, i + 2))
                            .toUriChecked(),
                    modifier));
            nodeName = new FsNodeName(
                    new UriBuilder(true)
                            .pathQuery(ssp.substring(i + 2))
                            .fragment(uri.getRawFragment())
                            .toUriChecked(),
                    modifier);
            if (NULL != modifier) {
                URI mpu = mountPoint.get().getUri();
                URI nuri = new URI(mpu.getScheme() + ':' + mpu.getRawSchemeSpecificPart() + nodeName.getUri());
                if (!uri.equals(nuri)) {
                    uri = nuri;
                }
            }
        } else if (uri.isAbsolute()) {
            mountPoint = Optional.of(new FsMountPoint(uri.resolve(DOT), modifier));
            nodeName = new FsNodeName(mountPoint.get().getUri().relativize(uri), modifier);
        } else {
            mountPoint = Optional.empty();
            nodeName = new FsNodeName(uri, modifier);
            if (NULL != modifier) {
                uri = nodeName.getUri();
            }
        }
        this.uri = uri;

        assert invariants();
    }

    private boolean invariants() {
        assert null != getUri();
        assert null == getUri().getRawFragment();
        assert getMountPoint().isPresent() == getUri().isAbsolute();
        assert null != getNodeName();
        if (getUri().isOpaque()) {
            assert getUri().getRawSchemeSpecificPart().contains(FsMountPoint.SEPARATOR);
        } else if (getUri().isAbsolute()) {
            assert getUri().normalize() == getUri();
            assert getUri().equals(getMountPoint().get().getUri().resolve(getNodeName().getUri()));
        } else {
            assert getUri().normalize() == getUri();
            assert getNodeName().getUri() == getUri();
        }
        return true;
    }

    /**
     * Returns the URI for this node path.
     *
     * @return The URI for this node path.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns a URI which is recursively transformed from the URI of this
     * path so that it's absolute and hierarchical.
     * If this path is already in absolute and hierarchical form, its URI gets
     * returned.
     * <p>
     * For example, the path URIs {@code zip:file:/archive!/node} and
     * {@code tar:file:/archive!/node} would both produce the hierarchical URI
     * {@code file:/archive/node}.
     *
     * @return A URI which is recursively transformed from the URI of this
     * path so that it's absolute and hierarchical.
     */
    public URI toHierarchicalUri() {
        final Optional<URI> hierarchical = this.hierarchical;
        if (hierarchical.isPresent()) {
            return hierarchical.get();
        } else if (uri.isOpaque()) {
            final URI mpu = mountPoint.get().toHierarchicalUri();
            final URI enu = nodeName.getUri();
            try {
                return (this.hierarchical = Optional.of(enu.toString().isEmpty()
                        ? mpu
                        : new UriBuilder(true)
                        .uri(mpu)
                        .path(mpu.getRawPath() + FsNodeName.SEPARATOR)
                        .toUriChecked()
                        .resolve(enu))).get();
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            return (this.hierarchical = Optional.of(uri)).get();
        }
    }

    /**
     * Returns the mount point component or {@code null} iff this path's
     * {@link #getUri() URI} is not absolute.
     *
     * @return The nullable mount point.
     */
    public Optional<FsMountPoint> getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the node name component.
     * This may be empty, but is never {@code null}.
     *
     * @return The node name component.
     */
    public FsNodeName getNodeName() {
        return nodeName;
    }

    /**
     * Resolves the given node name against this path.
     *
     * @param nodeName a node name relative to this path.
     * @return A new path with an absolute URI.
     */
    public FsNodePath
    resolve(final FsNodeName nodeName) {
        if (nodeName.isRoot() && null == this.uri.getQuery()) {
            return this;
        }
        return new FsNodePath(this.mountPoint, new FsNodeName(this.nodeName, nodeName));
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(FsNodePath that) {
        return this.uri.compareTo(that.uri);
    }

    /**
     * Returns {@code true} iff the given object is a path name and its URI
     * {@link URI#equals(Object) equals} the URI of this path name.
     * Note that this ignores the mount point and node name.
     */
    @Override
    public boolean equals(@CheckForNull Object that) {
        return this == that || that instanceof FsNodePath && this.uri.equals(((FsNodePath) that).uri);
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
