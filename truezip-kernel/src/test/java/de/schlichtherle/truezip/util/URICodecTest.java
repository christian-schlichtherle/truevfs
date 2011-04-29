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

import java.util.logging.Logger;
import de.schlichtherle.truezip.util.URICodec.Component;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;

import static de.schlichtherle.truezip.util.URICodec.Component.*;
import static java.util.logging.Level.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class URICodecTest {

    private static final Logger logger
            = Logger.getLogger(URICodecTest.class.getName());

    private URICodec codec;

    @Before
    public void setUp() {
        this.codec = new URICodec();
    }

    @Test
    public void testNull() {
        try {
            codec.encode(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            codec.encode(null, ANY);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            codec.encode("", null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            codec.decode(null);
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
                codec.decode(test[0]);
                fail();
            } catch (IllegalArgumentException ex) {
                logger.log(FINE, ex.toString(), ex);
            }
        }
    }

    @Test
    public void testRoundTrip() {
        for (final Object[] test : new Object[][] {
            { EnumSet.allOf(Component.class), "\ufffd", "%EF%BF%BD" }, // replacement character
            { EnumSet.allOf(Component.class), "abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz" },
            { EnumSet.allOf(Component.class), "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ" },
            { EnumSet.allOf(Component.class), "0123456789", "0123456789" },
            { EnumSet.allOf(Component.class), "_-!.~'()*", "_-!.~'()*" }, // mark
            { EnumSet.allOf(Component.class), "@", "@" },
            { EnumSet.of(ANY, PATH), ":", "%3A" },
            { EnumSet.of(AUTHORITY, ABSOLUTE_PATH, QUERY, FRAGMENT), ":", ":" },
            { EnumSet.of(ANY, AUTHORITY), "/", "%2F" },
            { EnumSet.of(ABSOLUTE_PATH, PATH, QUERY, FRAGMENT), "/", "/" },
            { EnumSet.of(ANY, AUTHORITY, ABSOLUTE_PATH, PATH), "?", "%3F" },
            { EnumSet.of(QUERY, FRAGMENT), "?", "?" },
            { EnumSet.allOf(Component.class), "#", "%23" },
            { EnumSet.allOf(Component.class), "%", "%25" }, // percent
            { EnumSet.allOf(Component.class), "%a", "%25a" }, // percent adjacent
            { EnumSet.allOf(Component.class), "a%", "a%25" }, // percent adjacent
            { EnumSet.allOf(Component.class), "%%", "%25%25" }, // percents
            { EnumSet.allOf(Component.class), "a%b", "a%25b" }, // percent embedded
            { EnumSet.allOf(Component.class), "%a%", "%25a%25" }, // reverse embedded
            { EnumSet.allOf(Component.class), " ", "%20" },
            { EnumSet.allOf(Component.class), "\u0000", "%00" }, // control
            { EnumSet.allOf(Component.class), "\u20ac", "%E2%82%AC" }, // Euro sign
            { EnumSet.allOf(Component.class), "a\u20acb", "a%E2%82%ACb" }, // dito embedded
            { EnumSet.allOf(Component.class), "\u20aca\u20ac", "%E2%82%ACa%E2%82%AC" }, // inverse embedding
            { EnumSet.allOf(Component.class), "\u00c4\u00d6\u00dc\u00df\u00e4\u00f6\u00fc", "%C3%84%C3%96%C3%9C%C3%9F%C3%A4%C3%B6%C3%BC" }, // German diaeresis and sharp s
            { EnumSet.allOf(Component.class), "a\u00c4b\u00d6c\u00dcd\u00dfe\u00e4f\u00f6g\u00fch", "a%C3%84b%C3%96c%C3%9Cd%C3%9Fe%C3%A4f%C3%B6g%C3%BCh" }, // dito embedded
        }) {
            for (final Component component : (EnumSet<Component>) test[0])
                assertEquals(test[2], codec.encode(test[1].toString(), component));
            assertEquals(test[1], codec.decode(test[2].toString()));
        }
    }
}
