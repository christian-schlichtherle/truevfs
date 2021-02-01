/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macosx.keychain;

import global.namespace.truevfs.comp.key.macosx.keychain.Keychain.AttributeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

import static global.namespace.truevfs.comp.key.macosx.keychain.KeychainUtils.list;
import static global.namespace.truevfs.comp.key.macosx.keychain.KeychainUtils.map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

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
            assertNotSame("Same byte buffers on iteration " + id, ib, ob);
            assertEquals("Unequal byte buffers on iteration " + id, ib, ob);
        }
    }
}
