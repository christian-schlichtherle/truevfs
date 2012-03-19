/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class PromptingKeyManagerServiceTest {

    private KeyManagerService instance;
    
    @Before
    public void setUp() {
        instance = new PromptingKeyManagerService();
    }

    @Test
    public void testGet() {
        assertNotNull(instance.get(AesCipherParameters.class));
    }
}