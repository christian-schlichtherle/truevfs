/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx;

import java.nio.ByteBuffer;
import static net.java.truevfs.key.macosx.OsxKeyManager.*;
import net.java.truevfs.key.spec.param.AesKeyStrength;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import static net.java.truevfs.key.spec.util.BufferUtils.*;
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
        final ByteBuffer serialized = encode(original); // must not encode password!

        logger.trace("Serialized object to {} bytes.", serialized.remaining());
        logger.trace("Serialized form:\n{}", string(serialized));

        final AesPbeParameters clone = decode(serialized);
        original.setPassword(null);
        assertEquals(original, clone);
    }
}
