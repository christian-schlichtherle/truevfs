/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import java.net.URI;
import java.util.Objects;
import javax.annotation.CheckForNull;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @param  <M> The type of the key manager.
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerTestSuite<M extends KeyManager<?>> {
    protected M manager;

    @Before
    public void setUp() {
        manager = Objects.requireNonNull(newKeyManager());
    }

    protected abstract @CheckForNull M newKeyManager();

    @Test
    public void testMakeKeyProvider() {
        URI id = URI.create("a");

        try {
            manager.make(null);
            fail();
        } catch (NullPointerException expected) {
        }

        KeyProvider<?> prov1 = manager.make(id);
        assertNotNull(prov1);

        KeyProvider<?> prov2 = manager.make(id);
        assertSame(prov1, prov2);
    }

    @Test
    public void testMoveKeyProvider() {
        URI idA = URI.create("a");
        URI idB = URI.create("b");

        try {
            manager.move(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            manager.move(idA, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            manager.move(null, idB);
            fail();
        } catch (NullPointerException expected) {
        }

        assertNull(manager.move(idA, idB));

        KeyProvider<?> provA1 = manager.make(idA);
        assertNotNull(provA1);

        assertNull(manager.move(idA, idB));

        KeyProvider<?> provA2 = manager.make(idA);
        assertNotNull(provA2);
        assertFalse(provA1.equals(provA2));

        KeyProvider<?> provB1 = manager.make(idB);
        assertNotNull(provB1);
        assertSame(provA1, provB1);
    }

    @Test
    public void testDeleteKeyProvider() {
        URI id = URI.create("a");

        assertNull(manager.delete(id));

        KeyProvider<?> prov1 = manager.make(id);
        assertNotNull(manager.delete(id));

        assertNull(manager.delete(id));

        KeyProvider<?> prov2 = manager.make(id);
        assertNotNull(manager.delete(id));

        assertNull(manager.delete(id));

        assertFalse(prov1.equals(prov2));
    }
}