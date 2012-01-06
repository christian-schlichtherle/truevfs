/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.sl;

import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
