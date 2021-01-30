/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.macosx.keychain;

import net.java.truecommons3.key.macosx.keychain.Keychain.AttributeClass;
import net.java.truecommons3.shed.Option;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

import static net.java.truecommons3.key.macosx.keychain.KeychainUtils.list;
import static net.java.truecommons3.key.macosx.keychain.KeychainUtils.map;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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
        final Map<AttributeClass, ByteBuffer> output = map(list(Option.some(input))).get();
        assertThat(output.size(), is(input.size()));
        for (final AttributeClass id : AttributeClass.values()) {
            final ByteBuffer ib = input.get(id), ob = output.get(id);
            assertNotSame("Same byte buffers on iteration "+ id, ib, ob);
            assertEquals("Unequal byte buffers on iteration " + id, ib, ob);
        }
    }
}
