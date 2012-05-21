/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file.sl;

import net.truevfs.kernel.sl.IoPoolLocator;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class IOPoolLocatorTest {
    @Test
    public void testIsLocatable() {
        assertNotNull(IoPoolLocator.SINGLETON.getIoPool());
    }
}
