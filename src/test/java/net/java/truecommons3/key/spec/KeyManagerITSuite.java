/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import org.junit.After;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * @param <K> the type of the keys.
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerITSuite<K> {

    protected abstract KeyManager<K> keyManager();

    protected final URI uri() {
        return URI.create("please-ignore-this-test-resource");
    }

    @After
    public void after() { keyManager().unlink(uri()); }

    @Test
    public void testLifecycle() throws UnknownKeyException {
        final K writeKey = keyManager().provider(uri()).getKeyForWriting();
        final K readKey = keyManager().provider(uri()).getKeyForReading(false);
        assertEquals(writeKey, readKey);
        keyManager().unlink(uri());
    }
}
