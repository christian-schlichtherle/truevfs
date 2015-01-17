/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truevfs.comp.zip.UByte;
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
            UByte.check(UByte.MIN_VALUE - 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        UByte.check(UByte.MIN_VALUE);
        UByte.check(UByte.MAX_VALUE);

        try {
            UByte.check(UByte.MAX_VALUE + 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }
    }
}