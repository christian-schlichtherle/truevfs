/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import de.truezip.key.PromptingKeyProvider.View;
import java.net.URI;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A key manager which prompts the user for a key if required.
 *
 * @param  <K> the type of the safe keys.
 * @see    PromptingKeyProvider
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class PromptingKeyManager<K extends SafeKey<K>>
extends SafeKeyManager<K, PromptingKeyProvider<K>> {

    private final View<K> view;

    /**
     * Constructs a new prompting key manager.
     *
     * @param view the view instance for prompting for keys.
     */
    public PromptingKeyManager(final View<K> view) {
        this.view = Objects.requireNonNull(view);
    }

    final View<K> getView() {
        return view;
    }

    /**
     * Returns a new prompting key provider.
     * 
     * @return A new prompting key provider.
     */
    @Override
    protected final PromptingKeyProvider<K> newKeyProvider() {
        return new PromptingKeyProvider<>(this);
    }

    @Override
    public final synchronized PromptingKeyProvider<K> make(final URI resource) {
        final PromptingKeyProvider<K> provider = super.make(resource);
        provider.setResource(resource);
        return provider;
    }

    @Override
    public final synchronized @CheckForNull PromptingKeyProvider<K> get(final URI resource) {
        final PromptingKeyProvider<K> provider = super.get(resource);
        if (null != provider)
            provider.setResource(resource);
        return provider;
    }

    @Override
    public final synchronized @CheckForNull PromptingKeyProvider<K> move(URI oldResource, URI newResource) {
        final PromptingKeyProvider<K>
                oldProvider = super.move(oldResource, newResource);
        if (null != oldProvider)
            oldProvider.setResource(null);
        final PromptingKeyProvider<K>
                newProvider = super.get(newResource);
        if (null != newProvider)
            newProvider.setResource(newResource);
        return oldProvider;
    }

    @Override
    public final synchronized @CheckForNull PromptingKeyProvider<K> delete(URI resource) {
        final PromptingKeyProvider<K> provider = super.delete(resource);
        if (null != provider)
            provider.setResource(null);
        return provider;
    }

    @Override
    public synchronized void unlock(URI resource) {
        final PromptingKeyProvider<K> provider = get(resource);
        if (null != provider)
            provider.resetCancelledKey();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[view=%s, priority=%d]",
                getClass().getName(),
                getView(),
                getPriority());
    }
}