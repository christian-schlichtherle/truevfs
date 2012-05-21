/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import net.truevfs.kernel.spi.IoPoolService;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TempFilePoolServiceTest {

    private IoPoolService instance;
    
    @Before
    public void setUp() {
        instance = new TempFilePoolService();
    }

    @Test
    public void testGet() {
        assertSame(instance.getIoPool(), TempFilePool.INSTANCE);
    }
}