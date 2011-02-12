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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ServiceConfigurationError;

/**
 * A service for key managers.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id: FsManagers$
 */
public interface KeyManagerService {

    /**
     * Returns the key manager for the given key type.
     * <p>
     * Calling this method multiple times must return the same key manager
     * for the same key type in order to ensure consistency of the key space.
     *
     * @return The key manager for the given key type.
     * @throws ServiceConfigurationError if no appropriate key manager is
     *         available.
     */
    @NonNull <K> KeyManager<? extends K, ?> getKeyManager(@NonNull Class<K> type);
}
