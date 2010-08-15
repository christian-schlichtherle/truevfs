/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.nio.charset;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

import junit.framework.*;

/**
 * @author Christian Schlichtherle
 * @version $Revision$
 */
public abstract class OctetCharsetTestCase extends TestCase {

    protected Charset charset;
    
    protected OctetCharsetTestCase(String testName) {
        super(testName);
    }

    /** Must set charset. */
    protected abstract void setUp() throws Exception;

    protected void tearDown() throws Exception {
        charset = null;
    }

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
