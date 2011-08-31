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
 * A simple round trip test of the static utility methods for unsigned short
 * integers.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
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
