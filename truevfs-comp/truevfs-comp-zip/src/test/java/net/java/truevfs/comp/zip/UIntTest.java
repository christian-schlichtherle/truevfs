/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truevfs.comp.zip.UInt;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * A simple round trip test of the static utility methods for unsigned
 * integers.
 * 
 * @author Christian Schlichtherle
 */
public final class UIntTest {

    @Test
    public void testCheck() {
        try {
            UInt.check(UInt.MIN_VALUE - 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        UInt.check(UInt.MIN_VALUE);
        UInt.check(UInt.MAX_VALUE);

        try {
            UInt.check(UInt.MAX_VALUE + 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }
    }
}