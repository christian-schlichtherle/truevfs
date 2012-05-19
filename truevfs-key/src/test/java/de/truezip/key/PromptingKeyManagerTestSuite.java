/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import java.net.URI;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class PromptingKeyManagerTestSuite
extends KeyManagerTestSuite<PromptingKeyManager<?>> {

    @Test
    public void testMakePromptingKeyProvider() {
        URI id = URI.create("a");

        PromptingKeyProvider<?> prov = manager.make(id);
        assertSame(id, prov.getResource());
    }

    @Test
    public void testMovePromptingKeyProvider() {
        URI idA = URI.create("a");
        URI idB = URI.create("b");

        PromptingKeyProvider<?> prov = manager.make(idA);
        assertSame(idA, prov.getResource());

        manager.move(idA, idB);
        assertSame(idB, prov.getResource());
    }

    @Test
    public void testDeletePromptingKeyProvider() {
        URI id = URI.create("a");

        PromptingKeyProvider<?> prov = manager.make(id);
        assertSame(id, prov.getResource());

        manager.delete(id);
        assertNull(prov.getResource());
    }
}