/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import java.util.logging.Logger;
import de.schlichtherle.truezip.util.UriEncoder.Encoding;
import org.junit.Before;
import org.junit.Test;

import static de.schlichtherle.truezip.util.UriEncoder.Encoding.*;
import static java.util.logging.Level.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class UriCodecTest {

    private static final BitField<Encoding>
            ENCODING_MASK = BitField.allOf(Encoding.class);
    private static final Logger logger
            = Logger.getLogger(UriCodecTest.class.getName());

    private UriEncoder encoder;
    private UriDecoder decoder;

    @Before
    public void setUp() {
        this.encoder = new UriEncoder();
        this.decoder = new UriDecoder();
    }

    @Test
    public void testNull() {
        try {
            encoder.encode(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            encoder.encode(null, ANY);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            encoder.encode("", null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            decoder.decode(null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testIllegalEncodedString() {
        for (final String[] test : new String[][] {
            { "a%ZZ" },
            { "a%ZZb" },
            { "a%EF%BF" },
            { "a%EF%BFb" },
        }) {
            try {
                decoder.decode(test[0]);
                fail();
            } catch (IllegalArgumentException ex) {
                logger.log(FINE, ex.toString(), ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRoundTrip() {
        for (final Object[] test : new Object[][] {
            { ENCODING_MASK, "\ufffd", "%EF%BF%BD" }, // replacement character
            { ENCODING_MASK, "abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz" },
            { ENCODING_MASK, "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ" },
            { ENCODING_MASK, "0123456789", "0123456789" },
            { ENCODING_MASK, "_-!.~'()*", "_-!.~'()*" }, // mark
            { ENCODING_MASK, "@", "@" },
            { BitField.of(ANY, PATH), ":", "%3A" },
            { BitField.of(AUTHORITY, ABSOLUTE_PATH, QUERY, FRAGMENT), ":", ":" },
            { BitField.of(ANY, AUTHORITY), "/", "%2F" },
            { BitField.of(ABSOLUTE_PATH, PATH, QUERY, FRAGMENT), "/", "/" },
            { BitField.of(ANY, AUTHORITY, ABSOLUTE_PATH, PATH), "?", "%3F" },
            { BitField.of(QUERY, FRAGMENT), "?", "?" },
            { ENCODING_MASK, "#", "%23" },
            { ENCODING_MASK, "%", "%25" }, // percent
            { ENCODING_MASK, "%a", "%25a" }, // percent adjacent
            { ENCODING_MASK, "a%", "a%25" }, // percent adjacent
            { ENCODING_MASK, "%%", "%25%25" }, // percents
            { ENCODING_MASK, "a%b", "a%25b" }, // percent embedded
            { ENCODING_MASK, "%a%", "%25a%25" }, // reverse embedded
            { ENCODING_MASK, " ", "%20" },
            { ENCODING_MASK, "\u0000", "%00" }, // control
            { ENCODING_MASK, "\u20ac", "%E2%82%AC" }, // Euro sign
            { ENCODING_MASK, "a\u20acb", "a%E2%82%ACb" }, // dito embedded
            { ENCODING_MASK, "\u20aca\u20ac", "%E2%82%ACa%E2%82%AC" }, // inverse embedding
            { ENCODING_MASK, "\u00c4\u00d6\u00dc\u00df\u00e4\u00f6\u00fc", "%C3%84%C3%96%C3%9C%C3%9F%C3%A4%C3%B6%C3%BC" }, // German diaeresis and sharp s
            { ENCODING_MASK, "a\u00c4b\u00d6c\u00dcd\u00dfe\u00e4f\u00f6g\u00fch", "a%C3%84b%C3%96c%C3%9Cd%C3%9Fe%C3%A4f%C3%B6g%C3%BCh" }, // dito embedded
        }) {
            for (final Encoding component : (BitField<Encoding>) test[0])
                assertEquals(test[2], encoder.encode(test[1].toString(), component));
            assertEquals(test[1], decoder.decode(test[2].toString()));
        }
    }
}
