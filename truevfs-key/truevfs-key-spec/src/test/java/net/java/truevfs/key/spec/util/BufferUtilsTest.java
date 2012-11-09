/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static net.java.truevfs.key.spec.util.BufferUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class BufferUtilsTest {

    @Test
    public void testStringRoundTrip() {
        final String ist = "föö";
        final ByteBuffer ibb = byteBuffer(ist);
        final ByteBuffer obb = ibb.duplicate();
        final String ost = string(obb);
        assertThat(ost, is(ist));
        assertThat(obb, is(ibb));
    }

    @Test
    public void testCharArrayRoundTrip() {
        final char[] ica = "bär".toCharArray();
        final ByteBuffer ibb = byteBuffer(ica);
        final ByteBuffer obb = ibb.duplicate();
        final char[] oca = charArray(obb);
        assertThat(oca, is(ica));
        assertThat(obb, is(ibb));
    }

    @Test
    public void testCharBufferRoundTrip() {
        final CharBuffer icb = CharBuffer.wrap("bäz");
        final ByteBuffer ibb = byteBuffer(icb);
        final ByteBuffer obb = ibb.duplicate();
        final CharBuffer ocb = charBuffer(obb);
        assertThat(ocb, is(icb));
        assertThat(obb, is(ibb));
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
