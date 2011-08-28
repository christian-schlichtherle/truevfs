/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
