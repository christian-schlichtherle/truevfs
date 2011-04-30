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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import net.jcip.annotations.NotThreadSafe;

import static java.nio.charset.CoderResult.*;

/**
 * Encodes and decodes illegal characters in URI components according to
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
 * @version @version@
 */
@DefaultAnnotation(NonNull.class)
@NotThreadSafe
public final class UriCodec {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final String
            ALPHANUM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String MARK_CHARS = "-_.!~*'()";
    private static final String
            DEFAULT_LEGAL_CHARS = ALPHANUM_CHARS + MARK_CHARS + ",;$&+=@";

    private final CharsetEncoder encoder;
    private final CharsetDecoder decoder;

    private @CheckForNull StringBuilder stringBuilder;

    /**
     * Constructs a new URI codec which uses the UTF-8 character set to encode
     * non-US-ASCII characters.
     */
    public UriCodec() {
        this(UTF8);
    }

    /**
     * Constructs a new URI codec which uses the given character set to encode
     * non-US-ASCII characters.
     * <p>
     * <strong>WARNING:</strong> Using any other character set than UTF-8
     * should void interoperability with most applications!
     */
    public UriCodec(final Charset charset) {
        this.encoder = charset.newEncoder();
        this.decoder = charset.newDecoder();
    }

    private static void quote(final char dc, final StringBuilder eS) {
        quote(UTF8.encode(CharBuffer.wrap(Character.toString(dc))), eS);
    }

    private static void quote(final ByteBuffer eB, final StringBuilder eS) {
        while (eB.hasRemaining()) {
            final byte eb = eB.get();
            eS.append('%');
            eS.append(HEX[(eb >> 4) & 0xf]);
            eS.append(HEX[ eb       & 0xf]);
        }
    }

