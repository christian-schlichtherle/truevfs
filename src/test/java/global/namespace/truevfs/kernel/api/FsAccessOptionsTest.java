/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.commons.shed.BitField;
import org.junit.Test;

import static global.namespace.truevfs.kernel.api.FsAccessOption.*;
import static global.namespace.truevfs.kernel.api.FsAccessOptions.ACCESS_PREFERENCES_MASK;
import static global.namespace.truevfs.kernel.api.FsAccessOptions.NONE;
import static org.junit.Assert.assertEquals;

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
