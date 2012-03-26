/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Uses a map to hold the safe key providers managed by this instance.
 *
 * @param  <K> the type of the safe keys.
 * @param  <P> the type of the safe key providers.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class SafeKeyManager<K extends SafeKey<K>, P extends SafeKeyProvider<K>>
extends KeyManager<K> {

    private final Map<URI, P> providers = new HashMap<URI, P>();

    /** Constructs a new safe key manager. */
    protected SafeKeyManager() { }

    /**
     * Returns a new key provider.
     * 
     * @return A new key provider.
     */
    protected abstract P newKeyProvider();

    @Override
    public synchronized P get(final URI resource) {
        if (null == resource)
            throw new NullPointerException();
        return providers.get(resource);
    }

    @Override
    public synchronized P make(final URI resource) {
        if (null == resource)
            throw new NullPointerException();
        P provider = providers.get(resource);
        if (null == provider)
            providers.put(resource, provider = newKeyProvider());
        return provider;
    }

    @Override
    public synchronized P move(  final URI oldResource,
                                            final URI newResource) {
        if (null == newResource)
            throw new NullPointerException();
        if (oldResource.equals(newResource))
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
    public synchronized P delete(final URI resource) {
        if (null == resource)
            throw new NullPointerException();
        final P provider = providers.remove(resource);
        if (null != provider)
            provider.setKey(null);
        return provider;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d]",
                getClass().getName(),
                getPriority());
    }
}