    private static int dequote(final CharBuffer eC) {
        if (eC.hasRemaining()) {
            final char ec0 = eC.get();
            if (eC.hasRemaining()) {
                final char ec1 = eC.get();
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

    /**
     * Encodes all characters in the string {@code dS} which are illegal within
     * the URI component {@code comp}.
     * <p>
     * Note that calling this method on an already encoded string escapes any
     * escape sequences again, that is, each occurence of the character
     * {@code '%'} is substituted with the string {@code "%25"} again.
     * 
     * @param  dS the decoded string to encode.
     * @param  comp the URI component to encode.
     * @return The encoded string.
     * @throws IllegalArgumentException on any encoding error with a
     *         {@link URISyntaxException} as its
     *         {@link IllegalArgumentException#getCause() cause}.
     *         This exception should never occur if the character set of this
     *         codec is UTF-8.
     */
    public String encode(String dS, Encoding comp) {
        try {
            StringBuilder eS = encode(dS, comp, null);
            return eS != null ? eS.toString() : dS;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Encodes all characters in the string {@code dS} which are illegal within
     * the URI component {@code comp} to the string builder {@code eS}.
     * <p>
     * Note that calling this method on an already encoded string escapes
     * any escape sequences again, that is, each occurence of the character
     * {@code '%'} is substituted with the string {@code "%25"} again.
     * 
     * @param  dS the decoded string to encode.
     * @param  comp the URI component to encode.
     * @param  eS the string builder to which all encoded characters shall get
     *         appended.
     * @return If {@code dS} contains only legal characters for the URI
     *         component {@code comp}, then {@code null} gets returned.
     *         Otherwise, if {@code eS} is not {@code null}, then it gets
     *         returned with all encoded characters appended to it.
     *         Otherwise, a temporary string builder gets returned which solely
     *         contains all encoded characters.
     *         This temporary string builder may get cleared and reused upon
     *         the next call to <em>any</em> method of this object.
     * @throws URISyntaxException on any encoding error.
     *         This exception should never occur if the character set of this
     *         codec is UTF-8.
     *         If it occurs however, {@code eS} is left in an undefined state.
     */
    public @CheckForNull StringBuilder encode(
            final String dS,
            final Encoding comp,
            @CheckForNull StringBuilder eS)
    throws URISyntaxException {
        final String[] escapes = comp.escapes;
        final CharBuffer dC = CharBuffer.wrap(dS);  // decoded characters
        ByteBuffer eB = null;                       // encoded bytes
        CharsetEncoder enc = null;
        while (dC.hasRemaining()) {
            dC.mark();
            final char dc = dC.get();               // decoded character
            if (dc < 0x80) {
                final String es = escapes[dc];
                if (es != null) {
                    if (eB == null) {
                        if (eS == null) {
                            if ((eS = stringBuilder) == null)
                                eS = stringBuilder = new StringBuilder();
                            else
                                eS.setLength(0);
                            eS.append(dS, 0, dC.position() - 1); // prefix until current character
                        }
                        eB = ByteBuffer.allocate(3);
                        enc = encoder;
                    }
                    eS.append(es);
                }  else {
                    if (eS != null)
                        eS.append(dc);
                }
            } else {
                if (eB == null) {
                    if (eS == null) {
                        if ((eS = stringBuilder) == null)
                            eS = stringBuilder = new StringBuilder();
                        else
                            eS.setLength(0);
                        eS.append(dS, 0, dC.position() - 1); // prefix until current character
                    }
                    eB = ByteBuffer.allocate(3);
                    enc = encoder;
                }
                final int p = dC.position();
                dC.reset();
                dC.limit(p);
                { // Encode dC -> eB.
                    CoderResult cr;
                    if ((cr = enc.reset().encode(dC, eB, true)) != UNDERFLOW
                            || (cr = enc.flush(eB)) != UNDERFLOW) {
                        assert cr != OVERFLOW;
                        throw new URISyntaxException(dS, cr.toString());
                    }
                }
                eB.flip();
                quote(eB, eS);
                eB.clear();
                dC.limit(dC.capacity());
            }
        }
        return eB == null ? null : eS;
    }

    /**
     * Decodes all escape sequences in the string {@code eS}, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded using the character set provided to the constructor.
     * 
     * @param  eS the encoded string to decode.
     * @return The decoded string.
     * @throws IllegalArgumentException on any decoding error with a
     *         {@link URISyntaxException} as its
     *         {@link IllegalArgumentException#getCause() cause}.
     */
    public String decode(String eS) {
        try {
            StringBuilder dS = decode(eS, null);
            return dS != null ? dS.toString() : eS;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Decodes all escape sequences in the string {@code eS}, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded to the string builder {@code dS} using the character
     * set provided to the constructor.
     * 
     * @param  eS the encoded string to decode.
     * @param  dS the string builder to which all decoded characters shall get
     *         appended.
     * @return If {@code eS} contains no escape sequences, then {@code null}
     *         gets returned.
     *         Otherwise, if {@code dS} is not {@code null}, then it gets
     *         returned with all decoded characters appended to it.
     *         Otherwise, a temporary string builder gets returned which solely
     *         contains all decoded characters.
     *         This temporary string builder may get cleared and reused upon
     *         the next call to <em>any</em> method of this object.
     * @throws URISyntaxException on any decoding error.
     *         This exception will leave {@code eS} in an undefined state.
     */
    public @CheckForNull StringBuilder decode(
            final String eS,
            @CheckForNull StringBuilder dS)
    throws URISyntaxException {
        final CharBuffer eC = CharBuffer.wrap(eS);  // encoded characters
        ByteBuffer eB = null;                       // encoded bytes
        CharBuffer dC = null;                       // decoded characters
        CharsetDecoder dec = null;
        while (true) {
            eC.mark();
            final int ec = eC.hasRemaining() ? eC.get() : -1; // char is unsigned!
            if (ec == '%') {
                if (eB == null) {
                    if (dS == null) {
                        if ((dS = stringBuilder) == null)
                            dS = stringBuilder = new StringBuilder();
                        else
                            dS.setLength(0);
                        dS.append(eS, 0, eC.position() - 1); // prefix until current character
                    }
                    int l = eC.remaining();
                    l = (l + 1) / 3;
                    eB = ByteBuffer.allocate(l);
                    dC = CharBuffer.allocate(l);
                    dec = decoder;
                }
                final int eb = dequote(eC);         // encoded byte
                if (eb < 0)
                    throw new URISyntaxException(
                            eS,
                            "illegal escape sequence",
                            eC.reset().position());
                eB.put((byte) eb);
            } else {
                if (eB != null && eB.position() > 0) {
                    eB.flip();
                    { // Decode eB -> dC.
                        CoderResult cr;
                        if ((cr = dec.reset().decode(eB, dC, true)) != UNDERFLOW
                                || (cr = dec.flush(dC)) != UNDERFLOW) {
                            assert cr != OVERFLOW;
                            throw new URISyntaxException(eS, cr.toString());
                        }
                    }
                    eB.clear();
                    dC.flip();
                    dS.append(dC);
                    dC.clear();
                }
                if (ec < 0)
                    break;
                if (dS != null)
                    dS.append((char) ec);
            }
        }
        return eB == null ? null : dS;
    }

    /**
     * Defines the escape sequences for illegal characters in various URI
     * components.
     */
    public enum Encoding {
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

        private final String[] escapes = new String[0x80];

        private Encoding(final String legal) {
            // Populate table of getEscapeSequence sequences.
            final StringBuilder sb = new StringBuilder();
            for (char c = 0; c < 0x80; c++) {
                if (legal.indexOf(c) >= 0)
                    continue;
                sb.setLength(0);
                quote(c, sb);
                escapes[c] = sb.toString();
            }
        }
    }
}
