/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.key;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Uses a map to hold the safe key providers managed by this instance.
 *
 * @param  <K> the type of the safe keys.
 * @param  <P> the type of the safe key providers.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class SafeKeyManager<   K extends SafeKey<K>,
                                        P extends SafeKeyProvider<K>>
extends KeyManager<K> {

    private final Map<URI, P> providers = new HashMap<>();

    /** Constructs a new safe key manager. */
    protected SafeKeyManager() { }

    /**
     * Returns a new key provider.
     * 
     * @return A new key provider.
     */
    protected abstract P newKeyProvider();

    @Override
    public synchronized P make(final URI resource) {
        P provider = providers.get(Objects.requireNonNull(resource));
        if (null == provider)
            providers.put(resource, provider = newKeyProvider());
        return provider;
    }

    @Override
    public synchronized @CheckForNull P get(final URI resource) {
        return providers.get(Objects.requireNonNull(resource));
    }

    @Override
    public synchronized @CheckForNull P move(   final URI oldResource,
                                                final URI newResource) {
        if (oldResource.equals(Objects.requireNonNull(newResource)))
            throw new IllegalArgumentException();
        final P provider = providers.remove(oldResource);
        if (null != provider)
            return providers.put(newResource, provider);
        else
            return providers.remove(newResource);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned key provider is invalidated and will behave as if prompting
     * for the secret key had been disabled or cancelled by the user.
     */
    @Override
    public synchronized @CheckForNull P delete(final URI resource) {
        final P provider = providers.remove(Objects.requireNonNull(resource));
        if (null != provider)
            provider.setKey(null);
        return provider;
    }
}
