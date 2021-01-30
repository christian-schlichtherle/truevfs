/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import java.net.URI;
import java.util.Objects;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @param  <M> The type of the key manager.
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerTestSuite<M extends KeyManager<?>> {

    protected M manager;

    protected abstract M newKeyManager();

    @Before
    public void before() {
        manager = Objects.requireNonNull(newKeyManager());
    }

    @Test
    public void testProvider() {
        try {
            manager.provider(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertNotNull(manager.provider(URI.create("a")));
    }

    @Test
    public void testRelease() {
        try {
            manager.release(null);
            fail();
        } catch (NullPointerException expected) {
        }

        manager.release(URI.create("a")); // no effect
    }

    @Test
    public void testLink() {
        URI idA = URI.create("a");
        URI idB = URI.create("b");

        try {
            manager.link(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            manager.link(idA, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            manager.link(null, idB);
            fail();
        } catch (NullPointerException expected) {
        }

        manager.link(idA, idB);
        manager.unlink(idA);

        final KeyProvider<?> prov1 = manager.provider(idA);
        assertNotNull(prov1);

        manager.link(idA, idB);
        manager.unlink(idA);

        final KeyProvider<?> prov2 = manager.provider(idA);
        assertNotNull(prov2);

        assertNotSame(prov1, prov2); // may be trivially true - see contract
    }

    @Test
    public void testUnlink() {
        URI id = URI.create("a");

        try {
            manager.unlink(null);
            fail();
        } catch (NullPointerException expected) {
        }

        manager.unlink(id);

        final KeyProvider<?> prov1 = manager.provider(id);
        assertNotNull(prov1);
        manager.unlink(id);
        manager.unlink(id); // no effect

        final KeyProvider<?> prov2 = manager.provider(id);
        assertNotNull(prov2);
        manager.unlink(id);
        manager.unlink(id); // no effect

        assertNotSame(prov1, prov2);
    }
}
