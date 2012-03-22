/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.fs.FsOutputOptions.NONE;
import static de.schlichtherle.truezip.fs.FsOutputOptions.OUTPUT_PREFERENCES_MASK;
import de.schlichtherle.truezip.util.BitField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsOutputOptionsTest {

    @Test
    public void testOf() {
        for (final Object[] params : new Object[][] {
            // { $array, $bits }
            { new FsOutputOption[0], NONE },
            { new FsOutputOption[] { CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT }, OUTPUT_PREFERENCES_MASK },
        }) {
            final FsOutputOption[] array = (FsOutputOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(bits, FsOutputOptions.of(array));
        }
    }
}
