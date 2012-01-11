/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
