/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.prompting;

import net.java.truecommons3.key.spec.PersistentUnknownKeyException;
import net.java.truecommons3.key.spec.UnknownKeyException;
import net.java.truecommons3.key.spec.util.SuspensionPenalty;
import net.java.truecommons3.shed.UniqueObject;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @param  <K> the type of the prompting keys.
 * @see    SharedKeyManager
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SharedKeyProvider<K extends PromptingKey<K>>
extends UniqueObject {

    private final ThreadLocal<Long> invalidated = new ThreadLocal<>();
    private @Nullable K key;
    private @Nullable PersistentUnknownKeyException exception;
    private volatile State state = State.RESET;
    private int count;

    synchronized void link() {
        ++count;
        assert 0 <= count;
    }

    synchronized void unlink() {
        if (0 >= --count)
            resetUnconditionally();
        assert 0 <= count;
    }

    synchronized void release() {
        resetCancelledKey();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SharedKeyProvider} forwards the call to
     * {@link #setupKeyForWriting}.
     *
     * @throws UnknownKeyException If {@code setupKeyForWriting} throws
     *         this exception or the key is still {@code null}.
     */
    K getKeyCloneForWriting(final PromptingKeyProvider<K> provider)
    throws UnknownKeyException {
        setupKeyForWriting(provider);
        return getNonNullKeyClone();
    }

    /**
     * Sets up the key for (over)writing a protected resource.
     *
     * @throws UnknownKeyException If the key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getKeyCloneForWriting
     */
    private void setupKeyForWriting(final PromptingKeyProvider<K> provider)
    throws UnknownKeyException {
        State os, ns = this.state;
        do {
            os = ns;
            os.setupKeyForWriting(provider);
            ns = this.state;
        } while (ns != os);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SharedKeyProvider} forwards the call to
     * {@link #setupKeyForReading} and enforces a three seconds suspension penalty
     * if {@code invalid} is {@code true} before returning.
     * Because this method is final, this qualifies the implementation in
     * this class as a "safe" {@code KeyProvider} implementation,
     * even when subclassed.
     *
     * @throws UnknownKeyException If {@code setupKeyForReading} throws
     *         this exception or the key is still {@code null}.
     */
    K getKeyCloneForReading(
            final PromptingKeyProvider<K> provider,
            final boolean invalid)
    throws UnknownKeyException {
        if (invalid)
            invalidated.set(System.currentTimeMillis());
        try {
            setupKeyForReading(provider, invalid);
        } finally {
            final Long invalidated = this.invalidated.get();
            SuspensionPenalty.enforce(null == invalidated ? 0 : invalidated);
        }
        return getNonNullKeyClone();
    }

    /**
     * Sets up the key for reading a protected resource.
     *
     * @throws UnknownKeyException If the key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getKeyCloneForReading
     */
    private void setupKeyForReading(
            final PromptingKeyProvider<K> provider,
            boolean invalid)
    throws UnknownKeyException {
        State os, ns = this.state;
        do {
            os = ns;
            invalid = os.setupKeyForReading(provider, invalid);
            ns = this.state;
        } while (ns != os);
    }

    private K getNonNullKeyClone() throws UnknownKeyException {
        final K key = getKeyClone();
        if (null == key)
            throw new UnknownKeyException();
        return key;
    }

    synchronized @Nullable K getKeyClone() {
        final K key = this.key;
        return null == key ? null : key.clone();
    }

    synchronized void setKeyClone(final @Nullable K key) {
        final K old = this.key;
        final boolean set = null != key;
        this.key = set ? key.clone() : null;
        this.state = set ? State.SET : State.CANCELLED;
        if (null != old)
            old.reset();
    }

    synchronized @Nullable PersistentUnknownKeyException getException() {
        return exception;
    }

    synchronized void setException(
            final @Nullable PersistentUnknownKeyException exception) {
        if (null != (this.exception = exception))
            setKeyClone(null);
    }

    /**
     * Resets the state of this key provider
     * if and only if prompting for a key has been cancelled.
     */
    void resetCancelledKey() { state.resetCancelledKey(this); }

    synchronized void resetUnconditionally() {
        setKeyClone(null);
        setException(null);
        this.state = State.RESET;
    }

    synchronized boolean isChangeRequested() {
        return null != key && key.isChangeRequested();
    }

    /** Implements the behavior strategy for prompting key providers. */
    private enum State {

        RESET {
            @Override
            <K extends PromptingKey<K>> void setupKeyForWriting(
                    final PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                provider.promptKeyForWriting();
            }

            @Override
            <K extends PromptingKey<K>> boolean setupKeyForReading(
                    final PromptingKeyProvider<K> provider,
                    final boolean invalid)
            throws UnknownKeyException {
                provider.promptKeyForReading(invalid);
                return false;
            }

            @Override
            <K extends PromptingKey<K>> void resetCancelledKey(
                    SharedKeyProvider<K> provider) {
            }
        },

        SET {
            @Override
            <K extends PromptingKey<K>> void setupKeyForWriting(
                    final PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                if (provider.isChangeRequested())
                    provider.resetUnconditionally();
            }

            @Override
            <K extends PromptingKey<K>> boolean setupKeyForReading(
                    final PromptingKeyProvider<K> provider,
                    final boolean invalid)
            throws UnknownKeyException {
                if (invalid)
                    provider.resetUnconditionally();
                return invalid;
            }

            @Override
            <K extends PromptingKey<K>> void resetCancelledKey(
                    SharedKeyProvider<K> provider) {
            }
        },

        CANCELLED {
            @Override
            <K extends PromptingKey<K>> void setupKeyForWriting(
                    PromptingKeyProvider<K> provider)
            throws UnknownKeyException {
                throw exception(provider);
            }

            @Override
            <K extends PromptingKey<K>> boolean setupKeyForReading(
                    PromptingKeyProvider<K> provider,
                    boolean invalid)
            throws UnknownKeyException {
                throw exception(provider);
            }

            <K extends PromptingKey<K>> PersistentUnknownKeyException exception(
                    PromptingKeyProvider<K> provider) {
                PersistentUnknownKeyException ex = provider.getException();
                if (null == ex)
                    provider.setException(ex = new KeyPromptingCancelledException());
                return ex;
            }

            @Override
            <K extends PromptingKey<K>> void resetCancelledKey(
                    SharedKeyProvider<K> provider) {
                provider.resetUnconditionally();
            }
        };

        abstract <K extends PromptingKey<K>> void setupKeyForWriting(
                PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends PromptingKey<K>> boolean setupKeyForReading(
                PromptingKeyProvider<K> provider,
                boolean invalid)
        throws UnknownKeyException;

        abstract <K extends PromptingKey<K>> void resetCancelledKey(
                SharedKeyProvider<K> provider);
    }
}
