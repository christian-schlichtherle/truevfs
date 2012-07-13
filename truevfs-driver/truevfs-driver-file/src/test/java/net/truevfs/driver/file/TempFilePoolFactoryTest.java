/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import net.truevfs.kernel.spec.spi.IoBufferPoolFactory;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TempFilePoolFactoryTest {

    private final IoBufferPoolFactory factory = new TempFilePoolFactory();

    @Test
    public void testPriority() {
        assertEquals(0, factory.getPriority());
    }
}
