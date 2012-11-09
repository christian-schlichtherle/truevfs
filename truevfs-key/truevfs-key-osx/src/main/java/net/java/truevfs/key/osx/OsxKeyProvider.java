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
    private volatile AesPbeParameters param;

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
        AesPbeParameters op = param;
        if (null == op) op = manager.getKey(resource);
        if (null != op && !op.isChangeRequested()) return op.clone();
        final AesPbeParameters np = provider.getKeyForWriting();
        if (!np.equals(op)) manager.setKey(resource, np);
        return param = np;
    }

    @Override
    public AesPbeParameters getKeyForReading(final boolean invalid)
    throws UnknownKeyException {
        if (!invalid) {
            AesPbeParameters op = param;
            if (null == op) op = manager.getKey(resource);
            if (null != op) return op.clone();
        }
        return provider.getKeyForReading(invalid);
    }

    @Override
    public void setKey(final AesPbeParameters np) {
        final AesPbeParameters op = param;
        provider.setKey(np);
        if (!np.equals(op)) manager.setKey(resource, np);
        param = np;
    }
}
