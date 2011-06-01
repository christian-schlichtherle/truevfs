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
package de.schlichtherle.truezip.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class OctetCharsetTestSuite {

    private Charset charset;

    @Before
    public void setUp() throws Exception {
        charset = newCharset();
    }

    protected abstract Charset newCharset();

    @Test
    public void testRoundTrip() throws CharacterCodingException {
        final CharsetDecoder dec = charset.newDecoder();
        final CharsetEncoder enc = charset.newEncoder();
        final byte[] b1 = new byte[256];
        for (int i = 0; i < b1.length; i++)
            b1[i] = (byte) i;
        final ByteBuffer bb1 = ByteBuffer.wrap(b1);
        final CharBuffer cb = dec.decode(bb1);
        final ByteBuffer bb2 = enc.encode(cb);
        final byte[] b2 = bb2.array();
        assertTrue(Arrays.equals(b1, b2));
    }
}
