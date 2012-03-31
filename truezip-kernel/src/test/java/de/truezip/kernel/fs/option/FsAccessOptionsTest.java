/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.option;

import static de.truezip.kernel.option.FsAccessOption.*;
import static de.truezip.kernel.option.FsAccessOptions.ACCESS_PREFERENCES_MASK;
import static de.truezip.kernel.option.FsAccessOptions.NONE;
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
            { new FsAccessOption[] { CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT }, ACCESS_PREFERENCES_MASK },
        }) {
            final FsAccessOption[] array = (FsAccessOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(FsAccessOptions.of(array), bits);
        }
    }
}
