/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

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

    @Test(expected = NullPointerException.class)
    public void testNoKeyProviderForNullResource() {
        manager.provider(null);
    }

    @Test
    public void testNonNullKeyProviderForAnyNonNullResource() {
        assertNotNull(manager.provider(URI.create("a")));
    }

    @Test
    public void testLinkKeyProvider() {
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

        KeyProvider<?> provA1 = manager.provider(idA);
        assertNotNull(provA1);

        manager.link(idA, idB);
        manager.unlink(idA);

        KeyProvider<?> provA2 = manager.provider(idA);
        assertNotNull(provA2);

        assertNotSame(provA1, provA2);
    }

    @Test
    public void testUnlinkKeyProvider() {
        URI id = URI.create("a");

        manager.unlink(id);

        KeyProvider<?> prov1 = manager.provider(id);
        manager.unlink(id);
        manager.unlink(id);

        KeyProvider<?> prov2 = manager.provider(id);
        manager.unlink(id);
        manager.unlink(id);

        assertNotSame(prov1, prov2);
    }
}
