/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.unknown;

import net.java.truecommons.key.spec.AbstractKeyProvider;
import net.java.truecommons.key.spec.PersistentUnknownKeyException;
import net.java.truecommons.key.spec.UnknownKeyException;

/**
 * This key provider fails to resolve any keys.
 *
 * @since  TrueCommons 2.2
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
