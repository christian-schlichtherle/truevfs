/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import net.truevfs.kernel.spec.spi.IoPoolFactory;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TempFilePoolServiceTest {

    private IoPoolFactory service;
    
    @Before
    public void setUp() {
        service = new TempFilePoolFactory();
    }

    @Test
    public void testPriority() {
        assertEquals(0, service.getPriority());
    }
}
