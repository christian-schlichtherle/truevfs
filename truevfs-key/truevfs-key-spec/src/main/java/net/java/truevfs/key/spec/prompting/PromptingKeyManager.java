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

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@code PromptingKeyManager} forwards the
     * call to {@link #resetCancelledKey(URI)}.
     */
    @Override
    public void release(URI resource) { resetCancelledKey(resource); }

    /**
     * Resets the state of the key provider for the given protected resource
     * if and only if prompting for the key has been cancelled.
     *
     * @param resource the URI of the protected resource.
     */
    protected void resetCancelledKey(URI resource) {
        manager.resetCancelledKey(resource);
    }

    /**
     * Resets the state of the key provider for the given protected resource
     * unconditionally.
     *
     * @param resource the URI of the protected resource.
     */
    protected void resetUnconditionally(URI resource) {
        manager.resetUnconditionally(resource);
    }

    /**
     * Returns a string representation of this object for logging and debugging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[view=%s]",
                getClass().getName(),
                getView());
    }
}
