/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.net.URI;
import java.util.Objects;
import javax.annotation.CheckForNull;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @param  <M> The type of the key manager.
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerTestSuite<M extends KeyManager<?>> {

    final M manager = Objects.requireNonNull(newKeyManager());

    protected abstract @CheckForNull M newKeyManager();

    @Test
    public void testMakeKeyProvider() {
        URI id = URI.create("a");

        try {
            manager.provider(null);
            fail();
        } catch (NullPointerException expected) {
        }

        KeyProvider<?> prov1 = manager.provider(id);
        assertNotNull(prov1);

        KeyProvider<?> prov2 = manager.provider(id);
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

        manager.move(idA, idB);

        KeyProvider<?> provA1 = manager.provider(idA);
        assertNotNull(provA1);

        manager.move(idA, idB);

        KeyProvider<?> provA2 = manager.provider(idA);
        assertNotNull(provA2);
        assertFalse(provA1.equals(provA2));

        KeyProvider<?> provB1 = manager.provider(idB);
        assertNotNull(provB1);
        assertSame(provA1, provB1);
    }

    @Test
    public void testDeleteKeyProvider() {
        URI id = URI.create("a");

        manager.delete(id);

        KeyProvider<?> prov1 = manager.provider(id);
        manager.delete(id);
        manager.delete(id);

        KeyProvider<?> prov2 = manager.provider(id);
        manager.delete(id);
        manager.delete(id);

        assertFalse(prov1.equals(prov2));
    }
}
