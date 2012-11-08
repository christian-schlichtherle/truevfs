/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.util.SuspensionPenalty;

/**
 * Provides the base functionality required to implement a "safe" key provider.
 * Each instance of this class maintains a single instance of the interface
 * {@link SafeKey}).
 * A clone of this key is returned on each call to {@link #getKeyForWriting}
 * and {@link #getKeyForReading}.
 *
 * @param  <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class SafeKeyProvider<K extends SafeKey<K>>
extends AbstractKeyProvider<K> {

    /**
     * The minimum delay between subsequent attempts to verify a key in
     * milliseconds.
     * More specifically, this is the minimum delay between two calls to
     * {@link #getKeyForReading} by the same thread if the parameter {@code invalid}
     * is {@code true}.
     */
    public static final int MIN_KEY_RETRY_DELAY = SuspensionPenalty.MIN_KEY_RETRY_DELAY;

    private volatile @CheckForNull K key;

    private final ThreadLocal<Long> invalidated = new ThreadLocal<>();

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #setupKeyForWriting}.
     *
     * @throws UnknownKeyException If {@code setupKeyForWriting} throws
     *         this exception or the key is still {@code null}.
     */
    @Override
    public final K getKeyForWriting() throws UnknownKeyException {
        setupKeyForWriting();
        return getNonNullKey();
    }

    /**
     * Retrieves the key for writing a protected resource.
     * <p>
     * Subsequent calls to this method may return the same object.
     *
     * @throws UnknownKeyException If the key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getKeyForWriting
     */
    protected abstract void setupKeyForWriting()
    throws UnknownKeyException;

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #setupKeyForReading} and enforces a three seconds suspension penalty
     * if {@code invalid} is {@code true} before returning.
     * Because this method is final, this qualifies the implementation in
     * this class as a "safe" {@code KeyProvider} implementation,
     * even when subclassed.
     *
     * @throws UnknownKeyException If {@code setupKeyForReading} throws
     *         this exception or the key is still {@code null}.
     */
    @Override
    public final K getKeyForReading(boolean invalid) throws UnknownKeyException {
        if (invalid) invalidated.set(System.currentTimeMillis());
        try {
            setupKeyForReading(invalid);
        } finally {
            final Long invalidated = this.invalidated.get();
            SuspensionPenalty.enforce(null == invalidated ? 0 : invalidated);
        }
        return getNonNullKey();
    }

    /**
     * Retrieves the key for reading a protected resource.
     * <p>
     * Subsequent calls to this method may return the same object.
     *
     * @throws UnknownKeyException If the key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getKeyForReading
     */
    protected abstract void setupKeyForReading(boolean invalid)
    throws UnknownKeyException;

    private K getNonNullKey() throws UnknownKeyException {
        final K key = getKey();
        if (null == key) throw new UnknownKeyException();
        return key;
    }

    protected @CheckForNull K getKey() {
        final K key = this.key;
        return null == key ? null : key.clone();
    }

    @Override
    public void setKey(final @CheckForNull K key) {
        final K old = this.key;
        this.key = null == key ? null : key.clone();
        if (null != old) old.reset();
    }
}
