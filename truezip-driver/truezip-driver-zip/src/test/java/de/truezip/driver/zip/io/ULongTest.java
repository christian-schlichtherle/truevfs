/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.driver.zip.io.ULong;
import org.junit.Test;

import static org.junit.Assert.*;

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