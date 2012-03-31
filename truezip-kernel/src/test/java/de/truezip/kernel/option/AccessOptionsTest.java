/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.option;

import static de.truezip.kernel.option.AccessOption.*;
import static de.truezip.kernel.option.AccessOptions.ACCESS_PREFERENCES_MASK;
import static de.truezip.kernel.option.AccessOptions.NONE;
import de.truezip.kernel.util.BitField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class AccessOptionsTest {

    @Test
    public void testOf() {
        for (final Object[] params : new Object[][] {
            // { $array, $bits }
            { new AccessOption[0], NONE },
            { new AccessOption[] { CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT }, ACCESS_PREFERENCES_MASK },
        }) {
            final AccessOption[] array = (AccessOption[]) params[0];
            final BitField<?> bits = (BitField<?>) params[1];
            assertEquals(AccessOptions.of(array), bits);
        }
    }
}
