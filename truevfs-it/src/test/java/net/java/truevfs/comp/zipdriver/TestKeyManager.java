/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;

import net.java.truecommons.key.spec.AbstractKeyManager;
import net.java.truecommons.key.spec.KeyProvider;
import net.java.truecommons.key.spec.prompting.PromptingKey;
import net.java.truecommons.key.spec.prompting.PromptingKey.View;
import net.java.truecommons.key.spec.prompting.PromptingKeyManager;

/**
 * @param  <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class TestKeyManager<K extends PromptingKey<K>>
extends AbstractKeyManager<K> {

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
    public void unlink(URI resource) { manager.unlink(resource); }

    @Override
    public void release(URI resource) { unlink(resource); }
}
