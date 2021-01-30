/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.macosx;

import net.java.truecommons3.key.spec.KeyProvider;
import net.java.truecommons3.key.spec.UnknownKeyException;
import net.java.truecommons3.key.spec.prompting.AbstractPromptingPbeParameters;
import net.java.truecommons3.shed.Option;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.util.Objects;

/**
 * Interacts with the {@link OsxKeyManager} to persist passwords into Apple's
 * Keychain Services API.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
@ThreadSafe
final class OsxKeyProvider<P extends AbstractPromptingPbeParameters<P, ?>>
implements KeyProvider<P> {

    private final OsxKeyManager<P> manager;
    private final URI resource;
    private final KeyProvider<P> provider;
    private volatile Option<P> param = Option.none();

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
        Option<P> op = param;
        if (op.isEmpty())
            op = manager.getKey(resource);
        for (P p : op)
            if (!p.isChangeRequested())
                return p.clone();
        final Option<P> np = Option.some(provider.getKeyForWriting());
        if (!np.equals(op))
            manager.setKey(resource, np);
        return (param = np).get();
    }

    @Override
    public P getKeyForReading(final boolean invalid)
    throws UnknownKeyException {
        if (!invalid) {
            Option<P> op = param;
            if (op.isEmpty())
                op = manager.getKey(resource);
            for (P p : op)
                return p.clone();
        }
        return provider.getKeyForReading(invalid);
    }

    @Override
    public void setKey(final @Nullable P key) {
        final Option<P> op = param;
        final Option<P> np = Option.apply(key);
        provider.setKey(key);
        if (!Objects.equals(np, op))
            manager.setKey(resource, np);
        param = np;
    }
}
