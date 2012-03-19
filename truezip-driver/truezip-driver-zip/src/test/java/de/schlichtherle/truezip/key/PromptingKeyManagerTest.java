/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.key.pbe.console.ConsoleAesPbeParametersView;
import java.net.URI;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public class PromptingKeyManagerTest
extends KeyManagerTestSuite<PromptingKeyManager<?>> {

    @Override
    protected PromptingKeyManager<?> newKeyManager() {
        return new PromptingKeyManager<AesPbeParameters>(
                new ConsoleAesPbeParametersView());
    }

    @Test
    public void testGetPromptingKeyProvider() {
        URI id = URI.create("a");

        PromptingKeyProvider<?> prov = manager.getKeyProvider(id);
        assertSame(id, prov.getResource());
    }

    @Test
    public void testMovePromptingKeyProvider() {
        URI idA = URI.create("a");
        URI idB = URI.create("b");

        PromptingKeyProvider<?> prov = manager.getKeyProvider(idA);
        assertSame(idA, prov.getResource());

        manager.moveKeyProvider(idA, idB);
        assertSame(idB, prov.getResource());
    }

    @Test
    public void testRemovePromptingKeyProvider() {
        URI id = URI.create("a");

        PromptingKeyProvider<?> prov = manager.getKeyProvider(id);
        assertSame(id, prov.getResource());

        manager.removeKeyProvider(id);
        assertNull(prov.getResource());
    }
}