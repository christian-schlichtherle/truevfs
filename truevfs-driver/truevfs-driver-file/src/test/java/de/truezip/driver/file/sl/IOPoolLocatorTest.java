/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.sl;

import de.truezip.kernel.sl.IOPoolLocator;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class IOPoolLocatorTest {
    @Test
    public void testIsLocatable() {
        assertNotNull(IOPoolLocator.SINGLETON.getIOPool());
    }
}
