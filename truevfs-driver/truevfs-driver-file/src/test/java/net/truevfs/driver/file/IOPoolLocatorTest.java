/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import net.truevfs.kernel.sl.IoPoolLocator;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class IOPoolLocatorTest {
    @Test
    public void testIoPool() {
        assertTrue(IoPoolLocator.SINGLETON.getIoPool() instanceof TempFilePool);
    }
}
