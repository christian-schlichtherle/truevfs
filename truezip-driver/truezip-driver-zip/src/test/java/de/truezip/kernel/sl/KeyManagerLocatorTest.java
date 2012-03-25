/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sl;

import de.truezip.kernel.key.KeyManagerProvider;
import de.truezip.kernel.key.pbe.AesPbeParameters;
import de.truezip.kernel.sl.KeyManagerLocator;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class KeyManagerLocatorTest {

    private KeyManagerProvider instance;
    
    @Before
    public void setUp() {
        instance = KeyManagerLocator.SINGLETON;
    }

    @Test
    public void testGetManager() {
        //assertNotNull(instance.get(Object.class));
        assertNotNull(instance.get(AesPbeParameters.class));
    }
}