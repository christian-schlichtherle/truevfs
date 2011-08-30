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

import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class KeyManagerTestSuite<M extends KeyManager<?>> {
    protected M manager;

    @Before
    public void setUp() {
        manager = newKeyManager();
    }

    protected abstract M newKeyManager();

    @Test
    public void testGetKeyProvider() {
        URI id = URI.create("a");

        try {
            manager.getKeyProvider(null);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException expected) {
        }

        KeyProvider<?> prov1 = manager.getKeyProvider(id);
        assertNotNull(prov1);

        KeyProvider<?> prov2 = manager.getKeyProvider(id);
        assertSame(prov1, prov2);
    }

    @Test
    public void testMoveKeyProvider() {
        URI idA = URI.create("a");
        URI idB = URI.create("b");

        try {
            manager.moveKeyProvider(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            manager.moveKeyProvider(idA, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            manager.moveKeyProvider(null, idB);
            fail();
        } catch (NullPointerException expected) {
        }

        assertNull(manager.moveKeyProvider(idA, idB));

        KeyProvider<?> provA1 = manager.getKeyProvider(idA);
        assertNotNull(provA1);

        assertNull(manager.moveKeyProvider(idA, idB));

        KeyProvider<?> provA2 = manager.getKeyProvider(idA);
        assertNotNull(provA2);
        assertFalse(provA1.equals(provA2));

        KeyProvider<?> provB1 = manager.getKeyProvider(idB);
        assertNotNull(provB1);
        assertSame(provA1, provB1);
    }

    @Test
    public void testRemoveKeyProvider() {
        URI id = URI.create("a");

        assertNull(manager.removeKeyProvider(id));

        KeyProvider<?> prov1 = manager.getKeyProvider(id);
        assertNotNull(manager.removeKeyProvider(id));

        assertNull(manager.removeKeyProvider(id));

        KeyProvider<?> prov2 = manager.getKeyProvider(id);
        assertNotNull(manager.removeKeyProvider(id));

        assertNull(manager.removeKeyProvider(id));

        assertFalse(prov1.equals(prov2));
    }
}
