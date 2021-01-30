/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import static java.nio.charset.CoderResult.OVERFLOW;
import static java.nio.charset.CoderResult.UNDERFLOW;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Escapes illegal characters in URI components according to
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
 * and its updates in
 * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
 * for IPv6 addresses.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">
 *      RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</a>
 * @see <a href="http://www.ietf.org/rfc/rfc2732.txt">
 *      RFC&nbsp;2732: Format for Literal IPv6 Addresses in URL's</a>
 * @see UriBuilder
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
final class UriEncoder {

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final String ALPHANUM_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String MARK_CHARS = "-_.!~*'()";
    private static final String DEFAULT_LEGAL_CHARS =
            ALPHANUM_CHARS + MARK_CHARS + ",;$&+=@";

    private final StringBuilder stringBuilder = new StringBuilder();
    private final CharsetEncoder encoder;
    private final boolean encode;
    private final boolean raw;

    /**
     * Constructs a new URI encoder which uses the UTF-8 character set to
     * escape non-US-ASCII characters.
     * Equivalent to {@link #UriEncoder(Option, boolean) UriEncoder(UTF8, false)}.
     */
    UriEncoder() { this(Option.some(UTF_8), false); }

    /**
     * Constructs a new URI codec which uses the UTF-8 character set to encode
     * non-US-ASCII characters.
     * Equivalent to {@link #UriEncoder(Option, boolean) UriEncoder(UTF8, false)}.
     *
     * @param raw If {@code true}, then the {@code '%'} character doesn't get
     *        quoted.
     */
    UriEncoder(boolean raw) { this(Option.some(UTF_8), raw); }

    /**
     * Constructs a new URI codec which uses the given character set to encode
     * non-US-ASCII characters.
     * Equivalent to {@link #UriEncoder(Option, boolean) UriEncoder(charset, false)}.
     *
     * @param charset the character set to use for encoding non-US-ASCII
     *        characters.
     *        If this parameter is {@code null},
     *        then non-US-ASCII characters will get encoded to {@code UTF-8}
     *        if and only if {@link Character#isISOControl(char)} or
     *        {@link Character#isSpaceChar(char)} is {@code true},
     *        so that most non-US-ASCII character would get preserved.
     *        Note that providing any other value than {@code null} or
     *        {@code UTF-8} will void interoperability with most applications.
     */
    UriEncoder(Option<Charset> charset) { this(charset, false); }

    /**
     * Constructs a new URI codec which uses the given character set to escape
     * non-US-ASCII characters.
     * <p>
     *
     * @param charset the character set to use for encoding non-US-ASCII
     *        characters.
     *        If this parameter is {@code null},
     *        then non-US-ASCII characters will get encoded to {@code UTF-8}
     *        if and only if {@link Character#isISOControl(char)} or
     *        {@link Character#isSpaceChar(char)} is {@code true},
     *        so that most non-US-ASCII character would get preserved.
     *        Note that providing any other value than {@code null} or
     *        {@code UTF_8} will void interoperability with most applications.
     * @param raw If {@code true}, then the {@code '%'} character doesn't get
     *        quoted.
     */
    UriEncoder(Option<Charset> charset, final boolean raw) {
        if (!(this.encode = !charset.isEmpty()))
            charset = Option.some(UTF_8);
        this.encoder = charset.get().newEncoder();
        this.raw = raw;
    }

    boolean isRaw() {return raw; }

