/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.impl;

import de.truezip.key.*;
import java.io.Closeable;
import java.net.URI;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A "safe" key provider which prompts the user for a key for its protected
 * resource.
 * The user is prompted via an instance of the {@link View} interface.
 * The view may then display the resource URI by calling {@link #getResource}
 * on this instance and set the key by using the given {@link Controller}.
 *
 * @param   <K> The type of the keys.
 * @see     PromptingKeyManager
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class PromptingKeyProvider<K extends SafeKey<K>>
extends SafeKeyProvider<K> {

    private final PromptingKeyProviderView<K> view;

    private volatile State state = State.RESET;

    /** The resource identifier for the protected resource. */
    private volatile @CheckForNull URI resource;

    private volatile boolean changeRequested;

    private volatile @CheckForNull CacheableUnknownKeyException exception;

    PromptingKeyProvider(PromptingKeyManager<K> manager) {
        this.view = manager.getView();
    }

    private PromptingKeyProviderView<K> getView() {
        return view;
    }

    private State getState() {
        return state;
    }

    private void setState(final State state) {
        assert null != state;
        this.state = state;
    }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    public @CheckForNull URI getResource() {
        return resource;
    }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    void setResource(final @CheckForNull URI resource) {
        this.resource = resource;
    }

    @Override
    protected void retrieveWriteKey() throws UnknownKeyException {
        getState().retrieveWriteKey(this);
    }

    @Override
    protected void retrieveReadKey(boolean invalid)
    throws UnknownKeyException {
        getState().retrieveReadKey(this, invalid);
    }

    @Override
    protected K getKey() {
        return getState().getKey(this);
    }

    private @CheckForNull K getKey0() {
        return super.getKey();
    }

    @Override
    public void setKey(final @CheckForNull K key) {
        getState().setKey(this, key);
    }

    private void setKey0(final @CheckForNull K key) {
        super.setKey(key);
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

    private @CheckForNull CacheableUnknownKeyException getException() {
        return exception;
    }

    private void setException(
            final @CheckForNull CacheableUnknownKeyException exception) {
        this.exception = exception;
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
        setKey0(null);
        setChangeRequested(false);
        setException(null);
        setState(State.RESET);
    }

    /** Implements the behavior strategy of its enclosing class. */
    @ThreadSafe
    private enum State {
        RESET {
            @Override
            <K extends SafeKey<K>> void
            retrieveWriteKey(final PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                State state;
                try {
                    PromptingKeyProvider<K>.BaseController controller
                            = provider.new WriteController(this);
                    try {
                        provider.getView().promptWriteKey(controller);
                    } finally {
                        controller.close();
                    }
                } finally {
                    if (this == (state = provider.getState()))
                        provider.setState(state = CANCELLED);
                }
                state.retrieveWriteKey(provider);
            }

            @Override
            <K extends SafeKey<K>> void
            retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                State state;
                do {
                    try {
                        PromptingKeyProvider<K>.BaseController controller
                                = provider.new ReadController(this);
                        try {
                            provider.getView().promptReadKey(controller, invalid);
                        } finally {
                            controller.close();
                        }
                    } catch (CacheableUnknownKeyException ex) {
                        setException(provider, ex);
                    }
                    state = provider.getState();
                } while (state == this);
                state.retrieveReadKey(provider, false);
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        SET {
            @Override
            <K extends SafeKey<K>> void
            retrieveWriteKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                if (provider.isChangeRequested()) {
                    provider.setChangeRequested(false);
                    RESET.retrieveWriteKey(provider); // DON'T change state!
                }
            }

            @Override
            <K extends SafeKey<K>> void
            retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                if (invalid) {
                    provider.setState(RESET);
                    RESET.retrieveReadKey(provider, true);
                }
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        CANCELLED {
            @Override
            <K extends SafeKey<K>> void
            retrieveWriteKey(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                throw getException(provider);
            }

            @Override
            <K extends SafeKey<K>> void
            retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                throw getException(provider);
            }

            @Override
            <K extends SafeKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
                provider.reset();
            }
        };

        abstract <K extends SafeKey<K>> void
        retrieveWriteKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> void
        retrieveReadKey(PromptingKeyProvider<K> provider, boolean invalid)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> void
        resetCancelledKey(PromptingKeyProvider<K> provider);

        final @CheckForNull <K extends SafeKey<K>> K
        getKey(PromptingKeyProvider<K> provider) {
            return provider.getKey0();
        }

        final <K extends SafeKey<K>> void
        setKey(PromptingKeyProvider<K> provider, @CheckForNull K key) {
            provider.setKey0(key);
            provider.setState(null != key ? State.SET : State.CANCELLED);
        }

        <K extends SafeKey<K>> void
        setChangeRequested(PromptingKeyProvider<K> provider, boolean changeRequested) {
            provider.setChangeRequested(changeRequested);
        }

        @CheckForNull <K extends SafeKey<K>> URI
        getResource(PromptingKeyProvider<K> provider) {
            return provider.getResource();
        }

        final <K extends SafeKey<K>> CacheableUnknownKeyException
        getException(PromptingKeyProvider<K> provider) {
            CacheableUnknownKeyException ex = provider.getException();
            if (null == ex)
                provider.setException(ex = new KeyPromptingCancelledException());
            return ex;
        }
        
        final <K extends SafeKey<K>> void
        setException(PromptingKeyProvider<K> provider, CacheableUnknownKeyException ex) {
            provider.setException(ex);
            provider.setState(CANCELLED);
        }
    } // State

    /** Proxies access to the secret key for {@link View} implementations. */
    @NotThreadSafe
    private abstract class BaseController implements PromptingKeyProviderController<K>, Closeable {
        private @CheckForNull State state;

        BaseController(final State state) {
            this.state = state;
        }

        private State getState() {
            final State state = this.state;
            if (null == state)
                throw new IllegalStateException();
            return state;
        }

        @Override
        public void close() {
            this.state = null;
        }

        @Override
        public URI getResource() {
            final URI resource = getState().getResource(PromptingKeyProvider.this);
            if (null == resource)
                throw new IllegalStateException();
            return resource;
        }

        @Override
        public K getKey() {
            return getState().getKey(PromptingKeyProvider.this);
        }

        @Override
        public void setKey(K key) {
            getState().setKey(PromptingKeyProvider.this, key);
        }

        @Override
        public void setChangeRequested(boolean changeRequested) {
            getState().setChangeRequested(PromptingKeyProvider.this, changeRequested);
        }
    } // BaseController

    /**
     * The controller to use when promting for a secret key to encrypt a
     * protected resource.
     */
    @NotThreadSafe
    private final class WriteController extends BaseController {
        WriteController(State state) {
            super(state);
        }

        @Override
        public void setChangeRequested(boolean changeRequested) {
            throw new IllegalStateException();
        }
    } // WriteController

    /**
     * The controller to use when promting for a secret key to decrypt a
     * protected resource.
     */
    @NotThreadSafe
    private final class ReadController extends BaseController {
        ReadController(State state) {
            super(state);
        }

        @Override
        public K getKey() {
            throw new IllegalStateException();
        }
    } // ReadController
}