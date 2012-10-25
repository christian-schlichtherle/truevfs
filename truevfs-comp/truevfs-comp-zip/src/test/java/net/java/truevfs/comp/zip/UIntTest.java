/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

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
            UInt.validate(UInt.MIN_VALUE - 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        UInt.validate(UInt.MIN_VALUE);
        UInt.validate(UInt.MAX_VALUE);

        try {
            UInt.validate(UInt.MAX_VALUE + 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }
    }
}