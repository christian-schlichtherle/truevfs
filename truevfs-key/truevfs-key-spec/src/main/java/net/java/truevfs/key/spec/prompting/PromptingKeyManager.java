/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import java.net.URI;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.AbstractKeyManager;
import net.java.truevfs.key.spec.prompting.PromptingKey.View;

/**
 * A key manager which prompts the user for a secret key if required.
 *
 * @param  <K> the type of the prompting keys.
 * @see    PromptingKeyManagerMap
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class PromptingKeyManager<K extends PromptingKey<K>>
extends AbstractKeyManager<K> {

    private final SharedKeyManager<K> manager = new SharedKeyManager<>();
    private final View<K> view;

    /**
     * Constructs a new prompting key manager.
     *
     * @param view the view for key prompting.
     */
    public PromptingKeyManager(final View<K> view) {
        this.view = Objects.requireNonNull(view);
    }

    public final View<K> getView() { return view; }

    protected PromptingKeyProvider<K> get(final URI resource) {
        final SharedKeyProvider<K> p = manager.get(resource);
        return null == p ? null : new PromptingKeyProvider<>(this, resource, p);
    }

    @Override
    public PromptingKeyProvider<K> provider(URI resource) {
        return new PromptingKeyProvider<>(this, resource,
                manager.provider(resource));
    }

    @Override
    public void link(URI oldResource, URI newResource) {
        manager.link(oldResource, newResource);
    }

    @Override
    public void unlink(URI resource) {
        manager.unlink(resource);
    }

    @Override
    public void release(URI resource) {
        manager.release(resource);
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
