/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.option;

import static de.truezip.kernel.option.SyncOption.*;
import static de.truezip.kernel.option.SyncOptions.*;
import de.truezip.kernel.util.BitField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class SyncOptionsTest {

    @Test
    public void testOf() {
        for (final Object[] params : new Object[][] {
            // { $array, $bits }
            { new SyncOption[0], NONE },
            { new SyncOption[] { ABORT_CHANGES }, RESET },
            { new SyncOption[] { WAIT_CLOSE_IO }, SYNC },
            { new SyncOption[] { FORCE_CLOSE_IO, CLEAR_CACHE }, UMOUNT },
        }) {
            final SyncOption[] array = (SyncOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(bits, SyncOptions.of(array));
        }
    }
}
