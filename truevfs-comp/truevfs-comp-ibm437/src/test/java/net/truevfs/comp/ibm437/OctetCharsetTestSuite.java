/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
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