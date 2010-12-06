/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class KeyManagerTest {
    private KeyManager instance;

    @Before
    public void setUp() {
        instance = KeyManager.getInstance();
    }

    @After
    public void tearDown() {
        KeyManager.resetAndRemoveKeyProviders();
        KeyManager.setInstance(null);
    }

    @Test
    public void testInstance() {
        final KeyManager inst1 = KeyManager.getInstance();
        assertNotNull(inst1);

        KeyManager.setInstance(null);
        final KeyManager inst2 = KeyManager.getInstance();
        assertNotNull(inst2);
        assertNotSame(inst1, inst2);

        final KeyManager inst3 = new PromptingKeyManager();

        KeyManager.setInstance(inst3);
        final KeyManager inst4 = KeyManager.getInstance();
        assertNotNull(inst4);

        assertSame(inst3, inst4);
    }

    @Test
    public void testKeyProvider() {
        final URI idA = URI.create("keyProviderA");

        final KeyProvider resA1 = instance.getKeyProvider(idA, (Class) KeyProvider.class);
        assertNotNull(resA1);

        final KeyProvider resA2 = instance.getKeyProvider(idA, (Class) KeyProvider.class);
        assertSame(resA1, resA2);

        final KeyProvider resA3 = instance.getKeyProvider(
                idA, (Class) SucceedingKeyProvider.class);
        assertSame(resA1, resA3);

        final KeyProvider resA4 = new SucceedingKeyProvider();
        try {
            instance.setKeyProvider(null, resA4);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException exc) {
        }
        try {
            instance.setKeyProvider(idA, null);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException exc) {
        }
        try {
            instance.setKeyProvider(null, null);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException exc) {
        }
        instance.setKeyProvider(idA, resA4);

        final KeyProvider resA5 = instance.getKeyProvider(idA, (Class) KeyProvider.class);
        assertSame(resA4, resA5);

        final URI idB = URI.create("keyProviderB");

        final KeyProvider resB1 = instance.getKeyProvider(
                idB, (Class) SucceedingKeyProvider.class);
        assertNotNull(resB1);
        assertNotSame(resA5, resB1);
        assertTrue(resB1 instanceof SucceedingKeyProvider);

        final URI idC = URI.create("keyProviderC");

        try {
            instance.getKeyProvider(idC, (Class) FailingKeyProvider.class);
            fail("An IllegalArgumentException is expected from the previous call!");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testResetKeyProvider() {
        final URI id = URI.create("resetKeyProvider");

        final SmartKeyProvider provider
                = (SmartKeyProvider) instance.getKeyProvider(
                    id, SmartKeyProvider.class);
        provider.reset = false;
        boolean result = KeyManager.resetKeyProvider(id);
        assertTrue(result);
        assertTrue(provider.reset);

        assertSame(provider, instance.getKeyProvider(id, (Class) KeyProvider.class));
    }

    @Test
    public void testResetAndRemoveKeyProvider() {
        final URI idA = URI.create("resetAndRemoveKeyProviderA");
        final URI idB = URI.create("resetAndRemoveKeyProviderB");

        final SmartKeyProvider provA1
                = (SmartKeyProvider) instance.getKeyProvider(
                    idA, SmartKeyProvider.class);
        provA1.reset = false;
        boolean okA = KeyManager.resetAndRemoveKeyProvider(idA);
        assertTrue(okA);
        assertTrue(provA1.reset);

        final KeyProvider provA2 = instance.getKeyProvider(idA, (Class) KeyProvider.class);
        assertNotNull(provA2);
        assertNotSame(provA1, provA2);

        final SimpleKeyProvider provB1
                = (SimpleKeyProvider) instance.getKeyProvider(
                    idB, SimpleKeyProvider.class);
        boolean okB = KeyManager.resetAndRemoveKeyProvider(idB);
        assertTrue(okB);

        final KeyProvider provB2 = instance.getKeyProvider(idB, (Class) KeyProvider.class);
        assertNotNull(provB2);
        assertNotSame(provB1, provB2);
    }

    @Test
    public void testResetKeyProviders() {
        final URI idA = URI.create("resetKeyProvidersA");
        final URI idB = URI.create("resetKeyProvidersB");

        final SmartKeyProvider provA
                = (SmartKeyProvider) instance.getKeyProvider(
                    idA, SmartKeyProvider.class);
        provA.reset = false;

        final SmartKeyProvider provB
                = (SmartKeyProvider) instance.getKeyProvider(
                    idB, SmartKeyProvider.class);
        provB.reset = false;

        KeyManager.resetKeyProviders();

        assertTrue(provA.reset);
        assertTrue(provB.reset);
    }

    @Test
    public void testResetAndRemoveKeyProviders() {
        final URI idA = URI.create("resetAndRemoveKeyProvidersA");
        final URI idB = URI.create("resetAndRemoveKeyProvidersB");

        final SmartKeyProvider provA1
                = (SmartKeyProvider) instance.getKeyProvider(
                    idA, SmartKeyProvider.class);
        provA1.reset = false;

        final SimpleKeyProvider provB1
                = (SimpleKeyProvider) instance.getKeyProvider(
                    idB, SimpleKeyProvider.class);

        KeyManager.resetAndRemoveKeyProviders();
        assertTrue(provA1.reset);

        final KeyProvider provA2 = instance.getKeyProvider(idA, (Class) KeyProvider.class);
        assertNotNull(provA2);
        assertNotSame(provA1, provA2);

        final KeyProvider provB2 = instance.getKeyProvider(idB, (Class) KeyProvider.class);
        assertNotNull(provB2);
        assertNotSame(provB1, provB2);
    }

    @Test
    public void testMoveKeyProvider() {
        final URI oldID = URI.create("moveKeyProviderA");
        final URI newID = URI.create("moveKeyProviderB");

        try {
            KeyManager.moveKeyProvider(null, null);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException expected) {
        }
        try {
            KeyManager.moveKeyProvider(oldID, null);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException expected) {
        }
        try {
            KeyManager.moveKeyProvider(null, newID);
            fail("A NullPointerException is expected from the previous call!");
        } catch (NullPointerException expected) {
        }
        boolean result = KeyManager.moveKeyProvider(oldID, newID);
        assertEquals(false, result); // no provider mapped yet

        final PromptingKeyProvider provA1
                = (PromptingKeyProvider) instance.getKeyProvider(oldID, (Class) KeyProvider.class);
        assertNotNull(provA1);
        assertSame(oldID, provA1.getResource());

        result = KeyManager.moveKeyProvider(oldID, newID);
        assertEquals(true, result);

        final KeyProvider provA2 = instance.getKeyProvider(oldID, (Class) KeyProvider.class);
        assertNotNull(provA2);

        final PromptingKeyProvider provB1
                = (PromptingKeyProvider) instance.getKeyProvider(newID, (Class) KeyProvider.class);
        assertNotNull(provB1);
        assertSame(provA1, provB1);
        assertSame(newID, provB1.getResource());
    }

    static class SucceedingKeyProvider extends PromptingKeyProvider<char[]> {
    }

    /** The key manager cannot instantiate this private class. */
    private static class FailingKeyProvider extends PromptingKeyProvider<char[]> {
    }

    static class SimpleKeyProvider implements KeyProvider<char[]> {
        @Override
		public char[] getCreateKey() {
            return "secret".toCharArray();
        }

        @Override
		public char[] getOpenKey() {
            return "secret".toCharArray();
        }

        @Override
		public void invalidOpenKey() {
        }
    }

    static class SmartKeyProvider extends AbstractKeyProvider<char[]> {
        public boolean reset;

        @Override
		public void invalidOpenKeyImpl() {
        }

        @Override
		public void reset() {
            reset = true;
        }
    }
}
