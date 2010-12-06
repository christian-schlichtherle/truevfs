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
import static de.schlichtherle.truezip.io.filesystem.FileSystemEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.Paths.isRoot;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class Path implements Serializable {

    private static final long serialVersionUID = 5798435461242930648L;

    /** The separator which is used to split opaque URIs into segments. */
    public static final String BANG_SEPARATOR = "!" + SEPARATOR;

    private final URI uri;
    private final Path parent;
    private final URI parentPath;

    /**
     * Constructs a new path.
     * Following are the preconditions for the given URI:
     * <ul>
     * <li>It must be hierarchical.
     * <li>It must be in normal form.
     * <li>It must not have a fragment component.
     * <li>... (TODO: Detail all other preconditions)
     * </ul>
     *
     * @param  uri the non-{@code null} Uniform Resource Identifier (URI) of
     *         this path.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if any other precondition for the
     *         parameter does not hold.
     */
    public Path(URI uri) {
        this(uri, null);
    }

    /**
     * Constructs a new path.
     * Following are the preconditions for the given URI:
     * <ul>
     * <li>It must not have a fragment component.
     * <li>... (TODO: Detail all other preconditions)
     * </ul>
     *
     * @param  uri the non-{@code null} Uniform Resource Identifier (URI) of
     *         this path.
     * @param  parent the nullable parent path.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if any other precondition for the
     *         parameters does not hold.
     */
    public Path(final URI uri, final Path parent) {
        /*if (!uri.isAbsolute())
            throw new IllegalArgumentException();
        if (!uri.getRawSchemeSpecificPart().endsWith(SEPARATOR))
            throw new IllegalArgumentException();*/
        if (null != uri.getRawFragment())
            throw new IllegalArgumentException();
        try {
            if (uri.isOpaque()) {
                if (null == parent)
                    throw new IllegalArgumentException("Missing parent!");
                final String ssp = uri.getSchemeSpecificPart();
                if (!ssp.endsWith(BANG_SEPARATOR))
                    throw new URISyntaxException(   uri.toString(),
                                                    "Doesn't end with the bang separator \""
                                                    + BANG_SEPARATOR + '"');
                final String pmp = parent.getUri().toString();
                if (!ssp.startsWith(pmp))
                    throw new URISyntaxException(   uri.toString(),
                                                    "Parent/member mismatch");
                this.parentPath = URI.create(
                        ssp.substring(pmp.length(), ssp.length() - 2)
                        + SEPARATOR_CHAR);
            } else {
                if (uri.normalize() != uri)
                    throw new IllegalArgumentException();
                if (null != parent) {
                    final URI parentPath = parent.getUri().relativize(uri);
                    if (parentPath.equals(uri))
                        throw new URISyntaxException(   uri.toString(),
                                                        "Parent/member mismatch");
                    assert null == parentPath.getScheme();
                    this.parentPath = parentPath;
                } else {
                    this.parentPath = null;
                }
            }
            if (null != this.parentPath
                    && (this.parentPath.toString().startsWith(".." + SEPARATOR)
                        || this.parentPath.toString().equals("..")))
                throw new URISyntaxException(   uri.toString(),
                                                "Illegal parent path");
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        this.parent = parent;
        this.uri = uri;

        assert (null == this.parent && null == this.parentPath)
                ^ (null != this.parent && !this.parentPath.isAbsolute());
        assert null == this.parentPath
                || !this.parentPath.toString().startsWith(".." + SEPARATOR);
    }

    /**
     * Returns the non-{@code null} Uniform Resource Identifier (URI) of this
     * path which was provided to the constructor of this instance.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns the nullable parent path of this path which was provided to the
     * constructor of this instance.
     */
    public Path getParent() {
        return parent;
    }

    public URI resolve(String path) {
        return uri.isOpaque()
                ? URI.create(uri + path)
                : uri.resolve(path);
    }

    /**
     * Resolves the given path name against the relative path name of this
     * path instance to its parent path instance.
     *
     * @param  name a non-{@code null}
     *         {@link FileSystemEntry#getName path name}.
     * @throws NullPointerException if this path does not have a
     *         {@link #getParent() parent path} or {@code name} is {@code null}.
     */
    public URI parentPath(String name) {
        return parentPath.resolve(name);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[uri=")
                .append(uri)
                .append(",parent=")
                .append(parent)
                .append(",parentPath=")
                .append(parentPath)
                .append("]")
                .toString();
    }
}
