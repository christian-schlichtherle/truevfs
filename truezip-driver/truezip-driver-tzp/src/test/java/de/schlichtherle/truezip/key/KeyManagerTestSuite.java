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

import java.net.URI;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class KeyManagerTestSuite {
    private KeyManager<?> instance;

    @Before
    public void setUp() {
        instance = newKeyManager();
    }

    protected abstract KeyManager<?> newKeyManager();

    @Test
    public final void testGetKeyProvider() {
        URI id = URI.create("a");

        try {
            instance.getKeyProvider(null);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException expected) {
        }

        KeyProvider<?> prov1 = instance.getKeyProvider(id);
        assertNotNull(prov1);

        KeyProvider<?> prov2 = instance.getKeyProvider(id);
        assertSame(prov1, prov2);
    }

    @Test
    public final void testMoveKeyProvider() {
        URI idA = URI.create("a");
        URI idB = URI.create("b");

        try {
            instance.moveKeyProvider(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            instance.moveKeyProvider(idA, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            instance.moveKeyProvider(null, idB);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            instance.moveKeyProvider(idA, idB);
            fail();
        } catch (IllegalArgumentException expected) {
            // no provider mapped yet
        }

        KeyProvider<?> provA1 = instance.getKeyProvider(idA);
        assertNotNull(provA1);

        assertNull(instance.moveKeyProvider(idA, idB));

        KeyProvider<?> provA2 = instance.getKeyProvider(idA);
        assertNotNull(provA2);
        assertFalse(provA1.equals(provA2));

        KeyProvider<?> provB1 = instance.getKeyProvider(idB);
        assertNotNull(provB1);
        assertSame(provA1, provB1);
    }

    @Test
    public final void testRemoveKeyProvider() {
        URI id = URI.create("a");

        try {
            instance.removeKeyProvider(id);
            fail();
        } catch (IllegalArgumentException expected) {
            // no provider mapped yet
        }

        KeyProvider<?> prov1 = instance.getKeyProvider(id);
        assertNotNull(instance.removeKeyProvider(id));

        try {
            instance.removeKeyProvider(id);
            fail();
        } catch (IllegalArgumentException expected) {
            // no provider mapped any more
        }

        KeyProvider<?> prov2 = instance.getKeyProvider(id);
        assertNotNull(instance.removeKeyProvider(id));

        try {
            instance.removeKeyProvider(id);
            fail();
        } catch (IllegalArgumentException expected) {
            // no provider mapped any more
        }

        assertFalse(prov1.equals(prov2));
    }
}
