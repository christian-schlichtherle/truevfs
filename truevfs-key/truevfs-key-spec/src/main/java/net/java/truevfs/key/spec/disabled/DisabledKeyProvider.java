/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.disabled;

import net.java.truevfs.key.spec.KeyProvider;
import net.java.truevfs.key.spec.UnknownKeyException;

/**
 * This key provider fails to resolve any secret keys.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
final class DisabledKeyProvider implements KeyProvider<Object> {

    static final DisabledKeyProvider SINGLETON = new DisabledKeyProvider();

    private DisabledKeyProvider() { }

    @Override
    public Object getWriteKey() throws UnknownKeyException {
        throw new UnknownKeyException();
    }

    @Override
    public Object getReadKey(boolean invalid) throws UnknownKeyException {
        throw new UnknownKeyException();
    }

    @Override
    public void setKey(Object key) {
        throw new IllegalStateException("getReadKey() never returned!");
    }
}
