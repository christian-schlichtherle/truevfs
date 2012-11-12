/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx;

import java.nio.ByteBuffer;
import static net.java.truevfs.key.macosx.OsxKeyManager.*;
import net.java.truevfs.key.spec.common.AesKeyStrength;
import net.java.truevfs.key.spec.common.AesPbeParameters;
import static net.java.truevfs.key.spec.util.Buffers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
public class OsxKeyManagerTest {

    private static final Logger
            logger = LoggerFactory.getLogger(OsxKeyManagerTest.class);

    @Test
    public void testXmlSerialization() {
        final AesPbeParameters original = new AesPbeParameters();
        original.setChangeRequested(true);
        original.setKeyStrength(AesKeyStrength.BITS_256);
        original.setPassword("föo".toCharArray());
        final ByteBuffer xml = serialize(original); // must not serialize password!

        logger.trace("Serialized object to {} bytes.", xml.remaining());
        logger.trace("Serialized form:\n{}", string(xml));

        final AesPbeParameters clone = (AesPbeParameters) deserialize(xml);
        assertNull(clone.getPassword());
        original.setPassword(null);
        assertEquals(original, clone);
    }
}
