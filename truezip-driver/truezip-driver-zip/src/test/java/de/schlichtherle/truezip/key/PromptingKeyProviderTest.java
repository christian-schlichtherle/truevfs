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

import java.net.URI;
import org.junit.Before;
import org.junit.Test;

import static de.schlichtherle.truezip.key.MockView.Action.*;
import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class PromptingKeyProviderTest {

    private static final URI RESOURCE = URI.create("foo");

    private MockView<DummyKey> view;
    private PromptingKeyManager<DummyKey> manager;
    private PromptingKeyProvider<DummyKey> provider;

    @Before
    public void setUp() {
        view = new MockView<DummyKey>();
        view.setResource(RESOURCE);
        view.setChangeRequested(true);
        manager = new PromptingKeyManager<DummyKey>(view);
        provider = manager.getKeyProvider(RESOURCE);
    }

    @Test
    public void testLifeCycle() throws UnknownKeyException {
        DummyKey key = new DummyKey();
        view.setKey(key);
        assertSame(key, view.getKey());

        assertEquals(key, provider.getWriteKey());
        assertEquals(key, provider.getReadKey(false));

        view.setAction(CANCEL);

        assertEquals(key, provider.getWriteKey());
        assertEquals(key, provider.getReadKey(false));

        provider.resetCancelledKey();

        assertEquals(key, provider.getReadKey(false));
        assertEquals(key, provider.getWriteKey());

        provider.resetUnconditionally();

        view.setKey(key = new DummyKey());
        try {
            provider.getReadKey(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(key = new DummyKey());
        try {
            provider.getWriteKey();
            fail();
        } catch (UnknownKeyException expected) {
        }

        view.setAction(IGNORE);

        view.setKey(key = new DummyKey());
        try {
            provider.getReadKey(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(key = new DummyKey());
        try {
            provider.getWriteKey();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider.resetCancelledKey();
        view.setAction(ENTER);

        view.setKey(key = new DummyKey());
        assertEquals(key, provider.getReadKey(false));
        view.setKey(key = new DummyKey());
        assertEquals(key, provider.getWriteKey());

        provider.setKey(null);
        try {
            provider.getReadKey(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        try {
            provider.getWriteKey();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider.setKey(key = new DummyKey());
        assertEquals(key, provider.getReadKey(false));
        view.setKey(new DummyKey());
        assertEquals(key, provider.getWriteKey());
    }
}
