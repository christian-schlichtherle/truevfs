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
package de.schlichtherle.truezip.crypto.raes.param.swing;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerService;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import java.util.ServiceConfigurationError;

/**
 * A service for a swing based key manager implementation for
 * {@link AesCipherParameters}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SwingKeyManagerService implements KeyManagerService {

    private static final PromptingKeyManager<AesCipherParameters>
            manager = new PromptingKeyManager<AesCipherParameters>(
                new AesCipherParametersUI());

    @Override
    @SuppressWarnings("unchecked")
    public <K> KeyManager<? extends K, ?> getManager(Class<K> type) {
        if (type.isAssignableFrom(AesCipherParameters.class))
            return (KeyManager<? extends K, ?>) manager;
        throw new ServiceConfigurationError("No service available for " + type);
    }
}
