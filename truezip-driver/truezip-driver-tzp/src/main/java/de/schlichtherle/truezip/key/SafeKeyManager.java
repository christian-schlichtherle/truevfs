/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.ThreadSafe;

/**
 * Uses a map to hold the safe key providers managed by this instance.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class SafeKeyManager<K extends SafeKey<K>, P extends SafeKeyProvider<K>>
implements KeyManager<K> {

    private final Map<URI, P> providers = new HashMap<URI, P>();
    private final KeyProvider.Factory<? extends K, ? extends P> factory;

    /**
     * Constructs a new default key manager.
     *
     * @param factory the factory for creating new key providers.
     */
    public SafeKeyManager(
            final KeyProvider.Factory<? extends K, ? extends P> factory) {
        if (null == factory)
            throw new NullPointerException();
        this.factory = factory;
    }

    @Override
    public synchronized P getKeyProvider(URI resource) {
        if (null == resource)
            throw new NullPointerException();
        P provider = providers.get(resource);
        if (null == provider) {
            provider = factory.newKeyProvider();
            providers.put(resource, provider);
        }
        return provider;
    }

    P findKeyProvider(URI resource) {
        return providers.get(resource);
    }

    @Override
    public synchronized boolean moveKeyProvider(URI oldResource, URI newResource) {
        if (oldResource.equals(newResource))
            return false;
        if (null == newResource)
            throw new NullPointerException();
        final P provider = providers.remove(oldResource);
        if (null != provider) {
            providers.put(newResource, provider);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean removeKeyProvider(URI resource) {
        if (null == resource)
            throw new NullPointerException();
        return null != providers.remove(resource);
    }
}
