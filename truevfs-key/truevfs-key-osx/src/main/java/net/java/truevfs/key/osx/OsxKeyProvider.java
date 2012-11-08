/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.osx;

import java.net.URI;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.KeyProvider;
import net.java.truevfs.key.spec.UnknownKeyException;
import net.java.truevfs.key.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class OsxKeyProvider implements KeyProvider<AesPbeParameters> {

    private final OsxKeyManager manager;
    private final URI resource;
    private final KeyProvider<AesPbeParameters> provider;
    private volatile AesPbeParameters key;

    OsxKeyProvider(
            final OsxKeyManager manager,
            final URI resource,
            final KeyProvider<AesPbeParameters> provider) {
        this.manager = Objects.requireNonNull(manager);
        this.resource = Objects.requireNonNull(resource);
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public AesPbeParameters getKeyForWriting() throws UnknownKeyException {
        if (null == key) key = manager.getKey(resource);
        if (null == key) key = provider.getKeyForWriting();
        assert null != key;
        return key;
    }

    @Override
    public AesPbeParameters getKeyForReading(final boolean invalid)
    throws UnknownKeyException {
        if (null == key && !invalid) key = manager.getKey(resource);
        if (null == key || invalid) key = provider.getKeyForReading(invalid);
        assert null != key;
        return key;
    }

    @Override
    public void setKey(final AesPbeParameters nk) {
        final AesPbeParameters ok = key;
        provider.setKey(key = nk);
        if (!nk.equals(ok)) manager.setKey(resource, nk);
    }
}
