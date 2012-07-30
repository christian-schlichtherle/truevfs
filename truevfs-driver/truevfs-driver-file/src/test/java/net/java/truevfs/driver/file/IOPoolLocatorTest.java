/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import net.java.truevfs.driver.file.TempFilePool;
import net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class IOPoolLocatorTest {
    @Test
    public void testIoPool() {
        assertTrue(IoBufferPoolLocator.SINGLETON.get() instanceof TempFilePool);
    }
}
