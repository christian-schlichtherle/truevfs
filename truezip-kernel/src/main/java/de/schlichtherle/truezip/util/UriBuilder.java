/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.util.UriEncoder.Encoding.*;

/**
 * A mutable JavaBean for composing URIs according to
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
 * and its updates in
 * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
 * for IPv6 addresses.
 * <p>
 * This class complements the immutable {@link URI} class by enabling its
 * clients to compose a URI from its components which can get read or written
 * as independent properties.
 * Each URI is composed of the five components {@link #getScheme() scheme},
 * {@link #getAuthority() authority}, {@link #getPath() path},
 * {@link #getQuery() query} and {@link #getFragment fragment}.
 * When done with setting the properties for the URI components, the resulting
 * URI can be composed by calling any of the methods {@link #getUri()},
 * {@link #toUri()}, {@link #getString()} or {@link #toString()}.
 * <p>
 * This class quotes illegal characters wherever required for the respective
 * URI component.
 * As a deviation from RFC&nbsp;2396, non-US-ASCII characters get preserved
 * when encoding.
 * <p>
 * Note that using this class is superior to the five argument URI constructor
 * <code>new {@link URI#URI(String, String, String, String, String) URI(scheme, authority, path, query, fragment)}</code>
 * because the URI constructor does not quote all paths correctly.
 * For example, {@code new URI(null, null, "foo:bar", null, null)} does not
 * quote the colon before parsing so the resulting URI will have a scheme
 * component {@code foo} and a path component {@code bar} instead of just a
 * path component {@code foo:bar}.
 * 
 * <h3>Identities</h3>
 * For any {@link URI} {@code u} it is generally true that
 * <pre>{@code new UriBuilder(u).getUri().equals(u);}</pre>
 * and
 * <pre>{@code new UriBuilder().uri(u).getUri().equals(u);}</pre>
 * and
 * <pre>{@code
 * new UriBuilder()
 *     .scheme(u.getScheme())
 *     .authority(u.getAuthority())
 *     .path(u.isOpaque() ? u.getSchemeSpecificPart() : u.getPath())
 *     .query(u.getQuery())
 *     .fragment(u.getFragment())
 *     .getUri()
 *     .equals(u);
 * }</pre>
 * These identity productions still apply even if the method {@link #getUri()}
 * is substituted with the method {@link #toUri()}.
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">
 *      RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</a>
 * @see <a href="http://www.ietf.org/rfc/rfc2732.txt">
 *      RFC&nbsp;2732: Format for Literal IPv6 Addresses in URL's</a>
 * @see UriEncoder
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@NotThreadSafe
public final class UriBuilder {

    private final UriEncoder encoder;
    private @CheckForNull StringBuilder builder;
    private @CheckForNull String scheme;
    private @CheckForNull String authority;
    private @CheckForNull String path;
    private @CheckForNull String query;
    private @CheckForNull String fragment;

    /**
     * Constructs a new URI builder.
     */
    public UriBuilder() {
        encoder = new UriEncoder(null);
    }

    /**
     * Constructs a new URI builder.
     * 
     * @param uri the uri for initializing the initial state.
     */
    public UriBuilder(URI uri) {
        this();
        setUri(uri); // OK - class is final!
    }

    /**
     * Clears the state of this URI builder.
     * Calling this method is effectively the same as setting all URI component
     * properties to {@code null}.
     * 
     * @return {@code this}
     */
    public UriBuilder clear() {
        //uri = null;
        scheme = null;
        authority = null;
        path = null;
        query = null;
        fragment = null;
        return this;
    }

    /**
     * Returns a new URI string which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI string which is composed from the properties of
     *         this URI builder.
     * @throws IllegalStateException if composing a valid URI is not possible.
     */
    @Override
    public String toString() {
        try {
            return getString();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns a new URI string which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI string which is composed from the properties of
     *         this URI builder.
     * @throws URISyntaxException if composing a valid URI is not possible due
     *         to an invalid scheme.
     */
    public String getString() throws URISyntaxException {
        StringBuilder b = builder;
        if (null == b)
            b = builder = new StringBuilder();
        else
            b.setLength(0);
        int errIdx = -1;            // error index
        String errMsg = null;       // error message
        final String    s = scheme,
                        a = authority, p = path, q = query,
                        f = fragment;
        final boolean absUri = null != s;
        if (absUri)
            b.append(s).append(':');
        final int ssp = b.length(); // index of scheme specific part
        final boolean hasAuth = null != a;
        if (hasAuth)
            encoder.encode(a, AUTHORITY, b.append("//"));
        boolean absPath = false;
        if (null != p && !p.isEmpty()) {
            if (p.startsWith("/")) {
                absPath = true;
                encoder.encode(p, ABSOLUTE_PATH, b);
            } else if (hasAuth) {
                absPath = true;
                errIdx = b.length();
                errMsg = "Relative path with " + (a.isEmpty() ? "" : "non-") + "empty authority";
                encoder.encode(p, ABSOLUTE_PATH, b);
            } else if (absUri) {
                encoder.encode(p, QUERY, b);
            } else {
                encoder.encode(p, PATH, b);
            }
        }
        if (null != q) {
            b.append('?');
            if (absUri && !absPath) {
                errIdx = b.length();
                errMsg = "Query in opaque URI";
            }
            encoder.encode(q, QUERY, b);
        }
        assert absUri == 0 < ssp;
        if (absUri && ssp >= b.length()){
            errIdx = b.length();
            errMsg = "Empty scheme specific part in absolute URI";
        }
        if (null != f)
            encoder.encode(f, FRAGMENT, b.append('#'));
        if (absUri)
            validateScheme((CharBuffer) CharBuffer.wrap(b).limit(s.length()));
        final String u = b.toString();
        if (0 <= errIdx)
            throw new URISyntaxException(quote(u), errMsg, errIdx);
        return u;
    }

    /**
     * Checks the given string to conform to the syntax constraints for URI
     * schemes in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     *
     * @param  scheme the string to validate.
     * @throws URISyntaxException if {@code scheme} does not conform to the
     *         syntax constraints for URI schemes in
     *         <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>.
     */
    public static void validateScheme(final String scheme)
    throws URISyntaxException {
        validateScheme(CharBuffer.wrap(scheme));
    }

    private static void validateScheme(final CharBuffer input)
    throws URISyntaxException {
        if (!input.hasRemaining())
            throw newURISyntaxException(input, "Empty URI scheme");
        char c = input.get();
        // TODO: Character class is no help here - consider table lookup!
        if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c))
            throw newURISyntaxException(input, "Illegal character in URI scheme");
        while (input.hasRemaining()) {
            c = input.get();
            if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c)
                    && (c < '0' || '9' < c)
                    && c != '+' && c != '-' && c != '.')
                throw newURISyntaxException(input, "Illegal character in URI scheme");
        }
    }

    private static URISyntaxException newURISyntaxException(CharBuffer input, String reason) {
        int p = input.position() - 1;
        return new URISyntaxException(quote(input.rewind().limit(input.capacity())), reason, p);
    }

    private static String quote(Object s) {
        return "\"" + s + "\"";
    }

    /**
     * Initializes all URI components from the given URI string.
     * 
     * @param  uri the URI string.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints of the {@link URI} class.
     */
    public void setString(String uri) {
        setUri(URI.create(uri));
    }

    /**
     * Initializes all URI components from the given URI string.
     * 
     * @param  uri the URI string.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints of the {@link URI} class.
     * @return {@code this}
     */
    public UriBuilder string(String uri) {
        setString(uri);
        return this;
    }

    /**
     * Returns a new URI which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI which is composed from the properties of
     *         this URI builder.
     * @throws IllegalStateException if composing a valid URI is not possible.
     */
    public URI toUri() {
        try {
            return getUri();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns a new URI which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI which is composed from the properties of
     *         this URI builder.
     * @throws URISyntaxException if composing a valid URI is not possible.
     */
    public URI getUri() throws URISyntaxException {
        String u = getString();
        try {
            return new URI(u);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Initializes all URI components from the given URI.
     * 
     * @param  uri the URI.
     */
    public void setUri(final URI uri) {
        setScheme(uri.getScheme());
        setAuthority(uri.getAuthority());
        setPath(uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath());
        setQuery(uri.getQuery());
        setFragment(uri.getFragment());
    }

    /**
     * Initializes all URI components from the given URI.
     * 
     * @param  uri the URI.
     * @return {@code this}
     */
    public UriBuilder uri(URI uri) {
        setUri(uri);
        return this;
    }

    /**
     * Returns the URI scheme component.
     * 
     * @return The URI scheme component.
     */
    @CheckForNull
    public String getScheme() {
        return scheme;
    }

    /**
     * Sets the URI scheme component.
     *
     * @param  scheme the URI scheme component.
     */
    public void setScheme(final @CheckForNull String scheme) {
        this.scheme = scheme;
    }

    /**
     * Sets the URI scheme component.
     *
     * @param  scheme the URI scheme component.
     * @return {@code this}
     */
    public UriBuilder scheme(@CheckForNull String scheme) {
        setScheme(scheme);
        return this;
    }

    /**
     * Returns the URI authority component.
     * If this URI builder has been {@link #setUri(URI) initialized} from an
     * {@link URI#isOpaque() opaque} URI, then this property is {@code null}.
     * 
     * @return The URI authority component.
     */
    @CheckForNull
    public String getAuthority() {
        return authority;
    }

    /**
     * Sets the URI authority component.
     *
     * @param  authority the URI authority component.
     */
    public void setAuthority(final @CheckForNull String authority) {
        this.authority = authority;
    }

    /**
     * Sets the URI authority component.
     *
     * @param  authority the URI authority component.
     * @return {@code this}
     */
    public UriBuilder authority(@CheckForNull String authority) {
        setAuthority(authority);
        return this;
    }

    /**
     * Returns the URI path component.
     * If this URI builder has been {@link #setUri(URI) initialized} from an
     * {@link URI#isOpaque() opaque} URI, then this property contains the
     * scheme specific part of the URI.
     * 
     * @return The URI path component.
     */
    @CheckForNull
    public String getPath() {
        return path;
    }

    /**
     * Sets the URI path component.
     *
     * @param  path the URI path component.
     */
    public void setPath(final @CheckForNull String path) {
        this.path = path;
    }

    /**
     * Sets the URI path component.
     *
     * @param  path the URI path component.
     * @return {@code this}
     */
    public UriBuilder path(@CheckForNull String path) {
        setPath(path);
        return this;
    }
    
    /**
     * Returns the URI query component.
     * If this URI builder has been {@link #setUri(URI) initialized} from an
     * {@link URI#isOpaque() opaque} URI, then this property is {@code null}.
     * 
     * @return The URI query component.
     */
    @CheckForNull
    public String getQuery() {
        return query;
    }

    /**
     * Sets the URI query component.
     *
     * @param  query the URI query component.
     */
    public void setQuery(final @CheckForNull String query) {
        this.query = query;
    }

    /**
     * Sets the URI query component.
     *
     * @param  query the URI query component.
     * @return {@code this}
     */
    public UriBuilder query(@CheckForNull String query) {
        setQuery(query);
        return this;
    }
    
    /**
     * Returns the URI fragment component.
     * 
     * @return The URI fragment component.
     */
    @CheckForNull
    public String getFragment() {
        return fragment;
    }

    /**
     * Sets the URI fragment component.
     *
     * @param  fragment the URI fragment component.
     */
    public void setFragment(final @CheckForNull String fragment) {
        this.fragment = fragment;
    }
    
    /**
     * Sets the URI fragment component.
     *
     * @param  fragment the URI fragment component.
     * @return {@code this}
     */
    public UriBuilder fragment(@CheckForNull String fragment) {
        setFragment(fragment);
        return this;
    }
}
