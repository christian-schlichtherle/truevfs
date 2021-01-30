/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static net.java.truecommons3.shed.Buffers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class BuffersTest {

    private static final String[] tests = { "", "föo", "bär" };

    @Test
    public void testStringRoundTrip() {
        for (final String ist : tests) {
            final ByteBuffer ibb = byteBuffer(ist);
            final ByteBuffer obb = ibb.duplicate();
            final String ost = string(obb);
            assertThat(ost, is(ist));
            assertThat(obb, is(ibb));
        }
    }

    @Test
    public void testCharArrayRoundTrip() {
        for (final String ist : tests) {
            final char[] ica = ist.toCharArray();
            final ByteBuffer ibb = byteBuffer(ica);
            final ByteBuffer obb = ibb.duplicate();
            final char[] oca = charArray(obb);
            assertThat(oca, is(ica));
            assertThat(obb, is(ibb));
        }
    }

    @Test
    public void testCharBufferRoundTrip() {
        for (final String ist : tests) {
            final CharBuffer icb = CharBuffer.wrap(ist);
            final ByteBuffer ibb = byteBuffer(icb);
            final ByteBuffer obb = ibb.duplicate();
            final CharBuffer ocb = charBuffer(obb);
            assertThat(ocb, is(icb));
            assertThat(obb, is(ibb));
        }
    }

    @Test
    public void testNullConversions() {
        assertNull(byteBuffer((String) null));
        assertNull(byteBuffer((char[]) null));
        assertNull(byteBuffer((CharBuffer) null));
        assertNull(string((ByteBuffer) null));
        assertNull(charArray((ByteBuffer) null));
        assertNull(charBuffer((ByteBuffer) null));
    }
}
