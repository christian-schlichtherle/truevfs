/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truevfs.comp.zip.UShort;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * A simple round trip test of the static utility methods for unsigned short
 * integers.
 * 
 * @author Christian Schlichtherle
 */
public final class UShortTest {

    @Test
    public void testCheck() {
        try {
            UShort.check(UShort.MIN_VALUE - 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        UShort.check(UShort.MIN_VALUE);
        UShort.check(UShort.MAX_VALUE);

        try {
            UShort.check(UShort.MAX_VALUE + 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }
    }
}