/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import java.net.URI;
import net.java.truevfs.key.spec.UnknownKeyException;
import static net.java.truevfs.key.spec.prompting.TestView.Action.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class PromptingKeyProviderTest {

    private static final URI RESOURCE = URI.create("foo");

    private TestView<TestKey> view;
    private PromptingKeyManager<TestKey> manager;
    private PromptingKeyProvider<TestKey> provider;

    @Before
    public void setUp() {
        view = new TestView<>();
        view.setResource(RESOURCE);
        manager = new PromptingKeyManager<>(view);
        provider = manager.provider(RESOURCE);
    }

    @Test
    public void testLifeCycle() throws UnknownKeyException {
        TestKey key = new TestKey();
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

        view.setKey(key = new TestKey());
        try {
            provider.getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(key = new TestKey());
        try {
            provider.getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        view.setAction(IGNORE);

        view.setKey(key = new TestKey());
        try {
            provider.getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(key = new TestKey());
        try {
            provider.getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider.resetCancelledKey();
        view.setAction(ENTER);

        key = new TestKey();
        key.setChangeRequested(true);
        view.setKey(key);
        assertEquals(key, provider.getKeyForReading(false));
        view.setKey(key = new TestKey());
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

        provider.setKey(key = new TestKey());
        assertEquals(key, provider.getKeyForReading(false));
        view.setKey(new TestKey());
        assertEquals(key, provider.getKeyForWriting());
    }
}