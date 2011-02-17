/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.key;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class SafeKeyProviderTest {

    @Test
    public void testClone() {
        DummyKey key = new DummyKey();
        assertClone(key, SafeKeyProvider.clone(key));
        assertClone(SafeKeyProvider.clone(key), SafeKeyProvider.clone(key));
    }

    private static void assertClone(final DummyKey key, final DummyKey clone) {
        assertNotSame(key, clone);
        assertEquals(key, clone);
    }

    @Test
    public void testReset() {
        DummyKey key = new DummyKey();
        SafeKeyProvider.reset(key);
        assertTrue(key.reset);
    }
}
