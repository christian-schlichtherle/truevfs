/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

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
extends AbstractKeyManager<K> {

    private final Map<URI, P> providers = new HashMap<>();

    /** Constructs a new safe key manager. */
    protected SafeKeyManager() { }

    /** Returns a new key provider. */
    protected abstract P newProvider();

    /**
     * Returns the mapped key provider for the given protected resource or
     * {@code null} if no key provider is mapped.
     * Note that this method is <em>not</em> synchronized!
     *
     * @param resource the URI of the protected resource.
     */
    protected final @CheckForNull P get(final URI resource) {
        return providers.get(Objects.requireNonNull(resource));
    }

    @Override
    public synchronized P provider(final URI resource) {
        P p = get(resource);
        if (null == p) providers.put(resource, p = newProvider());
        return p;
    }

    @Override
    public synchronized void move(
            final URI oldResource,
            final URI newResource) {
        Objects.requireNonNull(newResource);
        final P p = providers.remove(Objects.requireNonNull(oldResource));
        if (null != p) providers.put(newResource, p);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned key provider is invalidated and will behave as if prompting
     * for the key had been disabled or cancelled by the user.
     */
    @Override
    public synchronized void delete(final URI resource) {
        final P p = providers.remove(Objects.requireNonNull(resource));
        if (null != p) p.setKey(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@code SafeKeyManager} does nothing.
     */
    @Override
    public void release(URI resource) { }
}
