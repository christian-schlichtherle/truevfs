/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static net.java.truecommons3.shed.UriEncoder.Encoding.*;

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
 * Each URI is composed of the five components scheme, authority, path, query
 * and fragment.
 * When done with setting the properties for the URI components, the resulting
 * URI can be composed by calling any of the methods {@link #toUriChecked()},
 * {@link #toUriUnchecked()}, {@link #toStringChecked()} or {@link #toStringUnchecked()}.
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
 * <pre>{@code new UriBuilder(u).toUri().equals(u);}</pre>
 * and
 * <pre>{@code new UriBuilder().uri(u).toUri().equals(u);}</pre>
 * and
 * <pre>{@code
 * new UriBuilder()
 *     .scheme(u.getScheme())
 *     .authority(u.getAuthority())
 *     .path(u.isOpaque() ? u.getSchemeSpecificPart() : u.getPath())
 *     .query(u.getQuery())
 *     .fragment(u.getFragment())
 *     .toUri()
 *     .equals(u);
 * }</pre>
 * These identity productions apply for the method {@link #toUriUnchecked()} as well as
 * the method {@link #toUriChecked()}.
 *
 * @see    <a href="http://www.ietf.org/rfc/rfc2396.txt">
 *         RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</a>
 * @see    <a href="http://www.ietf.org/rfc/rfc2732.txt">
 *         RFC&nbsp;2732: Format for Literal IPv6 Addresses in URL's</a>
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
public final class UriBuilder {

    private final StringBuilder builder = new StringBuilder();
    private final UriEncoder encoder;
    private Option<String> scheme = Option.none();
    private Option<String> authority = Option.none();
    private Option<String> path = Option.none();
    private Option<String> query = Option.none();
    private Option<String> fragment = Option.none();

    /**
     * Constructs a new URI builder.
     * Equivalent to {@link #UriBuilder(boolean) UriBuilder(false)}.
     */
    public UriBuilder() {
        this(false);
    }

    /**
     * Constructs a new URI builder.
     *
     * @param raw If {@code true}, then the {@code '%'} character doesn't get
     *        quoted.
     */
    public UriBuilder(boolean raw) {
        this.encoder = new UriEncoder(Option.<Charset>none(), raw);
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
     * @see    #toStringChecked()
     */
    public String toStringUnchecked() {
        try {
            return toStringChecked();
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
     * @see    #toStringUnchecked()
     */
    public String toStringChecked() throws URISyntaxException {
        builder.setLength(0);
        int errorIndex = -1;                        // error index
        Option<String> errorMessage = Option.none();  // error message
        boolean absUri = false;
        for (final String s : scheme) {
            absUri = true;
            builder.append(s).append(':');
        }
        final int ssp = builder.length();             // index of scheme specific part
        boolean hasAuth = false;
        for (final String a : authority) {
            hasAuth = true;
            builder.append("//");
            encoder.encode(AUTHORITY, a, builder);
        }
        boolean absPath = false;
        for (final String p : path) {
            if (!p.isEmpty()) {
                if (p.startsWith("/")) {
                    absPath = true;
                    encoder.encode(ABSOLUTE_PATH, p, builder);
                } else if (hasAuth) {
                    absPath = true;
                    errorIndex = builder.length();
                    errorMessage = Option.some("Relative path with " + (authority.isEmpty() ? "" : "non-") + "empty authority");
                    encoder.encode(ABSOLUTE_PATH, p, builder);
                } else if (absUri) {
                    encoder.encode(QUERY, p, builder);
                } else {
                    encoder.encode(PATH, p, builder);
                }
            }
        }
        for (final String q : query) {
            builder.append('?');
            if (absUri && !absPath) {
                errorIndex = builder.length();
                errorMessage = Option.some("Query in opaque URI");
            }
            encoder.encode(QUERY, q, builder);
        }
        assert absUri == 0 < ssp;
        if (absUri && ssp >= builder.length()){
            errorIndex = builder.length();
            errorMessage = Option.some("Empty scheme specific part in absolute URI");
        }
        for (final String f : fragment) {
            builder.append('#');
            encoder.encode(FRAGMENT, f, builder);
        }
        if (absUri)
            validateScheme((CharBuffer) CharBuffer.wrap(builder).limit(scheme.get().length()));
        final String u = builder.toString();
        for (String msg : errorMessage)
            throw new QuotedUriSyntaxException(u, errorMessage.get(), errorIndex);
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
        return new QuotedUriSyntaxException(input.rewind().limit(input.capacity()), reason, p);
    }

    /**
     * Initializes all URI components from the given URI string.
     *
     * @param  uri the URI string.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints of the {@link URI} class.
     * @return {@code this}
     */
    public UriBuilder string(String uri) { return uri(URI.create(uri)); }

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
     * @see    #toUriChecked()
     */
    public URI toUriUnchecked() {
        try {
            return toUriChecked();
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
     * @see    #toUriUnchecked()
     */
    public URI toUriChecked() throws URISyntaxException {
        final String s = toStringChecked();
        try {
            return new URI(s);
        } catch (URISyntaxException ex) {
            // See http://java.net/jira/browse/TRUEZIP-180
            throw new AssertionError(ex);
        }
    }

    /**
     * Initializes all URI components from the given URI.
     *
     * @param  uri the URI.
     * @return {@code this}
     */
    public UriBuilder uri(URI uri) {
        if (encoder.isRaw()) {
            return scheme(uri.getScheme())
                    .authority(uri.getRawAuthority())
                    .path(uri.isOpaque() ? uri.getRawSchemeSpecificPart() : uri.getRawPath())
                    .query(uri.getRawQuery())
                    .fragment(uri.getRawFragment());
        } else {
            return scheme(uri.getScheme())
                    .authority(uri.getAuthority())
                    .path(uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath())
                    .query(uri.getQuery())
                    .fragment(uri.getFragment());
        }
    }

    /**
     * Sets the nullable URI scheme component.
     *
     * @param  scheme the nullable URI scheme component.
     * @return {@code this}
     */
    public UriBuilder scheme(String scheme) {
        this.scheme = Option.apply(scheme);
        return this;
    }

    /**
     * Sets the nullable URI authority component.
     *
     * @param  authority the nullable URI authority component.
     * @return {@code this}
     */
    public UriBuilder authority(String authority) {
        this.authority = Option.apply(authority);
        return this;
    }

    /**
     * Sets the nullable URI path component.
     *
     * @param  path the nullable URI path component.
     * @return {@code this}
     */
    public UriBuilder path(String path) {
        this.path = Option.apply(path);
        return this;
    }

    /**
     * Sets the nullable URI query component.
     *
     * @param  query the nullable URI query component.
     * @return {@code this}
     */
    public UriBuilder query(String query) {
        this.query = Option.apply(query);
        return this;
    }

    /**
     * Sets the URI path and query components by splitting the given nullable
     * string at the first occurence of the query separator {@code '?'}.
     *
     * @param  pathQuery the nullable combined URI path and query components.
     * @return {@code this}
     */
    public UriBuilder pathQuery(String pathQuery) {
        final Option<String> opq = Option.apply(pathQuery);
        final int i;
        if (!opq.isEmpty() && 0 <= (i = opq.get().indexOf('?'))) {
            this.path = Option.some(opq.get().substring(0, i));
            this.query = Option.some(opq.get().substring(i + 1));
        } else {
            this.path = opq;
            this.query = Option.none();
        }
        return this;
    }

    /**
     * Sets the nullable URI fragment component.
     *
     * @param  fragment the nullable URI fragment component.
     * @return {@code this}
     */
    public UriBuilder fragment(String fragment) {
        this.fragment = Option.apply(fragment);
        return this;
    }
}
