/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.key;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import net.jcip.annotations.ThreadSafe;

/**
 * A key manager which prompts the user for a key if required.
 *
 * @param   <K> The type of the keys.
 * @see     PromptingKeyProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class PromptingKeyManager<K extends SafeKey<K>>
extends SafeKeyManager<K, PromptingKeyProvider<K>> {

    private static class PromptingKeyProviderFactory<K extends SafeKey<K>>
    implements KeyProvider.Factory<K, PromptingKeyProvider<K>> {

        @Override
        public PromptingKeyProvider<K> newKeyProvider() {
            return new PromptingKeyProvider<K>();
        }
    }

    /**
     * The user interface classes or instances.
     * Values may be instances of {@link PromptingKeyProviderUI} or
     * {@link Class}.
     */
    private final PromptingKeyProvider.UI<? extends K> ui;

    /**
     * Constructs a new prompting key manager.
     *
     * @param ui the user interface for prompting for keys.
     */
    public PromptingKeyManager(final PromptingKeyProvider.UI<? extends K> ui) {
        super(new PromptingKeyProviderFactory<K>());
        if (null == ui)
            throw new NullPointerException();
        this.ui = ui;
    }

    @Override
    public synchronized PromptingKeyProvider<K> getKeyProvider(URI resource) {
        PromptingKeyProvider<K> provider = super.getKeyProvider(resource);
        provider.setResource(resource);
        provider.setUI(ui);
        return provider;
    }

    @Override
    public synchronized boolean moveKeyProvider(URI oldResource, URI newResource) {
        boolean changed = super.moveKeyProvider(oldResource, newResource);
        if (changed) {
            PromptingKeyProvider<K> provider = super.getKeyProvider(newResource);
            provider.setResource(newResource);
            provider.setUI(ui);
        }
        return changed;
    }

    @Override
    public synchronized boolean removeKeyProvider(URI resource) {
        PromptingKeyProvider<K> provider = super.findKeyProvider(resource);
        boolean changed = super.removeKeyProvider(resource);
        if (changed) {
            provider.setResource(null);
            provider.setUI(null);
        }
        return changed;
    }
}
