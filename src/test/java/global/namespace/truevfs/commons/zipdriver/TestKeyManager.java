/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.zipdriver;

import global.namespace.truevfs.commons.key.api.KeyManager;
import global.namespace.truevfs.commons.key.api.KeyProvider;
import global.namespace.truevfs.commons.key.api.prompting.PromptingKey;
import global.namespace.truevfs.commons.key.api.prompting.PromptingKey.View;
import global.namespace.truevfs.commons.key.api.prompting.PromptingKeyManager;

import java.net.URI;

/**
 * @param <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
public final class TestKeyManager<K extends PromptingKey<K>> implements KeyManager<K> {

    private final PromptingKeyManager<K> manager;

    public TestKeyManager(View<K> view) {
        this.manager = new PromptingKeyManager<>(view);
    }

    @Override
    public KeyProvider<K> provider(URI resource) {
        return manager.provider(resource);
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
        unlink(resource);
    }
}
