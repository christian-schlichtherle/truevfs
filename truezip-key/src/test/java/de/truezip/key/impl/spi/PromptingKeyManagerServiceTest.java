/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.impl.spi;

import de.truezip.key.param.AesPbeParameters;
import de.truezip.key.spi.KeyManagerService;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class PromptingKeyManagerServiceTest {

    private KeyManagerService instance;
    
    @Before
    public void setUp() {
        instance = new PromptingKeyManagerService();
    }

    @Test
    public void testGet() {
        assertNotNull(instance.get(AesPbeParameters.class));
    }
}
