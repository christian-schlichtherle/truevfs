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
 * @author Christian Schlichtherle
 * @version @version@
 */
public class PromptingKeyProviderTest {

    private static final URI RESOURCE = URI.create("foo");

    private MockView<DummyKey> view;
    private PromptingKeyProvider<DummyKey> provider;

    @Before
    public void setUp() {
        view = new MockView<DummyKey>();
        view.setResource(RESOURCE);
        view.setChangeRequested(true);
        provider = new PromptingKeyProvider<DummyKey>();
        provider.setView(view);
        provider.setResource(RESOURCE);
    }

    @Test
    public void testLifeCycle() throws UnknownKeyException {
        view.setKey(new DummyKey());

        assertEquals(view.getKey(), provider.getCreateKey());
        assertEquals(view.getKey(), provider.getOpenKey(false));

        view.setAction(CANCEL);

        assertEquals(view.getKey(), provider.getCreateKey());
        assertEquals(view.getKey(), provider.getOpenKey(false));

        provider.resetCancelledKey();

        assertEquals(view.getKey(), provider.getOpenKey(false));
        assertEquals(view.getKey(), provider.getCreateKey());

        provider.resetUnconditionally();

        view.setKey(new DummyKey());
        try {
            provider.getOpenKey(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(new DummyKey());
        try {
            provider.getCreateKey();
            fail();
        } catch (UnknownKeyException expected) {
        }

        view.setAction(IGNORE);

        view.setKey(new DummyKey());
        try {
            provider.getOpenKey(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(new DummyKey());
        try {
            provider.getCreateKey();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider.resetCancelledKey();
        view.setAction(ENTER);

        view.setKey(new DummyKey());
        assertEquals(view.getKey(), provider.getOpenKey(false));
        view.setKey(new DummyKey());
        assertEquals(view.getKey(), provider.getCreateKey());
    }
}
