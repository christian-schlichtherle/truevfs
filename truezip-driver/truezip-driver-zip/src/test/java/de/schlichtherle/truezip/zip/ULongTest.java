/*
 * Copyright 2004-2012 Schlichtherle IT Services
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
 * A simple round trip test of the static utility methods for unsigned long
 * integers.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
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
