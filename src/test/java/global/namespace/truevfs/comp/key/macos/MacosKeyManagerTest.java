/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macos;

import global.namespace.truevfs.comp.key.api.aes.AesKeyStrength;
import global.namespace.truevfs.comp.key.api.aes.AesPbeParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static global.namespace.truevfs.comp.key.macos.MacosKeyManager.deserialize;
import static global.namespace.truevfs.comp.key.macos.MacosKeyManager.serialize;
import static global.namespace.truevfs.comp.util.Buffers.string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Christian Schlichtherle
 */
public class MacosKeyManagerTest {

    private static final Logger logger = LoggerFactory.getLogger(MacosKeyManagerTest.class);

    @Test
    public void testXmlSerialization() {
        final AesPbeParameters original = new AesPbeParameters();
        original.setChangeRequested(true);
        original.setKeyStrength(AesKeyStrength.BITS_256);
        original.setPassword("föo".toCharArray());
        final ByteBuffer xml = serialize(original); // must not serialize password!

        logger.trace("Serialized form ({} bytes):\n{}", xml.remaining(), string(xml));

        final AesPbeParameters clone = deserialize(xml);
        assertNull(clone.getPassword());
        original.setPassword(new char[0]);
        assertEquals(original, clone);
    }
}
