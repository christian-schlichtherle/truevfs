/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx.keychain;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import net.java.truevfs.key.macosx.keychain.Keychain.AttributeClass;
import static net.java.truevfs.key.macosx.keychain.KeychainUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class KeychainUtilsTest {

    @Test
    public void testMapRoundTrip() {
        final Map<AttributeClass, ByteBuffer> input = new EnumMap<>(AttributeClass.class);
        int count = 0;
        for (final AttributeClass id : AttributeClass.values())
            input.put(id, ByteBuffer.allocateDirect(++count));
        final Map<AttributeClass, ByteBuffer> output = map(list(input));
        assertThat(output.size(), is(input.size()));
        for (final AttributeClass id : AttributeClass.values()) {
            final ByteBuffer ib = input.get(id), ob = output.get(id);
            assertNotSame("Same byte buffers on iteration "+ id, ib, ob);
            assertEquals("Unequal byte buffers on iteration " + id, ib, ob);
        }
    }
}
