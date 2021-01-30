/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import static java.nio.charset.CoderResult.OVERFLOW;
import static java.nio.charset.CoderResult.UNDERFLOW;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Unescapes characters in URI components according to
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
 * and its updates in
 * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
 * for IPv6 addresses.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">
 *      RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</a>
 * @see <a href="http://www.ietf.org/rfc/rfc2732.txt">
 *      RFC&nbsp;2732: Format for Literal IPv6 Addresses in URL's</a>
 * @see UriEncoder
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
final class UriDecoder {

    private final StringBuilder stringBuilder = new StringBuilder();
    private final CharsetDecoder decoder;

    /**
     * Constructs a new URI decoder which uses the UTF-8 character set to
     * decode non-US-ASCII characters.
     */
    UriDecoder() { this(Option.<Charset>none()); }

    /**
     * Constructs a new URI decoder which uses the given character set to
     * decode non-US-ASCII characters.
     * 
     * @param charset the character set to use for encoding non-US-ASCII
     *        characters.
     *        If this parameter is {@code null}, then it defaults to
     *        {@code UTF-8}.
     *        Note that providing any other value than {@code null} or
     *        {@code UTF-8} will void interoperability with most applications.
     */
    UriDecoder(Option<Charset> charset) {
        if (charset.isEmpty())
            charset = Option.some(UTF_8);
        this.decoder = charset.get().newDecoder();
    }

    /**
     * Decodes all escape sequences in the string {@code eS}, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded using the character set provided to the constructor.
     * 
     * @param  es the encoded string to decode.
     * @return The decoded string.
     * @throws IllegalArgumentException on any decoding error with a
     *         {@link URISyntaxException} as its
     *         {@link IllegalArgumentException#getCause() cause}.
     */
    String decode(String es) {
        stringBuilder.setLength(0);
        try {
            if (decode(es, stringBuilder))
                return stringBuilder.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return es;
    }

    /**
     * Decodes all escape sequences in the string {@code eS}, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded to the string builder {@code dS} using the character
     * set provided to the constructor.
     * 
     * @param es the encoded string to decode.
     * @param dsb the string builder to which all decoded characters shall get
     *            appended.
     * @return Whether or not any characters in {@code es} had to be decoded.
     * @throws URISyntaxException on any decoding error.
     *         This exception will leave {@code dsb} in an undefined state.
     */
    boolean decode(final String es, final StringBuilder dsb)
    throws URISyntaxException {
        final CharBuffer ecb = CharBuffer.wrap(es); // encoded character buffer
        Option<ByteBuffer> oebb = Option.none();    // optional encoded byte buffer
        Option<CharBuffer> odcb = Option.none();    // optional decoded character buffer
        while (true) {
            ecb.mark();
            final int ec = ecb.hasRemaining() ? (ecb.get() & 0xFFFF) : -1; // encoded character (unsigned!)
            if ('%' == ec) {
                if (oebb.isEmpty()) {
                    final int capacity = (ecb.remaining() + 1) / 3;
                    oebb = Option.some(ByteBuffer.allocate(capacity));
                    odcb = Option.some(CharBuffer.allocate(capacity));
                }
                final int eb = dequote(ecb); // encoded byte
                if (eb < 0)
                    throw new URISyntaxException(es, "illegal escape sequence", ecb.reset().position());
                oebb.get().put((byte) eb);
            } else {
                if (!oebb.isEmpty() && 0 < oebb.get().position()) {
                    oebb.get().flip();
                    { // Decode ebb -> dcb.
                        CoderResult cr;
                        if (UNDERFLOW != (cr = decoder.reset().decode(oebb.get(), odcb.get(), true))
                                || UNDERFLOW != (cr = decoder.flush(odcb.get()))) {
                            assert OVERFLOW != cr;
                            throw new QuotedUriSyntaxException(es, cr.toString());
                        }
                    }
                    oebb.get().clear();
                    odcb.get().flip();
                    dsb.append(odcb.get());
                    odcb.get().clear();
                }
                if (0 > ec)
                    break;
                dsb.append((char) ec);
            }
        }
        return !oebb.isEmpty();
    }

    private static int dequote(final CharBuffer ecb) {
        if (ecb.hasRemaining()) {
            final char ec0 = ecb.get();
            if (ecb.hasRemaining()) {
                final char ec1 = ecb.get();
                return (dequote(ec0) << 4) | dequote(ec1);
            }
        }
        return -1;
    }

    private static int dequote(char ec) {
        if ('0' <= ec && ec <= '9')
            return ec - '0';
        ec &= ~(2 << 4); // toUpperCase for 'a' to 'z'
        if ('A' <= ec && ec <= 'F')
            return ec - 'A' + 10;
        return -1;
    }
}