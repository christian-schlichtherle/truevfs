/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A simple round trip test of the static utility methods for unsigned
 * integers.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
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