    /**
     * Encodes all characters in the string {@code dS} which are illegal within
     * the URI component {@code comp}.
     * <p>
     * Note that calling this method on an already encoded string escapes any
     * escape sequences again, that is, each occurence of the character
     * {@code '%'} is substituted with the string {@code "%25"} again.
     *
     * @param  component the URI component to encode.
     * @param  ds the decoded string to encode.
     * @return The encoded string.
     * @throws IllegalArgumentException on any encoding error with a
     *         {@link URISyntaxException} as its
     *         {@link IllegalArgumentException#getCause() cause}.
     *         This exception should never occur if the character set of this
     *         codec is UTF-8.
     */
    String encode(Encoding component, String ds) {
        stringBuilder.setLength(0);
        try {
            if (encode(component, ds, stringBuilder))
                return stringBuilder.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return ds;
    }

    /**
     * Encodes all characters in the string {@code dS} which are illegal within
     * the URI component {@code comp} to the string builder {@code eS}.
     * <p>
     * Note that calling this method on an already encoded string escapes
     * any escape sequences again, that is, each occurence of the character
     * {@code '%'} is substituted with the string {@code "%25"} again.
     *
     * @param  component the URI component to encode.
     * @param  ds the decoded string to encode.
     * @param  esb the encoded string builder to which all encoded characters
     *             shall get appended.
     * @return Whether or not any characters in {@code ds} had to be encoded.
     * @throws URISyntaxException on any encoding error.
     *         This exception should never occur if the character set of this
     *         codec is UTF-8.
     *         If it occurs however, {@code esb} is left in an undefined state.
     */
    boolean encode(final Encoding component, final String ds, final StringBuilder esb)
    throws URISyntaxException {
        final CharBuffer dcb = CharBuffer.wrap(ds); // decoded character buffer
        final Option[] oess = component.escapes;    // optional escape sequences
        Option<ByteBuffer> oebb = Option.none();    // optional encoded byte buffer
        while (dcb.hasRemaining()) {
            dcb.mark();
            final char dc = dcb.get(); // decoded character
            if (dc < 0x80) {
                final Option<String> oes = oess[dc]; // optional escape sequence
                if (!(oes.isEmpty() || '%' == dc && isRaw())) {
                    if (oebb.isEmpty())
                        oebb = Option.some(ByteBuffer.allocate(3));
                    esb.append(oes.get());
                }  else {
                    esb.append(dc);
                }
            } else if (Character.isISOControl(dc) ||
                       Character.isSpaceChar(dc)  ||
                       encode) {
                if (oebb.isEmpty())
                    oebb = Option.some(ByteBuffer.allocate(3));
                final int p = dcb.position();
                dcb.reset();
                dcb.limit(p);
                { // Encode dcb -> ebb.
                    CoderResult cr;
                    if (UNDERFLOW != (cr = encoder.reset().encode(dcb, oebb.get(), true))
                            || UNDERFLOW != (cr = encoder.flush(oebb.get()))) {
                        assert OVERFLOW != cr;
                        throw new QuotedUriSyntaxException(ds, cr.toString());
                    }
                }
                oebb.get().flip();
                quote(oebb.get(), esb);
                oebb.get().clear();
                dcb.limit(dcb.capacity());
            } else {
                esb.append(dc);
            }
        }
        return !oebb.isEmpty();
    }

    private static void quote(char dc, StringBuilder eS) {
        quote(UTF_8.encode(CharBuffer.wrap(Character.toString(dc))), eS);
    }

    private static void quote(final ByteBuffer eB, final StringBuilder eS) {
        while (eB.hasRemaining()) {
            final byte eb = eB.get();
            eS.append('%');
            eS.append(HEX[(eb >> 4) & 0xf]);
            eS.append(HEX[ eb       & 0xf]);
        }
    }

    /**
     * Defines the escape sequences for illegal characters in various URI
     * components.
     */
    enum Encoding {

        /**
         * Encoding which can be safely used for any URI component, except the
         * URI scheme component which does not allow escape sequences.
         * This encoding may produce redundant escape sequences, however.
         */
        ANY(DEFAULT_LEGAL_CHARS),

        /** Encoding for exclusive use with the URI authority component. */
        AUTHORITY(DEFAULT_LEGAL_CHARS + ":[]"),

        /**
         * Encoding for exclusive use with the URI path component
         * where the path may contain arbitrary characters.
         * This encoding may produce redundant escape sequences for absolute
         * paths, however.
         *
         * @see #ABSOLUTE_PATH
         */
        PATH(DEFAULT_LEGAL_CHARS + "/"),

        /**
         * Encoding for exclusive use with the URI path component
         * where the path starts with the separator character {@code '/'}.
         *
         * @see #PATH
         */
        ABSOLUTE_PATH(DEFAULT_LEGAL_CHARS + ":/"),

        /** Encoding for exclusive use with the URI query component. */
        QUERY(DEFAULT_LEGAL_CHARS + ":/?"),

        /** Encoding for exclusive use with the URI fragment component. */
        FRAGMENT(DEFAULT_LEGAL_CHARS + ":/?");

        private final Option[] escapes = new Option[0x80];

        Encoding(final String legal) {
            // Populate table of escape sequences.
            final StringBuilder sb = new StringBuilder();
            for (char c = 0; c < 0x80; c++) {
                if (0 <= legal.indexOf(c)) {
                    escapes[c] = Option.none();
                } else {
                    sb.setLength(0);
                    quote(c, sb);
                    escapes[c] = Option.some(sb.toString());
                }
            }
        }
    }
}