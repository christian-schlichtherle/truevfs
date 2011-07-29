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
import java.util.ServiceConfigurationError;

/**
 * A service for key managers for secret key classes.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface KeyManagerProvider {

    /**
     * Returns the key manager for the given secret key class.
     * Subsequent calls must return the same key manager for the same secret
     * key class.
     *
     * @param  <K> the type of the secret key class.
     * @param  type the class for the secret key type.
     * @return the key manager for the given secret key class.
     * @throws ServiceConfigurationError if no appropriate key manager is
     *         available.
     */
    <K> KeyManager<K> get(Class<K> type);
}
