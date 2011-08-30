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

import de.schlichtherle.truezip.crypto.SuspensionPenalty;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides the base functionality required to implement a "safe" key provider.
 * Each instance of this class maintains a single instance of the interface
 * {@link SafeKey}).
 * A clone of this key is returned on each call to {@link #getWriteKey}
 * and {@link #getReadKey}.
 *
 * @param   <K> The type of the keys.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class SafeKeyProvider<K extends SafeKey<K>>
implements KeyProvider<K> {

    /**
     * The minimum delay between subsequent attempts to verify a key in
     * milliseconds.
     * More specifically, this is the minimum delay between two calls to
     * {@link #getReadKey} by the same thread.
     */
    public static final int MIN_KEY_RETRY_DELAY = SuspensionPenalty.MIN_KEY_RETRY_DELAY;

    private volatile @CheckForNull K key;

    private final ThreadLocal<Long> invalidated = new ThreadLocalLong();

    /**
     * Constructs a new safe key provider.
     */
    protected SafeKeyProvider() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #retrieveWriteKey}.
     *
     * @throws UnknownKeyException If {@code retrieveWriteKey} throws
     *         this exception or the secret key is still {@code null}.
     */
    @Override
    public final K getWriteKey() throws UnknownKeyException {
        retrieveWriteKey();
        return getNonNullKey();
    }

    /**
     * Retrieves the secret key for the encryption of a protected resource.
     * <p>
     * Subsequent calls to this method may return the same object.
     *
     * @throws UnknownKeyException If the secret key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getWriteKey
     */
    protected abstract void retrieveWriteKey()
    throws UnknownKeyException;

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #retrieveReadKey} and enforces a three seconds suspension penalty
     * if {@code invalid} is {@code true} before returning.
     * Because this method is final, this qualifies the implementation in
     * this class as a "safe" {@code KeyProvider} implementation,
     * even when subclassed.
     *
     * @throws UnknownKeyException If {@code retrieveReadKey} throws
     *         this exception or the secret key is still {@code null}.
     */
    @Override
    public final K getReadKey(boolean invalid) throws UnknownKeyException {
        if (invalid)
            invalidated.set(System.currentTimeMillis());
        try {
            retrieveReadKey(invalid);
        } finally {
            enforceSuspensionPenalty();
        }
        return getNonNullKey();
    }

    /**
     * Retrieves the secret key for the decryption of a protected resource.
     * <p>
     * Subsequent calls to this method may return the same object.
     *
     * @throws UnknownKeyException If the secret key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see #getReadKey
     */
    protected abstract void retrieveReadKey(boolean invalid)
    throws UnknownKeyException;

    private K getNonNullKey() throws UnknownKeyException {
        final K key = getKey();
        if (null == key)
            throw new UnknownKeyException();
        return key;
    }

    protected @CheckForNull K getKey() {
        final K key = this.key;
        return null == key ? null : key.clone();
    }

    @Override
    public void setKey(final @CheckForNull K newKey) {
        final K oldKey = this.key;
        this.key = null == newKey ? null : newKey.clone();
        if (null != oldKey)
            oldKey.reset();
    }

    private void enforceSuspensionPenalty() {
        // FIXME: This makes this class untestable!
        //invalidated.set(
                SuspensionPenalty.enforce(invalidated.get());//);
    }

    private static final class ThreadLocalLong extends ThreadLocal<Long> {
        @Override
        public Long initialValue() {
            return 0L;
        }
    } // ThreadLocalLong
}
