/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.io.Closeable;
import java.net.URI;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A "safe" key provider which prompts the user for a key for its protected
 * resource.
 * The user is prompted via an instance of the {@link View}
 * interface.
 * The view may then display the resource URI by calling {@link #getResource}
 * on this instance and set the key by using the given
 * {@link Controller}.
 *
 * @param  <K> the type of the prompting keys.
 * @see    PromptingKeyManager
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class PromptingKeyProvider<K extends PromptingKey<K>>
extends SafeKeyProvider<K> {

    private final View<K> view;

    private volatile State state = State.RESET;

    /** The resource identifier for the protected resource. */
    private volatile @CheckForNull URI resource;

    private volatile boolean changeRequested;

    private volatile @CheckForNull PersistentUnknownKeyException exception;

    PromptingKeyProvider(PromptingKeyManager<K> manager) {
        this.view = manager.getView();
    }

    private View<K> getView() { return view; }

    private State getState() { return state; }

    private void setState(final State state) {
        assert null != state;
        this.state = state;
    }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    public @CheckForNull URI getResource() { return resource; }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    void setResource(final @CheckForNull URI resource) {
        this.resource = resource;
    }

    @Override
    protected void setupKeyForWriting() throws UnknownKeyException {
        getState().setupKeyForWriting(this);
    }

    @Override
    protected void setupKeyForReading(boolean invalid) throws UnknownKeyException {
        getState().setupKeyForReading(this, invalid);
    }

    @Override
    protected @CheckForNull K getKey() { return getState().getKey(this); }

    private @CheckForNull K getKey0() { return super.getKey(); }

    @Override
    public void setKey(final @CheckForNull K key) {
        getState().setKey(this, key);
    }

    private void setKey0(final @CheckForNull K key) {
        super.setKey(key);
    }

    private @CheckForNull PersistentUnknownKeyException getException() {
        return exception;
    }

    private void setException(
            final @CheckForNull PersistentUnknownKeyException exception) {
        this.exception = exception;
    }

    /**
     * Resets the state of this key provider
     * if and only if prompting for a key has been cancelled.
     */
    public void resetCancelledKey() { getState().resetCancelledKey(this); }

    /**
     * Resets the state of this key provider
     * unconditionally.
     */
    public void resetUnconditionally() { reset(); }

    private void reset() {
        setKey0(null);
        setException(null);
        setState(State.RESET);
    }

    /** Implements the behavior strategy of its enclosing class. */
    @ThreadSafe
    private enum State {
        RESET {
            @Override
            <K extends PromptingKey<K>> void
            setupKeyForWriting(final PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                State state;
                try {
                    try (final PromptingKeyProvider<K>.BaseController
                            controller = provider.new WriteController(this)) {
                        provider.getView().promptKeyForWriting(controller);
                    }
                } finally {
                    if (this == (state = provider.getState()))
                        provider.setState(state = CANCELLED);
                }
                state.setupKeyForWriting(provider);
            }

            @Override
            <K extends PromptingKey<K>> void
            setupKeyForReading(
                    final PromptingKeyProvider<K> provider,
                    final boolean invalid)
            throws UnknownKeyException {
                State state;
                do {
                    try {
                        try (final PromptingKeyProvider<K>.BaseController
                                controller = provider.new ReadController(this)) {
                            provider.getView().promptKeyForReading(controller, invalid);
                        }
                    } catch (final PersistentUnknownKeyException ex) {
                        setException(provider, ex);
                    }
                    state = provider.getState();
                } while (state == this);
                state.setupKeyForReading(provider, false);
            }

            @Override
            <K extends PromptingKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        SET {
            @Override
            <K extends PromptingKey<K>> void
            setupKeyForWriting(final PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                final K key = getKey(provider);
                if (key.isChangeRequested()) {
                    key.setChangeRequested(false);
                    setKey(provider, key);
                    RESET.setupKeyForWriting(provider); // DON'T change state!
                }
            }

            @Override
            <K extends PromptingKey<K>> void
            setupKeyForReading(
                    final PromptingKeyProvider<K> provider,
                    final boolean invalid)
            throws UnknownKeyException {
                if (invalid) {
                    provider.setState(RESET);
                    RESET.setupKeyForReading(provider, true);
                }
            }

            @Override
            <K extends PromptingKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
            }
        },

        CANCELLED {
            @Override
            <K extends PromptingKey<K>> void
            setupKeyForWriting(PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                throw exception(provider);
            }

            @Override
            <K extends PromptingKey<K>> void
            setupKeyForReading(PromptingKeyProvider<K> provider, boolean invalid)
            throws UnknownKeyException {
                throw exception(provider);
            }

            @Override
            <K extends PromptingKey<K>> void
            resetCancelledKey(PromptingKeyProvider<K> provider) {
                provider.reset();
            }
        };

        abstract <K extends PromptingKey<K>> void
        setupKeyForWriting(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends PromptingKey<K>> void
        setupKeyForReading(PromptingKeyProvider<K> provider, boolean invalid)
        throws UnknownKeyException;

        abstract <K extends PromptingKey<K>> void
        resetCancelledKey(PromptingKeyProvider<K> provider);

        final <K extends PromptingKey<K>> URI
        getResource(PromptingKeyProvider<K> provider) {
            final URI resource = provider.getResource();
            if (null == resource) throw new IllegalStateException();
            return resource;
        }

        final @CheckForNull <K extends PromptingKey<K>> K
        getKey(PromptingKeyProvider<K> provider) {
            return provider.getKey0();
        }

        final <K extends PromptingKey<K>> void
        setKey(PromptingKeyProvider<K> provider, @CheckForNull K key) {
            provider.setKey0(key);
            provider.setState(null != key ? State.SET : State.CANCELLED);
        }

        final <K extends PromptingKey<K>> PersistentUnknownKeyException
        exception(PromptingKeyProvider<K> provider) {
            PersistentUnknownKeyException ex = provider.getException();
            if (null == ex)
                provider.setException(ex = new KeyPromptingCancelledException());
            return ex;
        }

        final <K extends PromptingKey<K>> void
        setException(PromptingKeyProvider<K> provider, PersistentUnknownKeyException ex) {
            provider.setException(ex);
            provider.setState(CANCELLED);
        }
    } // State

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
     * Implementations must be safe for multi-threading!
     *
     * @param  <K> the type of the safe keys.
     * @author Christian Schlichtherle
     */
    public interface View<K extends PromptingKey<K>> {

        /**
         * Prompts the user for the key for (over)writing the contents of a
         * new or existing protected resource.
         * Upon return, the implementation should have updated the
         * {@link Controller#setKey key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called
         * {@link Controller#setKey} with a non-{@code null}
         * parameter, then a clone of this object will be used as the key.
         * <p>
         * Otherwise, prompting for a key is permanently disabled and each
         * subsequent call to {@link PromptingKeyProvider#promptKeyForWriting} or
         * {@link PromptingKeyProvider#promptKeyForReading}
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
         * {@link Controller#setKey key} property of the given
         * {@code controller}.
         * <p>
         * If the implementation has called
         * {@link Controller#setKey} with a non-{@code null}
         * parameter, then a clone of this object will be used as the key.
         * <p>
         * Otherwise, if the implementation has called
         * {@link Controller#setKey} with a {@code null}
         * parameter or throws a {@link KeyPromptingCancelledException}, then
         * prompting for the key is permanently disabled and each subsequent call
         * to {@link PromptingKeyProvider#promptKeyForWriting} or
         * {@link PromptingKeyProvider#promptKeyForReading} results in a
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
    } // View

    /**
     * Proxies access to the key for {@link View} implementations.
     *
     * @param  <K> the type of the safe keys.
     * @author Christian Schlichtherle
     */
    @NotThreadSafe
    public interface Controller<K extends PromptingKey<K>> {

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
        @CheckForNull K getKey();

        /**
         * Sets the protected resource's key to a clone of the given key or
         * resets it to null.
         *
         * @param  key The nullable key to clone and use for accessing the
         *         protected resource.
         * @throws IllegalStateException if setting key is not legal in the
         *         current state.
         */
        void setKey(@CheckForNull K key);
    } // Controller

    /**
     * Proxies access to the key for {@link View} implementations.
     */
    @NotThreadSafe
    private abstract class BaseController implements Controller<K>, Closeable {

        private @CheckForNull State state;

        BaseController(final State state) { this.state = state; }

        private State getState() {
            final State state = this.state;
            if (null == state) throw new IllegalStateException();
            return state;
        }

        @Override
        public final void close() { this.state = null; }

        @Override
        public final URI getResource() {
            return getState().getResource(PromptingKeyProvider.this);
        }

        @Override
        public final void setKey(@CheckForNull K key) {
            getState().setKey(PromptingKeyProvider.this, key);
        }
    } // BaseController

    /**
     * The controller to use when promting for a key to write a protected
     * resource.
     */
    @NotThreadSafe
    private final class WriteController extends BaseController {

        WriteController(State state) { super(state); }

        @Override
        public K getKey() {
            return getState().getKey(PromptingKeyProvider.this);
        }
    } // WriteController

    /**
     * The controller to use when promting for a key to read a protected
     * resource.
     */
    @NotThreadSafe
    private final class ReadController extends BaseController {

        ReadController(State state) { super(state); }

        @Override
        public K getKey() { throw new IllegalStateException(); }
    } // ReadController
}
