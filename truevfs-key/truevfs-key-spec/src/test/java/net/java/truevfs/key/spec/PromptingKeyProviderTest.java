/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.net.URI;
import static net.java.truevfs.key.spec.MockView.Action.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class PromptingKeyProviderTest {

    private static final URI RESOURCE = URI.create("foo");

    private MockView<DummyKey> view;
    private PromptingKeyManager<DummyKey> manager;
    private PromptingKeyProvider<DummyKey> provider;

    @Before
    public void setUp() {
        view = new MockView<>();
        view.setResource(RESOURCE);
        view.setChangeRequested(true);
        manager = new PromptingKeyManager<>(view);
        provider = manager.provider(RESOURCE);
    }

    @Test
    public void testLifeCycle() throws UnknownKeyException {
        DummyKey key = new DummyKey();
        view.setKey(key);
        assertSame(key, view.getKey());

        assertEquals(key, provider.getKeyForWriting());
        assertEquals(key, provider.getKeyForReading(false));

        view.setAction(CANCEL);

        assertEquals(key, provider.getKeyForWriting());
        assertEquals(key, provider.getKeyForReading(false));

        provider.resetCancelledKey();

        assertEquals(key, provider.getKeyForReading(false));
        assertEquals(key, provider.getKeyForWriting());

        provider.resetUnconditionally();

        view.setKey(key = new DummyKey());
        try {
            provider.getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(key = new DummyKey());
        try {
            provider.getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        view.setAction(IGNORE);

        view.setKey(key = new DummyKey());
        try {
            provider.getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(key = new DummyKey());
        try {
            provider.getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider.resetCancelledKey();
        view.setAction(ENTER);

        view.setKey(key = new DummyKey());
        assertEquals(key, provider.getKeyForReading(false));
        view.setKey(key = new DummyKey());
        assertEquals(key, provider.getKeyForWriting());

        provider.setKey(null);
        try {
            provider.getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        try {
            provider.getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider.setKey(key = new DummyKey());
        assertEquals(key, provider.getKeyForReading(false));
        view.setKey(new DummyKey());
        assertEquals(key, provider.getKeyForWriting());
    }
}