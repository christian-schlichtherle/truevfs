/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.driver.zip.io.UByte;
import org.junit.Test;

import static org.junit.Assert.*;

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