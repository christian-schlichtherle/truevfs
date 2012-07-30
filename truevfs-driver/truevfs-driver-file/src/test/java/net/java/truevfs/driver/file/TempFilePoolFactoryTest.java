/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import net.java.truevfs.driver.file.TempFilePoolFactory;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TempFilePoolFactoryTest {
    @Test
    public void testPriority() {
        assertEquals(0, new TempFilePoolFactory().getPriority());
    }
}
