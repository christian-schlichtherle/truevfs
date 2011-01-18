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
    public interface UI<K extends SafeKey<K>> {

        /**
         * Prompts the user for the key which may be used to create a new
         * protected resource or entirely replace the contents of an already
         * existing protected resource.
         * <p>
         * Upon return, the implementation is expected to update the common key
         * in {@code provider}.
         * Upon return, if {@code provider.getKey()} returns {@code null},
         * prompting for the key is assumed to have been cancelled by the user.
         * In this case, the current and each subsequent call to
         * {@link KeyProvider#getOpenKey} or {@link KeyProvider#getCreateKey}
         * by the client results in an {@link UnknownKeyException} and the user
         * is not prompted anymore until the provider is resetUnconditionally by the
         * {@link KeyManager}.
         * Otherwise, the key is used as the common key, a clone of which is
         * provided to the client upon request.
         * <p>
         * <b>Hint:</b> If the user cancels the dialog, it is recommended to
         * leave the provider's {@code key} property simply unmodified.
         * This causes the old key to be reused and allows the client to
         * continue its operation as if the user would not have requested to
         * change the key.
         *
         * @param  provider The key provider to store the result in.
         *         The property {@code resourceID} must be
         *         non-{@code null}.
         * @throws NullPointerException if either {@code provider} or its property
         *         {@code resourceID} is {@code null}.
         * @throws UnknownKeyException if the implementation does not want the
         *         key provider's state to be changed.
         *         This may be useful if prompting was interrupted by a call to
         *         {@link Thread#interrupt} while waiting on user input.
         *         In this case, another attempt to prompt the user should have
         *         the chance to succeed instead of being cancelled without
         *         actually prompting the user again.
         * @see    KeyPromptingInterruptedException
         */
        void promptCreateKey(@NonNull PromptingKeyProvider<? super K> provider)
        throws UnknownKeyException;

        /**
         * Prompts the user for the key which may be used to open an existing
         * protected resource in order to access its contents.
         * <p>
         * Upon return, the implementation is expected to update the common key
         * in {@code provider}.
         * Upon return, if {@code provider.getKey()} returns {@code null},
         * prompting for the key is assumed to have been cancelled by the user.
         * In this case, the current and each subsequent call to
         * {@link KeyProvider#getOpenKey} or {@link KeyProvider#getCreateKey}
         * by the client results in an {@link UnknownKeyException} and the user
         * is not prompted anymore until the provider is resetUnconditionally by the
         * {@link KeyManager}.
         * Otherwise, the key is used as the common key, a clone of which is
         * provided to the client upon request.
         *
         * @param  provider The key provider to store the result in.
         *         The property {@code resourceID} must be
         *         non-{@code null}.
         * @param  invalid {@code true} iff a previous call to this method resulted
         *         in an invalid key.
         * @return {@code true} if the user has requested to change the provided
         *         key.
         * @throws NullPointerException if either {@code provider} or its property
         *         {@code resourceID} is {@code null}.
         * @throws UnknownKeyException if the implementation does not want the
         *         key provider's state to be changed.
         *         This may be useful if prompting was interrupted by a call to
         *         {@link Thread#interrupt} while waiting on user input.
         *         In this case, another attempt to prompt the user should have
         *         the chance to succeed instead of being cancelled without
         *         actually prompting the user again.
         * @see    KeyPromptingInterruptedException
         */
        boolean promptOpenKey(@NonNull PromptingKeyProvider<? super K> provider, boolean invalid)
        throws UnknownKeyException;
    } // interface UI

    private static class PromptingLock { }

    /**
     * Used to lock out prompting by multiple threads.
     * Note that the prompting methods in this class <em>must not</em> be
     * synchronized on this instance since this would cause the Swing
     * based default implementation of the key manager to dead lock.
     * This is because the GUI is run from AWT's Event Dispatching Thread,
     * which must call some methods of this instance while another thread
     * is waiting for the key manager to return from prompting.
     * Instead, the prompting methods use this object to lock out concurrent
     * prompting by multiple threads.
     */
    private final PromptingLock lock = new PromptingLock();

    /** The resource identifier for the protected resource. */
    private volatile URI resource;

    /**
     * The user interface instance which is used to prompt the user for a key.
     */
    private volatile UI<? extends K> ui;

    private volatile @NonNull State state = RESET;

    private volatile K key;

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

    private @NonNull State getState() {
        return state;
    }

    private void setState(final @NonNull State state) {
        this.state = state;
    }

    /**
     * Returns the {@code key} property maintained by this key provider.
     * Client applications should not call this method directly
     * but rather call {@link #getOpenKey} or {@link #getCreateKey}:
     * It's intended to be used by subclasses and user interface classes only.
     *
     * @return The {@code key} property - may be {@code null}.
     */
    public K getKey() {
        return clone(key);
    }

    /**
     * Sets the {@code key} property maintained by this key provider.
     * Client applications should not call this method directly:
     * It's intended to be used by user interface classes only.
     *
     * @param key The {@code key} property - may be {@code null}.
     */
    // FIXME: Make the behaviour depend upon the state - throw
    // IllegalStateException if not allowed!
    public void setKey(final K key) {
        this.key = clone(key);
        reset(key);
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
        synchronized (lock) {
            return getState().getCreateKey(this);
        }
    }

    /**
     * Prompts for the key which should be used to create a new protected
     * resource or entirely replace the contents of an already existing
     * protected resource.
     */
    private @NonNull K promptCreateKey() throws UnknownKeyException {
        final K oldKey = this.key;
        getUI().promptCreateKey(this);
        reset(oldKey);

        final K newKey = getKey();
        if (newKey != null) {
            setState(KEY_PROVIDED);
            return newKey;
        } else {
            setState(CANCELLED);
            throw new KeyPromptingCancelledException();
        }
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
    protected final K getOpenKeyImpl(boolean invalid) throws UnknownKeyException {
        synchronized (lock) {
            return getState().getOpenKey(this, invalid);
        }
    }

    /**
     * Prompts for the key which should be used to open an existing protected
     * resource in order to access its contents.
     */
    private @NonNull K promptOpenKey(final boolean invalid) throws UnknownKeyException {
        final K oldKey = this.key;
        final boolean changeKey = getUI().promptOpenKey(this, invalid);
        reset(oldKey);

        final K newKey = getKey();
        if (newKey != null) {
            setState(changeKey ? KEY_CHANGE_REQUESTED : KEY_PROVIDED);
            return newKey;
        } else {
            setState(CANCELLED);
            throw new KeyPromptingCancelledException();
        }
    }

    /**
     * Resets the state of this key provider and the current key
     * if and only if prompting for a key has been cancelled.
     */
    public void resetUnknownKey() {
        synchronized (lock) {
            getState().resetUnknownKey(this);
        }
    }

    /**
     * Resets the state of this key provider and the current key
     * unconditionally.
     */
    public void resetUnconditionally() {
        synchronized (lock) {
            reset();
        }
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
                return provider.promptCreateKey();
            }

            @Override
            <K extends SafeKey<K>> K
            getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                return provider.promptOpenKey(invalid);
            }
        },

        KEY_PROVIDED {
            @Override
            <K extends SafeKey<K>> K
            getCreateKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                return provider.getKey();
            }

            @Override
            <K extends SafeKey<K>> K
            getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                if (invalid) {
                    provider.setState(RESET);
                    return provider.promptOpenKey(invalid);
                } else {
                    return provider.getKey();
                }
            }
        },

        KEY_CHANGE_REQUESTED {
            @Override
            <K extends SafeKey<K>> K getCreateKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                return provider.promptCreateKey();
            }

            @Override
            <K extends SafeKey<K>> K
            getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                if (invalid) {
                    provider.setState(RESET);
                    return provider.promptOpenKey(invalid);
                } else {
                    return provider.getKey();
                }
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
            <K extends SafeKey<K>> void resetUnknownKey(PromptingKeyProvider<K> provider) {
                provider.reset();
            }
        };

        abstract <K extends SafeKey<K>> K
        getCreateKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> K
        getOpenKey(PromptingKeyProvider<K> provider, boolean invalid)
        throws UnknownKeyException;

        <K extends SafeKey<K>> void
        resetUnknownKey(PromptingKeyProvider<K> provider) {
        }
    }
}
