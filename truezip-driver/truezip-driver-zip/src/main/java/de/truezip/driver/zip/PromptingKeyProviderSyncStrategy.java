/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.key.KeyProvider;
import de.truezip.kernel.key.impl.PromptingKeyProvider;

/**
 * Defines strategies for updating a key provider once an encrypted ZIP file
 * has been successfully synchronized.
 *
 * @author Christian Schlichtherle
 */
public enum PromptingKeyProviderSyncStrategy {

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