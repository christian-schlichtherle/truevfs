/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.net.URI;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.PromptingKeyProvider.View;

/**
 * A key manager which prompts the user for a secret key if required.
 *
 * @param  <K> the type of the prompting keys.
 * @see    PromptingKeyManagerMap
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class PromptingKeyManager<K extends PromptingKey<K>>
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

    final View<K> getView() { return view; }

    /**
     * Returns a new prompting key provider.
     *
     * @return A new prompting key provider.
     */
    @Override
    protected final PromptingKeyProvider<K> newProvider() {
        return new PromptingKeyProvider<>(this);
    }

    @Override
    public synchronized PromptingKeyProvider<K> provider(URI resource) {
        PromptingKeyProvider<K> access = get(resource);
        if (null == access)
            (access = super.provider(resource)).setResource(resource);
        return access;
    }

    @Override
    public synchronized void move(
            final URI oldResource,
            final URI newResource) {
        final PromptingKeyProvider<K> move = get(oldResource);
        final PromptingKeyProvider<K> delete = get(newResource);
        super.move(oldResource, newResource);
        if (null != move) {
            try {
                if (null != delete) delete.setResource(null);
            } finally {
                move.setResource(newResource);
            }
        }
    }

    @Override
    public synchronized void delete(final URI resource) {
        final PromptingKeyProvider<K> delete = get(resource);
        super.delete(resource);
        if (null != delete) delete.setResource(null);
    }

    @Override
    public synchronized void release(final URI resource) {
        final PromptingKeyProvider<K> release = get(resource);
        if (null != release) release.resetCancelledKey();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[view=%s]",
                getClass().getName(),
                getView());
    }
}
