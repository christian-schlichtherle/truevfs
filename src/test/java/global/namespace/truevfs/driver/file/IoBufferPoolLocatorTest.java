/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.truevfs.kernel.api.sl.IoBufferPoolLocator;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Christian Schlichtherle
 */
public class IoBufferPoolLocatorTest {

    @Test
    public void testIoPool() {
        assertTrue(IoBufferPoolLocator.SINGLETON.get() instanceof FileBufferPool);
    }
}
