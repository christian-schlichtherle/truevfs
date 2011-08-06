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

import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.key.pbe.console.ConsoleAesPbeParametersView;
import java.net.URI;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
