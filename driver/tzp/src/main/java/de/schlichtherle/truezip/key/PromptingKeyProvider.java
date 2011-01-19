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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.key.PromptingKeyProvider.State.*;

/**
 * A "safe" key provider which prompts the user for a key for its protected
 * resource.
 * The user is prompted via an instance of the {@link UI} user interface which
 * is {@link #setUI injected} to this instance by a {@link PromptingKeyManager}.
 * The UI may then display the resource URI by calling {@link #getResource} on
 * this instance (which is also {@link #setResource injected} by a
 * {@link PromptingKeyManager}) and finally set the key by calling
 * {@link #setKey}.
 *
 * @param   <K> The type of the keys.
 * @see     PromptingKeyManager
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class PromptingKeyProvider<K extends SafeKey<K>>
extends SafeKeyProvider<K> {

    /**
     * Used for the actual prompting of the user for a key (a password for
     * example) which is required to access a protected resource.
     * This interface is not depending on any particular user interface
     * techology, so prompting could be implemented using Swing, the console,
     * a web page or any other user interface technology.
     * <p>
     * Implementations of this interface are maintained by a
     * {@link PromptingKeyManager} and injected into the
     * {@link PromptingKeyProvider} before
     * {@link PromptingKeyProvider#getCreateKey()} or
     * {@link PromptingKeyProvider#getOpenKey(boolean)} is called.
     * <p>
     * Implementations of this interface <em>must</em> be thread safe
     * and should have no side effects!
     */
    @DefaultAnnotation(NonNull.class)
    public interface UI<K extends SafeKey<K>> {

        /**
         * Prompts the user for the key which may be used to create a new
         * protected resource or entirely replace the contents of an already
         * existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link #setKey key} property of the given {@code provider}.
         * <p>
         * If the implementation has called {@link #setKey} with a
         * non-{@code null} parameter, then a clone of this object will be
         * used as the key of the given {@code provider}.
         * <p>
         * Otherwise, prompting for a key is permanently disabled and each
         * subsequent call to {@link #getCreateKey} or {@link #getOpenKey}
         * results in a {@link KeyPromptingCancelledException} until
         * {@link #resetCancelledKey()} or {@link #resetUnconditionally()} gets
         * called.
         *
         * @param  provider The key provider to store the result in.
         * @throws UnknownKeyException if key prompting fails for any reason.
         */
        void promptCreateKey(PromptingKeyProvider<? super K> provider)
        throws UnknownKeyException;

        /**
         * Prompts the user for the key which may be used to open an existing
         * protected resource in order to access its contents.
         * Upon return, the implementation should have updated the
         * {@link #setKey key} property of the given {@code provider}.
         * <p>
         * If the implementation has called {@link #setKey} with a
         * non-{@code null} parameter, then a clone of this object will be
         * used as the key of the given {@code provider}.
         * <p>
         * Otherwise, if the implementation has called {@link #setKey} with a
         * {@code null} parameter or throws a
         * {@link KeyPromptingCancelledException}, then prompting for the key
         * is permanently disabled and each subsequent call to
         * {@link #getCreateKey} or {@link #getOpenKey} results in a
         * {@link KeyPromptingCancelledException} until
         * {@link #resetCancelledKey()} or {@link #resetUnconditionally()} gets
         * called.
         * <p>
         * Otherwise, the state of the key provider is not changed and this
         * method gets called again.
         *
         * @param  provider The key provider to store the result in.
         * @param  invalid {@code true} iff a previous call to this method
         *         resulted in an invalid key.
         * @throws KeyPromptingCancelledException if key prompting has been
         *         cancelled by the user.
         * @throws UnknownKeyException if key prompting fails for any other
         *         reason.
         */
        void promptOpenKey( PromptingKeyProvider<? super K> provider,
                            boolean invalid)
        throws UnknownKeyException;
    } // interface UI

    private volatile @NonNull State state = RESET;

    /** The resource identifier for the protected resource. */
    private volatile URI resource;

    /**
     * The user interface instance which is used to prompt the user for a key.
     */
    private volatile UI<? extends K> ui;

    private volatile K key;

    private volatile boolean changeKeySelected;

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    public URI getResource() {
        return resource;
    }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    void setResource(final URI resource) {
        this.resource = resource;
    }

    final UI<? extends K> getUI() {
        return ui;
    }

    final void setUI(final UI<? extends K> ui) {
        this.ui = ui;
    }

    @NonNull State getState() {
        return state;
    }

    private void setState(final @NonNull State state) {
        this.state = state;
    }

    /**
     * Returns the key which should be used to create a new protected
     * resource or entirely replace the contents of an already existing
     * protected resource.
     * <p>
     * If required or explicitly requested by the user, the user is prompted
     * for this key.
     *
     * @throws UnknownKeyException If the user has cancelled prompting or
     *         prompting has been disabled by the {@link PromptingKeyManager}.
     * @see KeyProvider#getCreateKey
     */
    @Override
    protected final K getCreateKeyImpl() throws UnknownKeyException {
        return getState().getCreateKey(this);
    }

    /**
     * Returns the key which should be used to open an existing protected
     * resource in order to access its contents.
     * <p>
     * If required, the user is prompted for this key.
     *
     * @throws UnknownKeyException If the user has cancelled prompting or
     *         prompting has been disabled by the {@link PromptingKeyManager}.
     * @see KeyProvider#getOpenKey
     */
    @Override
    protected final K getOpenKeyImpl(boolean invalid)
    throws UnknownKeyException {
        return getState().getOpenKey(this, invalid);
    }

    /**
     * Returns the {@code key} property maintained by this key provider.
     * Client applications should not call this method directly
     * but rather call {@link #getOpenKey} or {@link #getCreateKey}:
     * It's intended to be used by subclasses and user interface classes only.
     *
     * @return The nullable {@code key} property.
     */
    K getKey() {
        return clone(key);
    }

    /**
     * Sets the {@code key} property maintained by this key provider.
     * Client applications should not call this method directly:
     * It's intended to be used by user interface classes only.
     *
     * @param  key The {@code key} property.
     * @throws IllegalStateException if setting the key is not legal in the
     *         current state.
     */
    public void setKey(@CheckForNull K key) {
        getState().setKey(this, key);
    }

    private void setKeyImpl(final @CheckForNull K newKey) {
        // This is quite paranoid, but supposedly fairly safe.
        final K oldKey = this.key;
        this.key = clone(newKey);
        reset(oldKey);
        reset(newKey);
    }

    /**
     * Returns whether or not the user shall get prompted for a new key upon
     * the next call to {@link #getCreateKey()}, provided that the key
     * has been {@link #setKey set} before.
     *
     * @return Whether or not the user shall get prompted for a new key upon
     *         the next call to {@link #getCreateKey()}, provided that the key
     *         has been {@link #setKey set} before.
     */
    boolean isChangeKeySelected() {
        return changeKeySelected;
    }

    /**
     * Requests to prompt the user for a new key upon the next call to
     * {@link #getCreateKey()}, provided that the key has been
     * {@link #setKey set} before.
     *
     * @param changeKeySelected whether or not the user shall get prompted for
     *        a new key upon the next call to {@link #getCreateKey()}, provided
     *        that the key has been {@link #setKey set} before.
     */
    public void setChangeKeySelected(final boolean changeKeySelected) {
        this.changeKeySelected = changeKeySelected;
    }

    /**
     * Resets the state of this key provider and the current key
     * if and only if prompting for a key has been cancelled.
     */
    public void resetCancelledKey() {
        getState().resetCancelledKey(this);
    }

    /**
     * Resets the state of this key provider and the current key
     * unconditionally.
     */
    public void resetUnconditionally() {
        reset();
    }

    private void reset() {
        setState(RESET);
        reset(this.key);
    }

    @DefaultAnnotation(NonNull.class)
    enum State {
        RESET {
            @Override
            <K extends SafeKey<K>> K
            getCreateKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                State state;
                try {
                    provider.getUI().promptCreateKey(provider);
                } finally {
                    if ((state = provider.getState()) == this)
                        provider.setState(state = CANCELLED);
                }
                return state.getCreateKey(provider);
            }

            @Override
            <K extends SafeKey<K>> K
            getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                State state;
                do {
                    try {
                        provider.getUI().promptOpenKey(provider, invalid);
                    } catch (KeyPromptingCancelledException ex) {
                        provider.setState(CANCELLED);
                        throw ex;
                    }
                    state = provider.getState();
                } while (state == this);
                return state.getOpenKey(provider, false);
            }

            @Override
            <K extends SafeKey<K>> void
            setKey(PromptingKeyProvider<K> provider, K key) {
                provider.setKeyImpl(key);
                provider.setState(null != key ? PROVIDED : CANCELLED);
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        PROVIDED {
            @Override
            <K extends SafeKey<K>> K
            getCreateKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                if (provider.isChangeKeySelected()) {
                    provider.setChangeKeySelected(false);
                    return RESET.getCreateKey(provider); // DON'T change state!
                } else {
                    return provider.getKey();
                }
            }

            @Override
            <K extends SafeKey<K>> K
            getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                if (invalid) {
                    provider.setState(RESET);
                    return RESET.getOpenKey(provider, true);
                } else {
                    return provider.getKey();
                }
            }

            @Override
            <K extends SafeKey<K>> void
            setKey(PromptingKeyProvider<K> provider, K key) {
                throw new IllegalStateException(toString());
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        CANCELLED {
            @Override
            <K extends SafeKey<K>> K
            getCreateKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                throw new KeyPromptingCancelledException();
            }

            @Override
            <K extends SafeKey<K>> K
            getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                throw new KeyPromptingCancelledException();
            }

            @Override
            <K extends SafeKey<K>> void
            setKey(PromptingKeyProvider<K> provider, K key) {
                throw new IllegalStateException(toString());
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
                provider.reset();
            }
        };

        abstract <K extends SafeKey<K>> K
        getCreateKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> K
        getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> void
        setKey(PromptingKeyProvider<K> provider, @CheckForNull K key);

        abstract <K extends SafeKey<K>> void
        resetCancelledKey(PromptingKeyProvider<K> provider);
    }
}
