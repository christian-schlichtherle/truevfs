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
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import net.jcip.annotations.NotThreadSafe;

import static java.nio.charset.CoderResult.*;

/**
 * Static utility methods for encoding/decoding illegal characters in a URI
 * according to RFC&nbsp;2396.
 * The character set used for encoding/decoding is UTF-8.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @see <a target="_blank" href="http://www.ietf.org/rfc/rfc2396.txt">
 *      RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</a>
 */
@DefaultAnnotation(NonNull.class)
@NotThreadSafe
public final class URICodec {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String DECODED_REPLACEMENT = "\ufffd";
    private static final byte[] ENCODED_REPLACEMENT;
    static {
        final ByteBuffer eB = UTF8.encode(DECODED_REPLACEMENT);
        ENCODED_REPLACEMENT = new byte[eB.limit()];
        eB.get(ENCODED_REPLACEMENT);
    }

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final String
            ALPHANUM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String MARK_CHARS = "-_.!~*'()";
    private static final String
            DEFAULT_LEGAL_CHARS = ALPHANUM_CHARS + MARK_CHARS + ",;$&+=@";

    private final CharsetEncoder encoder = UTF8
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .replaceWith(ENCODED_REPLACEMENT);

    private final CharsetDecoder decoder = UTF8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .replaceWith(DECODED_REPLACEMENT);

    private @CheckForNull StringBuilder stringBuilder;

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
     * Encodes all characters in the given URI string which do <em>not</em>
     * belong to the character class "unreserved", that is,
     * which are neither alphanumeric nor in the string {@code "-_.!~*'()"}.
     * Illegal characters are encoded using UTF-8.
     * <p>
     * Note that calling this method on an already encoded URI string escapes
     * any getEscapeSequence sequences again, that is, each occurence of the character
     * {@code '%'} is substituted with the string {@code "%25"} again.
     * 
     * @param comp the URI component to encode.
     * @param dS the decoded string.
     * @return The encoded string.
     */
    public String encode(Component comp, String dS) {
        try {
            StringBuilder eS = encode(comp, dS, null);
            return eS != null ? eS.toString() : dS;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public @Nullable StringBuilder encode(
            final Component comp,
            final String dS,
            @CheckForNull StringBuilder eS)
    throws URISyntaxException {
        final String[] escapes = comp.escapes;
        final CharBuffer dC = CharBuffer.wrap(dS);  // decoded characters
        ByteBuffer eB = null;                       // encoded bytes
        CharsetEncoder enc = null;                  // encoder
        while (dC.hasRemaining()) {
            dC.mark();
            final char dc = dC.get();
            if (dc < 0x80) {
                final String es = escapes[dc];
                if (es != null) {
                    if (eS == null) {
                        eS = stringBuilder;
                        if (eS == null)
                            eS = stringBuilder = new StringBuilder();
                        else
                            eS.setLength(0);
                        eS.append(dS, 0, dC.position() - 1); // prefix until current character
                        eB = ByteBuffer.allocate(3);
                        enc = encoder;
                    }
                    eS.append(es);
                }  else {
                    if (eS != null)
                        eS.append(dc);
                }
            }  else {
                if (eS == null) {
                    eS = stringBuilder;
                    if (eS == null)
                        eS = stringBuilder = new StringBuilder();
                    else
                        eS.setLength(0);
                    eS.append(dS, 0, dC.position() - 1); // prefix until current character
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
        return eS;
    }

    /**
     * Decodes all escaped characters in the given URI string, that is,
     * each occurence of "%<i>XX</i>", where <i>X</i> is a hexadecimal digit,
     * gets substituted with the corresponding single byte and the resulting
     * string gets decoded using UTF-8 as the character set.
     * 
     * @param eS the encoded string.
     * @return The decoded string.
     */
    public String decode(String eS) {
        try {
            StringBuilder dS = decode(eS, null);
            return dS != null ? dS.toString() : eS;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public StringBuilder decode(
            final String eS,
            @CheckForNull StringBuilder dS)
    throws URISyntaxException {
        final CharBuffer eC = CharBuffer.wrap(eS);  // encoded characters
        ByteBuffer eB = null;                       // encoded bytes
        CharsetDecoder dec = null;                  // decoder
        CharBuffer dC = null;                       // decoded characters
        while (true) {
            eC.mark();
            final int ec = eC.hasRemaining() ? eC.get() : -1; // char is unsigned!
            if (ec == '%') {
                if (eB == null) {
                    dS = stringBuilder;
                    if (dS == null)
                        dS = stringBuilder = new StringBuilder();
                    else
                        dS.setLength(0);
                    dS.append(eS, 0, eC.position() - 1); // prefix until current character
                    final int l = eC.remaining();
                    eB = ByteBuffer.allocate((l + 1) / 3 * ENCODED_REPLACEMENT.length);
                    dC = CharBuffer.allocate(l);
                    dec = decoder;
                }
                final int eb = dequote(eC);
                if (eb < 0)
                    throw new URISyntaxException(
                            eS,
                            "illegal escape sequence",
                            eC.reset().position());
                eB.put((byte) eb);
            }  else {
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
        return dS;
    }

    public enum Component {
        //SCHEME,
        DEFAULT(DEFAULT_LEGAL_CHARS),
        AUTHORITY(DEFAULT_LEGAL_CHARS + ":"),
        ABSOLUTE_PATH(DEFAULT_LEGAL_CHARS + ":/"),
        PATH(DEFAULT_LEGAL_CHARS + "/"),
        QUERY(DEFAULT_LEGAL_CHARS + ":/?"),
        FRAGMENT(DEFAULT_LEGAL_CHARS + ":/?");

        private final String[] escapes = new String[0x80];

        private Component(final String legal) {
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
