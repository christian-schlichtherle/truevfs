/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.prompting;

import net.java.truecommons3.key.spec.KeyManager;
import net.java.truecommons3.key.spec.KeyProvider;
import net.java.truecommons3.key.spec.UnknownKeyException;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static net.java.truecommons3.key.spec.prompting.TestView.Action.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public abstract class PromptingKeyProviderTestSuite<
        P extends AbstractPromptingPbeParameters<P, ?>> {

    private static final URI RESOURCE = URI.create("foo");
    private static final AtomicInteger count = new AtomicInteger();

    private TestView<P> view;
    private KeyManager<P> manager;

    private P createParam() {
        final P param = newParam();
        param.setSecret((ByteBuffer) ByteBuffer
                .allocate(4)
                .putInt(count.getAndIncrement())
                .rewind());
        return param;
    }

    protected abstract P newParam();

    private KeyProvider<P> provider() { return manager.provider(RESOURCE); }

    @Before
    public void setUp() {
        view = new TestView<>();
        view.setResource(RESOURCE);
        manager = new PromptingKeyManager<>(view);
    }

    @Test
    public void testLifeCycle() throws UnknownKeyException {
        P param = createParam();
        view.setKey(param);
        assertSame(param, view.getKey());

        assertEquals(param, provider().getKeyForWriting());
        assertEquals(param, provider().getKeyForReading(false));

        view.setAction(CANCEL);

        assertEquals(param, provider().getKeyForWriting());
        assertEquals(param, provider().getKeyForReading(false));

        manager.release(RESOURCE);

        assertEquals(param, provider().getKeyForReading(false));
        assertEquals(param, provider().getKeyForWriting());

        manager.unlink(RESOURCE);

        view.setKey(createParam());
        try {
            provider().getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(createParam());
        try {
            provider().getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        view.setAction(IGNORE);

        view.setKey(createParam());
        try {
            provider().getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        view.setKey(createParam());
        try {
            provider().getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        manager.release(RESOURCE);
        view.setAction(ENTER);

        param = createParam();
        param.setChangeRequested(true);
        view.setKey(param);
        assertEquals(param, provider().getKeyForReading(false));
        view.setKey(param = createParam());
        assertEquals(param, provider().getKeyForWriting());

        provider().setKey(null);
        try {
            provider().getKeyForReading(false);
            fail();
        } catch (UnknownKeyException expected) {
        }
        try {
            provider().getKeyForWriting();
            fail();
        } catch (UnknownKeyException expected) {
        }

        provider().setKey(param = createParam());
        assertEquals(param, provider().getKeyForReading(false));
        view.setKey(createParam());
        assertEquals(param, provider().getKeyForWriting());
    }
}
