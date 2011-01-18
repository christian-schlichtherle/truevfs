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
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides the base functionality required to implement a "safe" key provider.
 * Each instance of this class maintains a single key which can be of any
 * run time type (it is just required to be {@link Cloneable}).
 * A clone of this key is returned on each call to {@link #getCreateKey}
 * and {@link #getOpenKey}.
 *
 * @param   <K> The type of the keys.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@ThreadSafe
public class SafeKeyProvider<K extends SafeKey<K>>
implements KeyProvider<K> {

    /**
     * The minimum delay between subsequent attempts to verify a key in
     * milliseconds.
     * More specifically, this is the minimum delay between two calls to
     * {@link #getOpenKey} by the same thread.
     */
    public static final int MIN_KEY_RETRY_DELAY = 3 * 1000;

    private final ThreadLocal<Long> invalidated = new ThreadLocal<Long>() {
        @Override
        public Long initialValue() {
            return 0L;
        }
    };

    protected SafeKeyProvider() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #getCreateKeyImpl}.
     *
     * @throws UnknownKeyException If {@code getCreateKeyImpl} throws
     *         this exception or the returned key is {@code null}.
     */
    @Override
    public final K getCreateKey() throws UnknownKeyException {
        final K key = getCreateKeyImpl();
        if (null == key)
            throw new UnknownKeyException();
        return clone(key);
    }

    /**
     * Returns the key which should be used to create a new protected
     * resource or entirely replace the contents of an already existing
     * protected resource.
     * <p>
     * Consecutive calls to this method may return the same object.
     *
     * @return A template for the {@code key} to clone or {@code null}.
     * @throws UnknownKeyException if the required key is unknown for some
     *         reason, e.g. if prompting for the key has been disabled or
     *         cancelled by the user.
     * @see #getCreateKey
     */
    protected @CheckForNull K getCreateKeyImpl() throws UnknownKeyException {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link SafeKeyProvider} forwards the call to
     * {@link #getOpenKeyImpl} and enforces a three seconds suspension penalty
     * if {@code invalid} is {@code true} before returning.
     * Because this method is final, this qualifies the implementation in
     * this class as a "friendly" {@code KeyProvider} implementation,
     * even when subclassed.
     *
     * @throws UnknownKeyException If {@code getOpenKeyImpl} throws
     *         this exception or the returned key is {@code null}.
     */
    @Override
    public final K getOpenKey(boolean invalid) throws UnknownKeyException {
        if (invalid)
            invalidated.set(System.currentTimeMillis());
        try {
            final K key = getOpenKeyImpl(invalid);
            if (null == key)
                throw new UnknownKeyException();
            return clone(key);
        } finally {
            enforceSuspensionPenalty();
        }
    }

    /**
     * Returns the key which should be used to open an existing protected
     * resource in order to access its contents.
     *
     * @return A template for the {@code key} to use or {@code null}.
     * @throws UnknownKeyException If the required key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see KeyProvider#getCreateKey
     */
    protected @CheckForNull K getOpenKeyImpl(boolean invalid)
    throws UnknownKeyException {
        return null;
    }

    /**
     * Returns a clone of the given key.
     *
     * @return A clone of the given key.
     */
    protected @Nullable K clone(@CheckForNull K key) {
        return null == key ? null : key.clone();
    }

    /**
     * Resets the given key.
     *
     * @param key the key to reset.
     */
    protected void reset(@CheckForNull K key) {
        if (null != key)
            key.reset();
    }

    @SuppressWarnings("SleepWhileHoldingLock")
    private void enforceSuspensionPenalty() {
        final long last = invalidated.get();
        long delay;
        InterruptedException interrupted = null;
        while ((delay = System.currentTimeMillis() - last) < MIN_KEY_RETRY_DELAY) {
            try {
                Thread.sleep(MIN_KEY_RETRY_DELAY - delay);
            } catch (InterruptedException ex) {
                interrupted = ex;
            }
        }
        if (interrupted != null)
            Thread.currentThread().interrupt();
    }
}
