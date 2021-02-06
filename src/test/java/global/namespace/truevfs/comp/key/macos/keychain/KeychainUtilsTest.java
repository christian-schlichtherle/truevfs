/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macos.keychain;

import global.namespace.truevfs.comp.key.macos.keychain.Keychain.AttributeClass;
import lombok.val;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.EnumMap;

import static global.namespace.truevfs.comp.key.macos.keychain.KeychainUtils.list;
import static global.namespace.truevfs.comp.key.macos.keychain.KeychainUtils.map;
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
        val input = new EnumMap<AttributeClass, ByteBuffer>(AttributeClass.class);
        int count = 0;
        for (val id : AttributeClass.values()) {
            input.put(id, ByteBuffer.allocateDirect(++count));
        }
        val output = map(list(input));
        assertThat(output.size(), is(input.size()));
        for (val id : AttributeClass.values()) {
            val ib = input.get(id);
            val ob = output.get(id);
            assertNotSame("Same byte buffers on iteration " + id, ib, ob);
            assertEquals("Unequal byte buffers on iteration " + id, ib, ob);
        }
    }
}
