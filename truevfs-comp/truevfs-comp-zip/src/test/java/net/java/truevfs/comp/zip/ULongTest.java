/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truevfs.comp.zip.ULong;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * A simple round trip test of the static utility methods for unsigned long
 * integers.
 * 
 * @author Christian Schlichtherle
 */
public final class ULongTest {

    @Test
    public void testCheck() {
        try {
            ULong.check(ULong.MIN_VALUE - 1);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        ULong.check(ULong.MIN_VALUE);
        ULong.check(ULong.MAX_VALUE);
    }
}