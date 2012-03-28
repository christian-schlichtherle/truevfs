/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import de.truezip.key.KeyPromptingCancelledException;
import de.truezip.key.SafeKey;
import de.truezip.key.UnknownKeyException;

/**
 * Used for the actual prompting of the user for a key (a password for
 * example) which is required to access a protected resource.
 * This interface is not depending on any particular techology, so
 * prompting could be implemented using Swing, the console, a web page or
 * no user interface technology at all.
 * <p>
 * Implementations of this interface are maintained by a
 * {@link PromptingKeyManager}.
 * <p>
 * Implementations of this interface must be thread safe
 * and should have no side effects!
 * 
 * @param  <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
public interface PromptingKeyProviderView<K extends SafeKey<K>> {

    /**
     * Prompts the user for the key for (over)writing the contents of a
     * new or existing protected resource.
     * Upon return, the implementation should have updated the
     * {@link PromptingKeyProviderController#setKey key} property of the given
     * {@code controller}.
     * <p>
     * If the implementation has called
     * {@link PromptingKeyProviderController#setKey} with a non-{@code null}
     * parameter, then a clone of this object will be used as the key.
     * <p>
     * Otherwise, prompting for a key is permanently disabled and each
     * subsequent call to {@link PromptingKeyProvider#getWriteKey} or
     * {@link PromptingKeyProvider#getReadKey}
     * results in a {@link KeyPromptingCancelledException} until
     * {@link PromptingKeyProvider#resetCancelledKey()} or
     * {@link PromptingKeyProvider#resetUnconditionally()} gets
     * called.
     *
     * @param  controller The key controller for storing the result.
     * @throws UnknownKeyException if key prompting fails for any reason.
     */
    void promptWriteKey(PromptingKeyProviderController<K> controller)
    throws UnknownKeyException;

    /**
     * Prompts the user for the key for reading the contents of an
     * existing protected resource.
     * Upon return, the implementation should have updated the
     * {@link PromptingKeyProviderController#setKey key} property of the given
     * {@code controller}.
     * <p>
     * If the implementation has called
     * {@link PromptingKeyProviderController#setKey} with a non-{@code null}
     * parameter, then a clone of this object will be used as the key.
     * <p>
     * Otherwise, if the implementation has called
     * {@link PromptingKeyProviderController#setKey} with a {@code null}
     * parameter or throws a {@link KeyPromptingCancelledException}, then
     * prompting for the key is permanently disabled and each subsequent call
     * to {@link PromptingKeyProvider#getWriteKey} or
     * {@link PromptingKeyProvider#getReadKey} results in a
     * {@link KeyPromptingCancelledException} until
     * {@link PromptingKeyProvider#resetCancelledKey()} or
     * {@link PromptingKeyProvider#resetUnconditionally()} gets
     * called.
     * <p>
     * Otherwise, the state of the key provider is not changed and this
     * method gets called again.
     *
     * @param  controller The key controller for storing the result.
     * @param  invalid {@code true} iff a previous call to this method
     *         resulted in an invalid key.
     * @throws UnknownKeyException if key prompting fails for any reason.
     */
    void promptReadKey( PromptingKeyProviderController<K> controller,
                        boolean invalid)
    throws UnknownKeyException;
}