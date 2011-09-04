/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
