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
 * A container for key providers for protected resources which need to
 * get created or opened by client applications.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param   <K> The type of the keys.
 * @param   <P> The type of the providers.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface KeyManager<K, P extends KeyProvider<K>> {

    /**
     * Returns the key provider for the given protected resource.
     * If no key provider is mapped, a new key provider is created and returned.
     *
     * @param  resource the URI of the protected resource.
     * @return The key provider mapped for the protected resource.
     */
    P getKeyProvider(URI resource);

    /**
     * Moves the key provider from the URI {@code oldResource} to
     * {@code newResource}.
     * 
     * @param  oldResource the old URI of the protected resource.
     * @param  newResource the new URI of the protected resource.
     * @return whether or not the operation succeeded, i.e. if a key provider
     *         was present for the old URI.
     */
    boolean moveKeyProvider(URI oldResource, URI newResource);

    /**
     * Removes the key provider for the given protected resource.
     *
     * @param  resource the URI of the protected resource.
     * @return whether or not the operation succeeded, i.e. if a key provider
     *         was present for the old URI.
     */
    boolean removeKeyProvider(URI resource);
}
