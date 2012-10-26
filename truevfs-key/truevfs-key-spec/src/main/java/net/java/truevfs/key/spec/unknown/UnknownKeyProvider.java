/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.unknown;

import net.java.truevfs.key.spec.KeyProvider;
import net.java.truevfs.key.spec.UnknownKeyException;

/**
 * This key provider fails to resolve any secret keys.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
final class UnknownKeyProvider implements KeyProvider<Object> {

    static final UnknownKeyProvider SINGLETON = new UnknownKeyProvider();

    private UnknownKeyProvider() { }

    @Override
    public Object getWriteKey() throws UnknownKeyException {
        throw new UnknownKeyException();
    }

    @Override
    public Object getReadKey(final boolean invalid) throws UnknownKeyException {
        if (invalid) setKey(null);
        return getWriteKey();
    }

    @Override
    public void setKey(Object key) {
        throw new IllegalStateException("getReadKey() never provided a key!");
    }
}
