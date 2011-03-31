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
 * The user is prompted via an instance of the {@link View} interface which
 * is {@link #setView injected} to this instance by a {@link PromptingKeyManager}.
 * The view may then display the resource URI by calling {@link #getResource}
 * on this instance (which is also {@link #setResource injected} by a
 * {@link PromptingKeyManager}) and finally set the key by using the given
 * {@link Controller}.
 *
 * @param   <K> The type of the keys.
 * @see     PromptingKeyManager
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class PromptingKeyProvider<K extends SafeKey<K>>
extends SafeKeyProvider<K> {

    /** The resource identifier for the protected resource. */
    private volatile @CheckForNull URI resource;

    /** The view instance which is used to prompt the user for a key. */
    private volatile @CheckForNull View<K> view;

    private volatile @NonNull State state = RESET;

    private volatile @CheckForNull K key;

    private volatile boolean changeRequested;

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

    final View<K> getView() {
        return view;
    }

    final void setView(final View<K> view) {
        this.view = view;
    }

    private @NonNull State getState() {
        return state;
    }

    private void setState(final @NonNull State state) {
        this.state = state;
    }

    @Override
    protected final K getWriteKeyImpl() throws UnknownKeyException {
        return getState().getWriteKey(this);
    }

    @Override
    protected final K getReadKeyImpl(boolean invalid)
    throws UnknownKeyException {
        return getState().getReadKey(this, invalid);
    }

    private @CheckForNull K getKey() {
        return key;
    }

    @Override
    public void setKey(final @CheckForNull K newKey) {
        // This is quite paranoid, but supposedly fairly safe.
        final K oldKey = this.key;
        this.key = clone(newKey);
        setState(null != newKey ? PROVIDED : CANCELLED);
        reset(oldKey);
        //reset(newKey); // don't be mean!
    }

    /**
     * Returns whether or not the user shall get prompted for a new key upon
     * the next call to {@link #getWriteKey()}, provided that the key
     * has been {@link #setKey set} before.
     *
     * @return Whether or not the user shall get prompted for a new key upon
     *         the next call to {@link #getWriteKey()}, provided that the key
     *         has been {@link #setKey set} before.
     */
    private boolean isChangeRequested() {
        return changeRequested;
    }

    private void setChangeRequested(final boolean changeRequested) {
        this.changeRequested = changeRequested;
    }

    /**
     * Resets the state of this key provider, its current key and the value of
     * its {@code changeRequested} property
     * if and only if prompting for a key has been cancelled.
     */
    public void resetCancelledKey() {
        getState().resetCancelledKey(this);
    }

    /**
     * Resets the state of this key provider, its current key and the value of
     * its {@code changeRequested} property
     * unconditionally.
     */
    public void resetUnconditionally() {
        reset();
    }

    private void reset() {
        setKey(null);
        setChangeRequested(false);
        setState(RESET);
    }

    /** Implements the behavior strategy of its enclosing class. */
    @ThreadSafe
    @DefaultAnnotation(NonNull.class)
    enum State {
        RESET {
            @Override
            <K extends SafeKey<K>> K
            getWriteKey(final PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                State state;
                try {
                    Controller<K> controller = new WriteController<K>(provider, this);
                    provider.getView().promptWriteKey(controller);
                    controller.invalidate();
                } finally {
                    if ((state = provider.getState()) == this)
                        provider.setState(state = CANCELLED);
                }
                return state.getWriteKey(provider);
            }

            @Override
            <K extends SafeKey<K>> K
            getReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                State state;
                do {
                    try {
                        Controller<K> controller = new ReadController<K>(provider, this);
                        provider.getView().promptReadKey(controller, invalid);
                        controller.invalidate();
                    } catch (KeyPromptingCancelledException ex) {
                        provider.setState(CANCELLED);
                        throw ex;
                    }
                    state = provider.getState();
                } while (state == this);
                return state.getReadKey(provider, false);
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        PROVIDED {
            @Override
            <K extends SafeKey<K>> K
            getWriteKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                if (provider.isChangeRequested()) {
                    provider.setChangeRequested(false);
                    return RESET.getWriteKey(provider); // DON'T change state!
                } else {
                    return provider.getKey();
                }
            }

            @Override
            <K extends SafeKey<K>> K
            getReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                if (invalid) {
                    provider.setState(RESET);
                    return RESET.getReadKey(provider, true);
                } else {
                    return provider.getKey();
                }
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        CANCELLED {
            @Override
            <K extends SafeKey<K>> K
            getWriteKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                throw new KeyPromptingCancelledException();
            }

            @Override
            <K extends SafeKey<K>> K
            getReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                throw new KeyPromptingCancelledException();
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
                provider.reset();
            }
        };

        abstract <K extends SafeKey<K>> K
        getWriteKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> K
        getReadKey(PromptingKeyProvider<K> provider, boolean invalid)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> void
        resetCancelledKey(PromptingKeyProvider<K> provider);

        @CheckForNull <K extends SafeKey<K>> K
        getKey(PromptingKeyProvider<K> provider) {
            return provider.getKey();
        }

        final <K extends SafeKey<K>> void
        setKey(PromptingKeyProvider<K> provider, K key) {
            provider.setKey(key);
        }

        <K extends SafeKey<K>> void
        setChangeRequested(PromptingKeyProvider<K> provider, boolean changeRequested) {
            provider.setChangeRequested(changeRequested);
        }

        <K extends SafeKey<K>> URI
        getResource(PromptingKeyProvider<K> provider) {
            return provider.getResource();
        }
    } // enum State

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
     * {@link PromptingKeyProvider#getWriteKey()} or
     * {@link PromptingKeyProvider#getReadKey(boolean)} is called.
     * <p>
     * Implementations of this interface <em>must</em> be thread safe
     * and should have no side effects!
     */
    @DefaultAnnotation(NonNull.class)
    public interface View<K extends SafeKey<K>> {

        /**
         * Prompts the user for the key for (over)writing the contents of a
         * new or existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link Controller#setKey key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called {@link Controller#setKey} with a
         * non-{@code null} parameter, then a clone of this object will be
         * used as the key.
         * <p>
         * Otherwise, prompting for a key is permanently disabled and each
         * subsequent call to {@link #getWriteKey} or {@link #getReadKey}
         * results in a {@link KeyPromptingCancelledException} until
         * {@link #resetCancelledKey()} or {@link #resetUnconditionally()} gets
         * called.
         *
         * @param  controller The key controller for storing the result.
         * @throws UnknownKeyException if key prompting fails for any reason.
         */
        void promptWriteKey(Controller<K> controller)
        throws UnknownKeyException;

        /**
         * Prompts the user for the key for reading the contents of an
         * existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link Controller#setKey key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called {@link Controller#setKey} with a
         * non-{@code null} parameter, then a clone of this object will be
         * used as the key.
         * <p>
         * Otherwise, if the implementation has called {@link Controller#setKey}
         * with a {@code null} parameter or throws a
         * {@link KeyPromptingCancelledException}, then prompting for the key
         * is permanently disabled and each subsequent call to
         * {@link #getWriteKey} or {@link #getReadKey} results in a
         * {@link KeyPromptingCancelledException} until
         * {@link #resetCancelledKey()} or {@link #resetUnconditionally()} gets
         * called.
         * <p>
         * Otherwise, the state of the key provider is not changed and this
         * method gets called again.
         *
         * @param  controller The key controller for storing the result.
         * @param  invalid {@code true} iff a previous call to this method
         *         resulted in an invalid key.
         * @throws KeyPromptingCancelledException if key prompting has been
         *         cancelled by the user.
         * @throws UnknownKeyException if key prompting fails for any other
         *         reason.
         */
        void promptReadKey(Controller<K> controller, boolean invalid)
        throws UnknownKeyException;
    } // interface View

    /** Proxies access to the key for {@link View} implementations. */
    @ThreadSafe
    @DefaultAnnotation(NonNull.class)
    public static class Controller<K extends SafeKey<K>> {
        private final PromptingKeyProvider<K> provider;
        private @CheckForNull State state;

        private Controller( final PromptingKeyProvider<K> provider,
                            final State state) {
            this.provider = provider;
            this.state = state;
        }

        /**
         * Returns the unique resource identifier (resource ID) of the
         * protected resource for which this controller is used.
         *
         * @throws IllegalStateException if getting this property is not legal
         *         in the current state.
         */
        public URI getResource() {
            if (null == state)
                throw new IllegalStateException();
            return state.getResource(provider);
        }

        /**
         * Returns the protected resource's key.
         *
         * @return The protected resource's key.
         * @throws IllegalStateException if getting key is not legal in the
         *         current state.
         */
        public @CheckForNull K getKey() {
            if (null == state)
                throw new IllegalStateException();
            return state.getKey(provider);
        }

        /**
         * Sets the protected resource's key to a clone of the given key.
         *
         * @param  key The key to clone to use for the protected resource.
         * @throws IllegalStateException if setting key is not legal in the
         *         current state.
         */
        public void setKey(@CheckForNull K key) {
            if (null == state)
                throw new IllegalStateException();
            state.setKey(provider, key);
        }

        /**
         * Requests to prompt the user for a new key upon the next call to
         * {@link #getWriteKey()}, provided that the key is
         * {@link #setKey set} by then.
         *
         * @param  changeRequested whether or not the user shall get prompted
         *         for a new key upon the next call to {@link #getWriteKey()},
         *         provided that the key is {@link #setKey set} then.
         * @throws IllegalStateException if setting this property is not legal
         *         in the current state.
         */
        public void setChangeRequested(boolean changeRequested) {
            if (null == state)
                throw new IllegalStateException();
            state.setChangeRequested(provider, changeRequested);
        }

        private void invalidate() {
            state = null;
        }
    } // class Controller

    private static class WriteController<K extends SafeKey<K>>
    extends Controller<K> {
        private WriteController(PromptingKeyProvider<K> provider, State state) {
            super(provider, state);
        }

        @Override
        public void setChangeRequested(boolean changeRequested) {
            throw new IllegalStateException();
        }
    } // class WriteKeyController

    private static class ReadController<K extends SafeKey<K>>
    extends Controller<K> {
        private ReadController(PromptingKeyProvider<K> provider, State state) {
            super(provider, state);
        }

        @Override
        public K getKey() {
            throw new IllegalStateException();
        }
    } // class WriteKeyController
}
