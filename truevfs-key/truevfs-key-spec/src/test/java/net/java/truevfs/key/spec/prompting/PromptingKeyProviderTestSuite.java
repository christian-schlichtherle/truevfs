/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import net.java.truevfs.key.spec.UnknownKeyException;
import static net.java.truevfs.key.spec.prompting.TestView.Action.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class PromptingKeyProviderTestSuite<
        P extends PromptingPbeParameters<P, ?>> {

    private static final URI RESOURCE = URI.create("foo");
    private static final AtomicInteger count = new AtomicInteger();

    private TestView<P> view;
    private PromptingKeyManager<P> manager;
    private PromptingKeyProvider<P> provider;

    private P createParam() {
        final P param = newParam();
        param.setSecret((ByteBuffer) ByteBuffer
                .allocate(4)
                .putInt(count.getAndIncrement())
                .rewind());
        return param;
    }

    protected abstract P newParam();

    @Before
    public void setUp() {
        view = new TestView<>();
        view.setResource(RESOURCE);
        manager = new PromptingKeyManager<>(view);
        provider = manager.provider(RESOURCE);
    }

    @Test
    public void testLifeCycle() throws UnknownKeyException {
        P param = createParam();
        view.setKey(param);
        assertSame(param, view.getKey());

        assertEquals(param, provider.getKeyForWriting());
        assertEquals(param, provider.getKeyForReading(false));

        view.setAction(CANCEL);

        assertEquals(param, provider.getKeyForWriting());
        assertEquals(param, provider.getKeyForReading(false));

        provider.resetCancelledKey();

        assertEquals(param, provider.getKeyForReading(false));
        assertEquals(param, provider.getKeyForWriting());

        provider.resetUnconditionally();

        view.setKey(createParam());
        try {
            provider.getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(createParam());
        try {
            provider.getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        view.setAction(IGNORE);

        view.setKey(createParam());
        try {
            provider.getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(createParam());
        try {
            provider.getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider.resetCancelledKey();
        view.setAction(ENTER);

        param = createParam();
        param.setChangeRequested(true);
        view.setKey(param);
        assertEquals(param, provider.getKeyForReading(false));
        view.setKey(param = createParam());
        assertEquals(param, provider.getKeyForWriting());

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

        provider.setKey(param = createParam());
        assertEquals(param, provider.getKeyForReading(false));
        view.setKey(createParam());
        assertEquals(param, provider.getKeyForWriting());
    }
}
