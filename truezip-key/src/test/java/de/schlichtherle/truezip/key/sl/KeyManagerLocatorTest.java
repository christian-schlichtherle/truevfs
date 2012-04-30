/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.sl;

import de.truezip.key.param.AesPbeParameters;
import de.truezip.key.sl.KeyManagerLocator;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagerLocatorTest {
    @Test
    public void testGetManager() {
        assertNotNull(KeyManagerLocator.SINGLETON.keyManager(AesPbeParameters.class));
    }
}
