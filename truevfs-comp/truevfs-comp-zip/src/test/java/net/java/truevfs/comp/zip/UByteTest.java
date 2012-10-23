/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * A simple round trip test of the static utility methods for unsigned byte
 * integers.
 *
 * @author Christian Schlichtherle
 */
public final class UByteTest {

    @Test
    public void testCheck() {
        try {
            UByte.validate(UByte.MIN_VALUE - 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        UByte.validate(UByte.MIN_VALUE);
        UByte.validate(UByte.MAX_VALUE);

        try {
            UByte.validate(UByte.MAX_VALUE + 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }
    }
}