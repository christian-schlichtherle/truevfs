/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import net.truevfs.kernel.spec.spi.IoPoolService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TempFilePoolServiceTest {

    private IoPoolService service;
    
    @Before
    public void setUp() {
        service = new TempFilePoolService();
    }

    @Test
    public void testIoPool() {
        assertSame(service.getIoPool(), TempFilePool.INSTANCE);
    }

    @Test
    public void testPriority() {
        assertEquals(-100, service.getPriority());
    }
}
