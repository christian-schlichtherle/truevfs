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

import de.schlichtherle.truezip.key.KeyProvider.Factory;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
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

    private final View<? extends K> view;

    /**
     * Constructs a new prompting key manager.
     *
     * @param view the view instance for prompting for keys.
     */
    public PromptingKeyManager(final View<? extends K> view) {
        super(new PromptingKeyProviderFactory<K>());
        if (null == view)
            throw new NullPointerException();
        this.view = view;
    }

    @Override
    public synchronized PromptingKeyProvider<K> getKeyProvider(URI resource) {
        PromptingKeyProvider<K> provider = super.getKeyProvider(resource);
        provider.setResource(resource);
        provider.setView(view);
        return provider;
    }

    @Override
    public synchronized boolean moveKeyProvider(URI oldResource, URI newResource) {
        boolean changed = super.moveKeyProvider(oldResource, newResource);
        if (changed) {
            PromptingKeyProvider<K> provider = super.getKeyProvider(newResource);
            provider.setResource(newResource);
            provider.setView(view);
        }
        return changed;
    }

    @Override
    public synchronized boolean removeKeyProvider(URI resource) {
        PromptingKeyProvider<K> provider = super.findKeyProvider(resource);
        boolean changed = super.removeKeyProvider(resource);
        if (changed) {
            provider.setResource(null);
            provider.setView(null);
        }
        return changed;
    }

    /** A factory for {@link PromptingKeyProvider}s. */
    private static class PromptingKeyProviderFactory<K extends SafeKey<K>>
    implements Factory<K, PromptingKeyProvider<K>> {

        @Override
        public PromptingKeyProvider<K> newKeyProvider() {
            return new PromptingKeyProvider<K>();
        }
    } // class PromptingKeyProviderFactory
}
