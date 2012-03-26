/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key.impl;

import de.truezip.kernel.key.KeyManagerTestSuite;
import de.truezip.kernel.key.impl.PromptingKeyManager;
import de.truezip.kernel.key.impl.PromptingKeyProvider;
import de.truezip.kernel.key.impl.pbe.console.ConsoleAesPbeParametersView;
import de.truezip.kernel.key.param.AesPbeParameters;
import java.net.URI;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
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
    public void testRemovePromptingKeyProvider() {
        URI id = URI.create("a");

        PromptingKeyProvider<?> prov = manager.make(id);
        assertSame(id, prov.getResource());

        manager.delete(id);
        assertNull(prov.getResource());
    }
}