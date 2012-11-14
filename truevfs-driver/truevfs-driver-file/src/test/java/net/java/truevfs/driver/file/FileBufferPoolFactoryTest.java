/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FileBufferPoolFactoryTest {

    @Test
    public void testPriority() {
        assertTrue(new FileBufferPoolFactory().getPriority() < 0);
    }
}
