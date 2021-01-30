/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.prompting;

import net.java.truecommons3.key.spec.Key;
import net.java.truecommons3.key.spec.UnknownKeyException;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * A prompting key for writing and reading protected resources.
 * <p>
 * Implementations need to be stateful and hence do not need to be thread-safe.
 *
 * @param  <K> the type of this prompting key.
 * @see    PromptingKeyProvider
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public interface PromptingKey<K extends PromptingKey<K>>
extends Key<K> {

    /**
     * Returns whether or not the user shall get prompted for a new key upon
     * the next call to {@link PromptingKeyProvider#getKeyForWriting},
     * provided that the key has been {@link PromptingKeyProvider#setKey set}
     * before.
     *
     * @return Whether or not the user shall get prompted for a new key upon
     *         the next call to {@link PromptingKeyProvider#getKeyForWriting},
     *         provided that the key has been
     *         {@link PromptingKeyProvider#setKey set} before.
     */
    boolean isChangeRequested();

    /**
     * Requests to prompt the user for a new key upon the next call to
     * {@link PromptingKeyProvider#getKeyForWriting}, provided that the key
     * has been {@link PromptingKeyProvider#setKey set} by then.
     *
     * @param  changeRequested whether or not the user shall get prompted
     *         for a new key upon the next call to
     *         {@link PromptingKeyProvider#getKeyForWriting}, provided that the
     *         key has been {@link PromptingKeyProvider#setKey set} by then.
     */
    void setChangeRequested(boolean changeRequested);

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
     * Implementations must be thread-safe!
     *
     * @param  <K> the type of the safe keys.
     * @author Christian Schlichtherle
     */
    interface View<K extends PromptingKey<K>> {

        /**
         * Prompts the user for the key for (over)writing the contents of a
         * new or existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link Controller#setKeyClone key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called
         * {@link Controller#setKeyClone} with a non-{@code null}
         * parameter, then a clone of this object will be used as the key.
         * <p>
         * Otherwise, prompting for a key is permanently disabled and each
         * subsequent call to {@link PromptingKeyProvider#getKeyForWriting} or
         * {@link PromptingKeyProvider#getKeyForReading}
         * results in a {@link KeyPromptingCancelledException} until
         * {@link PromptingKeyProvider#resetCancelledKey()} or
         * {@link PromptingKeyProvider#resetUnconditionally()} gets
         * called.
         *
         * @param  controller The key controller for storing the result.
         * @throws UnknownKeyException if key prompting fails for any reason.
         */
        void promptKeyForWriting(Controller<K> controller)
        throws UnknownKeyException;

        /**
         * Prompts the user for the key for reading the contents of an
         * existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link Controller#setKeyClone key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called
         * {@link Controller#setKeyClone} with a non-{@code null}
         * parameter, then a clone of this object will be used as the key.
         * <p>
         * Otherwise, if the implementation has called
         * {@link Controller#setKeyClone} with a {@code null}
         * parameter or throws a {@link KeyPromptingCancelledException}, then
         * prompting for the key is permanently disabled and each subsequent call
         * to {@link PromptingKeyProvider#getKeyForWriting} or
         * {@link PromptingKeyProvider#getKeyForReading} results in a
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
        void promptKeyForReading(Controller<K> controller, boolean invalid)
        throws UnknownKeyException;
    }

    /**
     * Proxies access to the key for {@link View} implementations.
     * <p>
     * Implementations need to be stateful and hence do not need to be
     * thread-safe.
     *
     * @param  <K> the type of the safe keys.
     * @author Christian Schlichtherle
     */
    interface Controller<K extends PromptingKey<K>> {

        /**
         * Returns the unique resource identifier (resource ID) of the
         * protected resource for which this controller is used.
         *
         * @return The unique resource identifier (resource ID) of the
         *         protected resource for which this controller is used.
         * @throws IllegalStateException if getting this property is not legal
         *         in the current state.
         */
        URI getResource();

        /**
         * Returns a clone of the protected resource's key or {@code null} if
         * no key has been set.
         *
         * @return The protected resource's key.
         * @throws IllegalStateException if getting key is not legal in the
         *         current state.
         */
        @Nullable K getKeyClone();

        /**
         * Sets the protected resource's key to a clone of the given key or
         * resets it to null.
         *
         * @param  key The nullable key to clone and use for accessing the
         *         protected resource.
         * @throws IllegalStateException if setting key is not legal in the
         *         current state.
         */
        void setKeyClone(@Nullable K key);
    }
}
