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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider;

/**
 * Defines strategies for updating a key provider once an encrypted ZIP file
 * has been successfully synchronized.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public enum KeyProviderSyncStrategy {

    /**
     * Calls {@link PromptingKeyProvider#resetCancelledKey}
     * if and only if the given provider is a {@link PromptingKeyProvider}.
     */
    RESET_CANCELLED_KEY {
        @Override
        public void sync(KeyProvider<?> provider) {
            if (provider instanceof PromptingKeyProvider<?>)
                ((PromptingKeyProvider<?>) provider).resetCancelledKey();
        }
    },
    
    /**
     * Calls {@link PromptingKeyProvider#resetUnconditionally}
     * if and only if the given provider is a {@link PromptingKeyProvider}.
     */
    RESET_UNCONDITIONALLY {
        @Override
        public void sync(KeyProvider<?> provider) {
            if (provider instanceof PromptingKeyProvider<?>)
                ((PromptingKeyProvider<?>) provider).resetUnconditionally();
        }
    };

    /**
     * This method gets called upon a call to {@link ZipController#sync}
     * after a successful synchronization of an encrypted ZIP file.
     *
     * @param provider the key provider for the encrypted ZIP file which has
     *        been successfully synchronized.
     */
    public abstract void sync(KeyProvider<?> provider);
}
