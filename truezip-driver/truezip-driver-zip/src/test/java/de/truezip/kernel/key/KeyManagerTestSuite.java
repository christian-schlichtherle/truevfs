/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key;

import de.truezip.kernel.key.KeyManager;
import de.truezip.kernel.key.KeyProvider;
import java.net.URI;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @param   <M> The type of the key manager.
 * @author  Christian Schlichtherle
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