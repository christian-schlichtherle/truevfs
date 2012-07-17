/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.sl;

import net.truevfs.key.param.AesPbeParameters;
import net.truevfs.key.sl.KeyManagerMapLocator;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagerMapLocatorTest {
    @Test
    public void testGetManager() {
        assertNotNull(KeyManagerMapLocator.SINGLETON.keyManager(AesPbeParameters.class));
    }
}
