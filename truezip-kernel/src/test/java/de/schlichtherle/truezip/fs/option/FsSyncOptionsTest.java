/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.option;

import static de.schlichtherle.truezip.fs.option.FsSyncOption.*;
import static de.schlichtherle.truezip.fs.option.FsSyncOptions.*;
import de.schlichtherle.truezip.util.BitField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsSyncOptionsTest {

    @Test
    public void testOf() {
        for (final Object[] params : new Object[][] {
            // { $array, $bits }
            { new FsSyncOption[0], NONE },
            { new FsSyncOption[] { ABORT_CHANGES }, RESET },
            { new FsSyncOption[] { WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT }, SYNC },
            { new FsSyncOption[] { FORCE_CLOSE_INPUT, FORCE_CLOSE_OUTPUT, CLEAR_CACHE }, UMOUNT },
        }) {
            final FsSyncOption[] array = (FsSyncOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(bits, FsSyncOptions.of(array));
        }
    }
}
