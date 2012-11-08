/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.unknown;

import net.java.truevfs.key.spec.AbstractKeyProvider;
import net.java.truevfs.key.spec.PersistentUnknownKeyException;
import net.java.truevfs.key.spec.UnknownKeyException;

/**
 * This key provider fails to resolve any keys.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
final class UnknownKeyProvider extends AbstractKeyProvider<Object> {

    static final UnknownKeyProvider SINGLETON = new UnknownKeyProvider();

    private UnknownKeyProvider() { }

    @Override
    public Object getKeyForWriting() throws UnknownKeyException {
        throw new PersistentUnknownKeyException();
    }

    @Override
    public Object getKeyForReading(final boolean invalid) throws UnknownKeyException {
        if (invalid) setKey(null);
        return getKeyForWriting();
    }

    @Override
    public void setKey(Object key) {
        throw new IllegalStateException("getReadKey() never provided a key!");
    }
}
