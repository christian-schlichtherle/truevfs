/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api.prompting;

import global.namespace.truevfs.comp.key.api.KeyManager;
import global.namespace.truevfs.comp.key.api.KeyProvider;
import global.namespace.truevfs.comp.key.api.prompting.PromptingKey.View;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * A key manager which prompts the user for a secret key if required.
 *
 * @param <K> the type of the prompting keys.
 * @author Christian Schlichtherle
 * @see PromptingKeyManagerMap
 */
public final class PromptingKeyManager<K extends PromptingKey<K>> implements KeyManager<K> {

    private final SharedKeyManager<K> manager = new SharedKeyManager<>();
    private final View<K> view;

    /**
     * Constructs a new prompting key manager.
     *
     * @param view the view for key prompting.
     */
    public PromptingKeyManager(View<K> view) {
        this.view = requireNonNull(view);
    }

    View<K> getView() {
        return view;
    }

    @Override
    public KeyProvider<K> provider(URI uri) {
        return new PromptingKeyProvider<>(this, requireNonNull(uri),
                manager.provider(uri));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@code PromptingKeyManager} resets the
     * state of the key provider for the given protected resource if and only
     * if prompting for the key has been cancelled.
     */
    @Override
    public void release(URI uri) {
        manager.release(requireNonNull(uri));
    }

    @Override
    public void link(URI originUri, URI targetUri) {
        manager.link(requireNonNull(originUri), requireNonNull(targetUri));
    }

    @Override
    public void unlink(URI uri) {
        manager.unlink(requireNonNull(uri));
    }
}
