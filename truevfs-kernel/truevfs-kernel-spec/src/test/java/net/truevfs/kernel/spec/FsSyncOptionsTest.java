/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import static net.truevfs.kernel.spec.FsSyncOption.*;
import static net.truevfs.kernel.spec.FsSyncOptions.*;
import net.java.truecommons.shed.BitField;
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
            { new FsSyncOption[] { WAIT_CLOSE_IO }, SYNC },
            { new FsSyncOption[] { FORCE_CLOSE_IO, CLEAR_CACHE }, UMOUNT },
        }) {
            final FsSyncOption[] array = (FsSyncOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(bits, FsSyncOptions.of(array));
        }
    }
}
