/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsInputOption.CACHE;
import static de.schlichtherle.truezip.fs.FsInputOptions.INPUT_PREFERENCES_MASK;
import static de.schlichtherle.truezip.fs.FsInputOptions.NONE;
import de.schlichtherle.truezip.util.BitField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsInputOptionsTest {

    @Test
    public void testOf() {
        for (final Object[] params : new Object[][] {
            // { $array, $bits }
            { new FsInputOption[0], NONE },
            { new FsInputOption[] { CACHE }, INPUT_PREFERENCES_MASK },
        }) {
            final FsInputOption[] array = (FsInputOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(bits, FsInputOptions.of(array));
        }
    }
}
