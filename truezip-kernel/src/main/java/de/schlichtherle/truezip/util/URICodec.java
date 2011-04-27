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
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * Static utility methods for encoding/decoding illegal characters in a URI
 * according to RFC&nbsp;2396.
 * The character set used for encoding/decoding is UTF-8.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @see <a target="_blank" href="http://www.ietf.org/rfc/rfc2396.txt">
 *      <i>RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</i></a>
 */
@DefaultAnnotation({ CheckReturnValue.class, CheckForNull.class })
public final class URICodec {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final ThreadLocal<CharsetEncoder> encoders
            = new ThreadLocal<CharsetEncoder>() {
        @Override
        protected CharsetEncoder initialValue() {
            return UTF8 .newEncoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)
                        .replaceWith(ENCODED_REPLACEMENT);
        }
    };

    private static final ThreadLocal<CharsetDecoder> decoders
            = new ThreadLocal<CharsetDecoder>() {
        @Override
        protected CharsetDecoder initialValue() {
            return UTF8 .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)
                        .replaceWith(DECODED_REPLACEMENT);
        }
    };

    private static final String DECODED_REPLACEMENT = "\ufffd";
    private static final byte[] ENCODED_REPLACEMENT;
    static {
        final ByteBuffer eB = UTF8.encode(DECODED_REPLACEMENT);
        ENCODED_REPLACEMENT = new byte[eB.limit()];
        eB.get(ENCODED_REPLACEMENT);
    }

    private static final ThreadLocal<StringBuilder> stringBuilders
            = new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder(512);
        }
    };

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final String[] escapes = new String[0x80];

    // Populate encoded lookup table.
    static {
        final StringBuilder sb = stringBuilders.get();
        for (char c = 0; c < (char) 0x80; c++) {
            switch (Character.getType(c)) {
                case Character.DECIMAL_DIGIT_NUMBER:
                case Character.LOWERCASE_LETTER:
                case Character.UPPERCASE_LETTER:
                    continue;

                default:
                    if ("-_.!~*'()".indexOf(c) >= 0) // mark
                        continue;

                    sb.setLength(0);
                    escape(c, sb);
                    escapes[c] = sb.toString();
            }
        }
    }

    private static void escape(final char dc, @NonNull final StringBuilder eS) {
        escape(UTF8.encode(CharBuffer.wrap(Character.toString(dc))), eS);
    }

    private static void escape(
            @NonNull final ByteBuffer eB,
            @NonNull final StringBuilder eS) {
        while (eB.hasRemaining()) {
            final byte eb = eB.get();
            eS.append('%');
            eS.append(HEX[(eb >> 4) & 0xf]);
            eS.append(HEX[ eb       & 0xf]);
        }
    }

    private static int unescape(@NonNull final CharBuffer eC) {
        if (eC.hasRemaining()) {
            final char ec0 = eC.get();
            if (eC.hasRemaining()) {
                final char ec1 = eC.get();
                return (unescape(ec0) << 4) | unescape(ec1);
            }
        }
        return -1;
    }

    private static int unescape(char ec) {
	if ('0' <= ec && ec <= '9')
	    return ec - '0';
        ec &= ~(2 << 4); // toUpperCase for 'a' to 'z'
	/*if ('a' <= c && c <= 'f')
	    return c - 'a' + 10;*/
	if ('A' <= ec && ec <= 'F')
	    return ec - 'A' + 10;
	return -1;
    }

    /**
     * Encodes all characters in the given URI string which do <em>not</em>
     * belong to the character class "unreserved", that is,
     * which are neither alphanumeric nor in the string {@code "-_.!~*'()"}.
     * Illegal characters are encoded using UTF-8.
     * <p>
     * Note that calling this method on an already encoded URI string escapes
     * any escape sequences again, that is, each occurence of the character
     * {@code '%'} is substituted with the string {@code "%25"} again.
     * 
     * @param dS The decoded string - may be {@code null}.
     * @return The encoded string.
     *         This is {@code null} if and only if {@code dS} is {@code null}.
     * @see <a target="_blank" href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396 Appendix&nbsp;A</a>
     */
    @Nullable
    public static String encode(@CheckForNull final String dS) {
        if (dS == null)
            return null;

        final CharBuffer dC = CharBuffer.wrap(dS);  // decoded characters
        ByteBuffer eB = null;                       // encoded bytes
        CharsetEncoder enc = null;                  // encoder
        StringBuilder eS = null;                    // encoded string
        while (dC.hasRemaining()) {
            dC.mark();
            final char dc = dC.get();
            if (dc < 0x80) {
                final String es = escapes[dc];
                if (es != null) {
                    if (eS == null) {
                        eB = ByteBuffer.allocate(3);
                        enc = encoders.get();
                        eS = stringBuilders.get();
                        eS.setLength(0);
                        eS.append(dS, 0, dC.position() - 1); // prefix until current character
                    }
                    eS.append(es);
                }  else {
                    if (eS != null)
                        eS.append(dc);
                }
            }  else {
                if (eS == null) {
                    eB = ByteBuffer.allocate(3);
                    enc = encoders.get();
                    eS = stringBuilders.get();
                    eS.setLength(0);
                    eS.append(dS, 0, dC.position() - 1); // prefix until current character
                }
                final int p = dC.position();
                dC.reset();
                dC.limit(p);
                enc.reset().encode(dC, eB, true);
                enc.flush(eB);
                eB.flip();
                escape(eB, eS);
                eB.clear();
                dC.position(p);
                dC.limit(dC.capacity());
            }
        }
        return eS != null ? eS.toString() : dS;
    }

    /**
     * Decodes all escaped characters in the given URI string, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded using UTF-8 as the character set.
     * 
     * @param eS The encoded string - may be {@code null}.
     * @return The decoded string.
     *         This is {@code null} if and only if {@code eS} is {@code null}.
     * @see <a target="_blank" href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396 Appendix&nbsp;A</a>
     */
    @Nullable
    public static String decode(@CheckForNull final String eS) {
        if (eS == null)
            return null;

        final CharBuffer eC = CharBuffer.wrap(eS);  // encoded characters
        ByteBuffer eB = null;                       // encoded bytes
        CharsetDecoder dec = null;                  // decoder
        CharBuffer dC = null;                       // decoded characters
        StringBuilder dS = null;                    // decoded string
        while (true) {
            final int ec = eC.hasRemaining() ? eC.get() : -1; // char is unsigned!
            if (ec == '%') {
                if (eB == null) {
                    final int l = eC.remaining();
                    eB = ByteBuffer.allocate((l + 1) / 3 * ENCODED_REPLACEMENT.length);
                    dC = CharBuffer.allocate(l);
                    dec = decoders.get();
                    dS = stringBuilders.get();
                    dS.setLength(0);
                    dS.append(eS, 0, eC.position() - 1); // prefix until current character
                }
                final int eb = unescape(eC);
                if (eb >= 0)
                    eB.put((byte) eb);
                else
                    eB.put(ENCODED_REPLACEMENT);
            }  else {
                if (eB != null && eB.position() > 0) {
                    eB.flip();
                    dec.reset().decode(eB, dC, true);
                    dec.flush(dC);
                    eB.clear();
                    dC.flip();
                    dS.append(dC);
                    dC.clear();
                }
                if (ec >= 0) {
                    if (dS != null)
                        dS.append((char) ec);
                }  else {
                    break;
                }
            }
        }
        return dS != null ? dS.toString() : eS;
    }

    /** You cannot instantiate this class. */
    protected URICodec() {
    }
}
