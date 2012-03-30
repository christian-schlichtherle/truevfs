/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.option;

import static de.truezip.kernel.fs.option.FsAccessOption.*;
import static de.truezip.kernel.fs.option.FsAccessOptions.*;
import de.truezip.kernel.util.BitField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsAccessOptionsTest {

    @Test
    public void testOf() {
        for (final Object[] params : new Object[][] {
            // { $array, $bits }
            { new FsAccessOption[0], NONE },
            { new FsAccessOption[] { CACHE }, INPUT_PREFERENCES_MASK },
            { new FsAccessOption[] { CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT }, OUTPUT_PREFERENCES_MASK },
        }) {
            final FsAccessOption[] array = (FsAccessOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(bits, FsAccessOptions.of(array));
        }
    }
}
