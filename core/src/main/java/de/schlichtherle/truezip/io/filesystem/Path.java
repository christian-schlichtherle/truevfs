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

/**
 * Represents an identifier in the name space of a federated file system.
 * Note that this class is immutable and final, hence thread-safe, too.
 * <p>
 * Every path has a {@link #getPath() path name}, an optional
 * {@link #getMember() member name} and an optional
 * {@link #getParent() parent path}.
 *
 * <p>
 * The path name is a {@link URI Uniform Resource Identifier}
 * which conforms to the following additional constraints for paths:
 * <ol>
 * <li>The path name must not have a fragment component.
 * <li>If the path name is hierarchical, it must be in normal form.
 * <li>If the path name is opaque, its scheme specific part must contain at
 *     least one bang slash separator {@code "!/"}.
 *     The part after the last bang slash separator is parsed as a relative URI
 *     which must be in normal form, must not be equal to {@code ".."} and must
 *     not start with a {@code "/"} or {@code "../"}.
 *     Finally, the part before the last bang slash separator is recursively
 *     parsed as a path again - so it must conform to all these constraints,
 *     too.
 * </ol>
 * <p>
 * Examples for valid path names are:
 * <ul>
 * <li>{@code foo}
 * <li>{@code foo:/bar}
 * <li>{@code foo:bar:/baz!/bang}
 * </ul>
 * Examples for invalid path names are:
 * <ul>
 * <li>{@code foo/.} (not in normal form)
 * <li>{@code foo:/bar/.} (dito)
 * <li>{@code foo:bar} (Missing bang slash separator)
 * <li>{@code foo:bar:/baz/../bang!/} (parent path name not in normal form)
 * <li>{@code foo:bar:/baz/!/../bang} (member name starts with {@code "../"})
 * </ul>
 *
 * @see     MountPoint
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class Path implements Serializable, Comparable<Path> {

    private static final long serialVersionUID = 5798435461242930648L;

    /** The separator which is used to split opaque path names into segments. */
    public static final String BANG_SLASH = "!" + SEPARATOR;

    private final URI path;
    private final Path parent;
    private final URI member;

    /**
     * Constructs a new path.
     * This static factory method calls
     * {@link #Path(URI) new Path(path)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     * <p>
     * If the given path name is opaque, its parent path name is parsed
     * according to the syntax specification for paths
     * and the result is used to compute the
     * {@link #getParent() parent path}.
     *
     * @param  path the non-{@code null} {@link #getPath() path name}.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws URISyntaxException if {@code name} does not conform to
     *         the additional constraints for paths.
     * @return A non-{@code null} path.
     */
    public static Path create(URI path) {
        try {
            return new Path(path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new path.
     * <p>
     * If the given path name is opaque, its parent path name is parsed
     * according to the syntax specification for paths
     * and the result is used to compute the
     * {@link #getParent() parent path}.
     *
     * @param  path the non-{@code null} {@link #getPath() path name}.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws URISyntaxException if {@code name} does not conform to
     *         the additional constraints for paths.
     */
    public Path(URI path) throws URISyntaxException {
        this(path, null);
    }

    /**
     * Constructs a new path.
     * <p>
     * If the given path name is opaque, its parent path name is parsed
     * according to the syntax specification for paths.
     * Then, if {@code parent} is {@code null}, the result is used to compute
     * the {@link #getParent() parent path}.
     * Otherwise, the result must compare {@link #equals equal} to the given
     * {@link #getParent() parent path}.
     * <p>
     * If the given path name is hierarchical and the given parent path is
     * not {@code null}, the parent path's path name must be an ancestor
     * of the given path name, i.e. the member name must not be empty.
     *
     * @param  path the non-{@code null} {@link #getPath() path name}.
     * @param  parent the nullable {@link #getParent() parent path}.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws URISyntaxException if {@code name} does not conform to
     *         the additional constraints for paths
     *         or {@code parent} is not a valid parent path.
     */
    Path(final URI path, Path parent) throws URISyntaxException {
        final URI member;
        if (null != path.getRawFragment())
            throw new URISyntaxException(path.toString(),
                    "Fragment component not allowed");
        if (path.isOpaque()) {
            final String ssp = path.getSchemeSpecificPart();
            final int i = ssp.lastIndexOf(BANG_SLASH);
            if (0 > i)
                throw new URISyntaxException(path.toString(),
                        "Missing separator \"" + BANG_SLASH + '"');
            final URI parentPath = new URI(ssp.substring(0, i));
            if (parentPath.getRawSchemeSpecificPart().endsWith(SEPARATOR))
                throw new URISyntaxException(parentPath.toString(),
                        "Must not end with a separator \"" + SEPARATOR + '"');
            member = new URI(null, ssp.substring(i + 2), null);
            final String m = member.toString();
            if (member.normalize() != member
                    || m.equals("..")
                    || m.startsWith(SEPARATOR)
                    || m.startsWith(".." + SEPARATOR))
                throw new URISyntaxException(m, "Illegal member name");
            if (null == parent)
                parent = new Path(parentPath);
            else if (!parent.getPath().equals(parentPath))
                throw new URISyntaxException(path.toString(),
                        parent.toString() + ": not a parent of");
        } else {
            if (path.normalize() != path)
                throw new URISyntaxException(path.toString(),
                        "Not in normal form");
            if (null != parent) {
                member = parent.getPath().relativize(path);
                if (member == path || 0 == member.toString().length())
                    throw new URISyntaxException(path.toString(),
                            parent.toString() + ": not an ancestor of");
            } else {
                member = null;
            }
        }
        this.path = path;
        this.parent = parent;
        this.member = member;

        assert invariants();
    }

    private boolean invariants() {
        assert null != path;
        assert null == path.getRawFragment();
        if (path.isOpaque()) {
            assert path.toString().contains(BANG_SLASH);
            assert null != parent;
        } else {
            assert path.normalize() == path;
        }
        if (null != member) {
            assert null != parent;
            assert !member.isAbsolute();
            assert member.normalize() == member;
            final String m = member.toString();
            assert path.isOpaque() || 0 != m.length();
            assert !m.equals("..");
            assert !m.startsWith(SEPARATOR);
            assert !m.startsWith(".." + SEPARATOR);
        } else {
            assert null == parent;
        }
        return true;
    }

    /**
     * Returns the nullable parent path.
     * If a parent path was provided to the constructor, it's returned.
     * Otherwise, if the path name provided to the constructor is opaque,
     * a parent path is returned which has been computed from the path name.
     * Otherwise, {@code null} is returned.
     *
     * @return The nullable parent path.
     */
    public Path getParent() {
        return parent;
    }

    /**
     * Returns the nullable member name.
     * If this path has a {@link #getParent() parent path}, then this path's
     * path name relative to the parent path's path name is returned.
     * Otherwise, {@code null} is returned.
     *
     * @return The nullable member name.
     */
    public URI getMember() {
        return member;
    }

    /**
     * Returns the non-{@code null} path name which was provided to the
     * constructor.
     *
     * @return The non-{@code null} path name.
     */
    public URI getPath() {
        return path;
    }

    /**
     * Returns {@code true} iff the given object is a path and its path name
     * {@link URI#equals(Object) equals} this path's path name.
     * Note that this ignores the parent path and member name.
     */
    @Override
    public boolean equals(final Object that) {
        return this == that
                || that instanceof Path
                    && this.getPath().equals(((Path) that).getPath());
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(final Path that) {
        return this.getPath().compareTo(that.getPath());
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    /**
     * Equivalent to <code>{@link #getPath()}.{@link Object#toString()}</code>.
     */
    @Override
    public String toString() {
        return getPath().toString();
    }
}
