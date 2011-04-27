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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class URICodecTest {

    @Test
    public void testDecode() {
        for (final String[] test : new String[][] {
            { "\ufffd", "%ZZ" },
        }) {
            assertEquals(test[0], URICodec.decode(test[1]));
        }
    }

    @Test
    public void testRoundTrip() {
        for (final String[] test : new String[][] {
            { null, null },
            { "\ufffd", "%EF%BF%BD" }, // replacement character
            { "abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz" },
            { "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ" },
            { "_-!.~'()*", "_-!.~'()*" }, // mark
            { "0123456789", "0123456789" },
            { ":", "%3A" },
            { "%", "%25" }, // percent
            { "%a", "%25a" }, // percent adjacent
            { "a%", "a%25" }, // percent adjacent
            { "%%", "%25%25" }, // percents
            { "a%b", "a%25b" }, // percent embedded
            { "%a%", "%25a%25" }, // reverse embedded
            { "@", "%40" },
            { " ", "%20" },
            { "\u0000", "%00" }, // control
            { "\u20ac", "%E2%82%AC" }, // Euro sign
            { "a\u20acb", "a%E2%82%ACb" }, // dito embedded
            { "\u20aca\u20ac", "%E2%82%ACa%E2%82%AC" }, // inverse embedding
            { "\u00c4\u00d6\u00dc\u00df\u00e4\u00f6\u00fc", "%C3%84%C3%96%C3%9C%C3%9F%C3%A4%C3%B6%C3%BC" }, // German diaeresis and sharp s
            { "a\u00c4b\u00d6c\u00dcd\u00dfe\u00e4f\u00f6g\u00fch", "a%C3%84b%C3%96c%C3%9Cd%C3%9Fe%C3%A4f%C3%B6g%C3%BCh" }, // dito embedded
        }) {
            assertEquals(test[1], URICodec.encode(test[0]));
            assertEquals(test[0], URICodec.decode(test[1]));
        }
    }
}
