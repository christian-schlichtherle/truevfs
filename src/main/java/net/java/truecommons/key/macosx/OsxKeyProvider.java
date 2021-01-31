/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx;

import net.java.truecommons.key.spec.KeyProvider;
import net.java.truecommons.key.spec.UnknownKeyException;
import net.java.truecommons.key.spec.prompting.AbstractPromptingPbeParameters;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Interacts with the {@link OsxKeyManager} to persist passwords into Apple's
 * Keychain Services API.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class OsxKeyProvider<P extends AbstractPromptingPbeParameters<P, ?>> implements KeyProvider<P> {

    private final OsxKeyManager<P> manager;
    private final URI resource;
    private final KeyProvider<P> provider;
    private volatile Optional<P> param = Optional.empty();

    OsxKeyProvider(
            final OsxKeyManager<P> manager,
            final URI resource,
            final KeyProvider<P> provider) {
        this.manager = manager;
        this.resource = resource;
        this.provider = provider;
    }

    @Override
    public P getKeyForWriting() throws UnknownKeyException {
        Optional<P> op = param;
        if (!op.isPresent())
            op = manager.getKey(resource);
        if (op.isPresent()) {
            final P p = op.get();
            if (!p.isChangeRequested()) {
                return p.clone();
            }
        }
        final Optional<P> np = Optional.of(provider.getKeyForWriting());
        if (!np.equals(op))
            manager.setKey(resource, np);
        return (param = np).get();
    }

    @Override
    public P getKeyForReading(final boolean invalid) throws UnknownKeyException {
        if (!invalid) {
            Optional<P> op = param;
            if (!op.isPresent()) {
                op = manager.getKey(resource);
            }
            if (op.isPresent()) {
                return op.get().clone();
            }
        }
        return provider.getKeyForReading(invalid);
    }

    @Override
    public void setKey(final @Nullable P key) {
        final Optional<P> op = param;
        final Optional<P> np = Optional.ofNullable(key);
        provider.setKey(key);
        if (!Objects.equals(np, op)) {
            manager.setKey(resource, np);
        }
        param = np;
    }
}
