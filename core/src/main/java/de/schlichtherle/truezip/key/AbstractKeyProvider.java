/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import java.lang.reflect.Array;
import java.net.URI;

/**
 * This abstract class implements the base functionality required to be a
 * "friendly" {@link KeyProvider}.
 * Each instance of this class maintains a single key, which can be of any
 * run time type (it is just required to be an {@link Object}).
 * A clone of this key is returned on each call to {@link #getCreateKey}
 * and {@link #getOpenKey}.
 * Cloning is used for all array classes and all classes which properly
 * implement the {@link Cloneable} interface.
 * The class remains abstract because there is no meaningful template
 * implementation of the {@link #invalidOpenKeyImpl()} method.
 * <p>
 * Other than the key, this class is stateless.
 * Hence, instances may be shared among multiple protected resources,
 * causing them to use the same key.
 * However, this feature may be restricted by subclasses such as
 * {@link PromptingKeyProvider} for example.
 * <p>
 * This class is thread safe.
 *
 * @see KeyProvider
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class AbstractKeyProvider<K extends Cloneable>
        implements KeyProvider<K> {

    private final ThreadLocal<Long> invalidated = new ThreadLocal<Long>() {
        @Override
        public Long initialValue() {
            return 0L;
        }
    };

    /**
     * Forwards the call to {@link #getCreateKeyImpl}.
     *
     * @return A clone of the return value of {@code getCreateKeyImpl}.
     *         In case of an array, a shallow copy of the array is returned.
     * @throws UnknownKeyException If {@code getCreateKeyImpl} throws
     *         this or the returned key is {@code null}.
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     * @see KeyProvider#getCreateKey
     */
    public final K getCreateKey() throws UnknownKeyException {
        final K key = getCreateKeyImpl();
        if (key == null)
            throw new UnknownKeyException();
        return clone(key);
    }

    /**
     * Returns the key which should be used to create a new protected
     * resource or entirely replace the contents of an already existing
     * protected resource.
     *
     * @return A template for the {@code key} to use or {@code null}.
     * @throws UnknownKeyException If the required key is unknown.
     *         At the subclasses discretion, this may mean that prompting for
     *         the key has been disabled or cancelled by the user.
     * @see KeyProvider#getCreateKey
     */
    protected K getCreateKeyImpl() throws UnknownKeyException {
        return null;
    }

    /**
     * Forwards the call to {@link #getOpenKeyImpl} and enforces a three
     * seconds suspension penalty if {@link #invalidOpenKey} was called by
     * the same thread before.
     * Because this method is final, this qualifies the implementation in
     * this class as a "friendly" {@code KeyProvider} implementation,
     * even when subclassed.
     *
     * @return A clone of the return value of {@code getOpenKeyImpl}.
     *         In case of an array, a shallow copy of the array is returned.
     * @throws UnknownKeyException If {@code getOpenKeyImpl} throws
     *         this or the returned key is {@code null}.
     * @throws RuntimeException If cloning the key results in a runtime
     *         exception.
     * @see KeyProvider#getOpenKey
     */
    public final K getOpenKey() throws UnknownKeyException {
        try {
            final K key = getOpenKeyImpl();
            if (key == null)
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
    protected K getOpenKeyImpl() throws UnknownKeyException {
        return null;
    }

    /**
     * This method logs the current time for the current thread, which
     * is later used by {@link #getOpenKey} to enforce the suspension penalty
     * and then calls {@link #invalidOpenKeyImpl}.
     * Because this method is final, this implementation qualifies as a
     * "friendly" {@code KeyProvider} implementation, even when subclassed.
     *
     * @see KeyProvider#invalidOpenKey
     */
    public final void invalidOpenKey() {
        invalidated.set(System.currentTimeMillis());
        invalidOpenKeyImpl();
    }

    /**
     * Sublasses must implement this method.
     *
     * @see KeyProvider#invalidOpenKey
     */
    protected abstract void invalidOpenKeyImpl();

    /**
     * This hook may be overridden to reset this key provider instance.
     * The implementation in this class does nothing.
     */
    public void reset() {
        // Do NOT call this - it limits the reusability!
        //resetKey();
    }

    /**
     * Clones {@code key} reflectively.
     * If the key is an array, a shallow copy of the array is returned.
     *
     * @param key The key to get cloned - may be {@code null}.
     * @return A clone of the {@code key} property, which may be
     *         {@code null}.
     * @throws RuntimeException If cloning the key results in an exception.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    static <K> K clone(final K key) {
        // Could somebody please explain to me why the clone method is
        // declared "protected" in Object and Cloneable is just a marker
        // interface?
        // And furthermore, why does clone() called via reflection on an
        // array throw a NoSuchMethodException?
        // Somehow, this design doesn't speak to me...
        final Class<?> c = key.getClass();
        if (c.isArray()) {
            final int l = Array.getLength(key);
            final K clone = (K) Array.newInstance(c.getComponentType(), l);
            System.arraycopy(key, 0, clone, 0, l);
            return clone;
        } else {
            try {
                return (K) c.getMethod("clone", (Class[]) null)
                        .invoke(key, (Object[]) null);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
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

    /**
     * Maps this instance as the key provider for the given resource
     * identifier in the {@link KeyManager}.
     * <p>
     * The key manager will use this method whenever it adds a key provider
     * which is actually an instance of this class.
     * This allows subclasses to add additional behaviour or constraints
     * whenever an instance is mapped in the {@code KeyManager}.
     *
     * @param resource The resource identifier to map this instance for.
     * @return The key provider previously mapped for the given resource
     *         identifier or {@code null} if no key provider was mapped.
     * @throws NullPointerException If {@code resourceID} is
     *         {@code null}.
     * @throws IllegalStateException If mapping this instance is prohibited
     *         by a constraint in a subclass.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    protected KeyProvider<?> addToKeyManager(URI resource) {
        return KeyManager.mapKeyProvider(resource, this);
    }

    /**
     * Remove this instance as the key provider for the given resource
     * identifier from the map in the {@link KeyManager}.
     * <p>
     * The key manager will use this method whenever it adds a key provider
     * which is actually an instance of this class.
     * This allows subclasses to add additional behaviour or constraints
     * whenever an instance is unmapped in the {@code KeyManager}.
     *
     * @param resource The resource identifier to unmap this instance from.
     * @return The key provider previously mapped for the given resource
     *         identifier.
     * @throws NullPointerException If {@code resourceID} is
     *         {@code null}.
     * @throws IllegalStateException If unmapping this instance is prohibited
     *         by a constraint in a subclass.
     *         Please refer to the respective subclass documentation for
     *         more information about its constraint(s).
     */
    protected KeyProvider<?> removeFromKeyManager(URI resource) {
        return KeyManager.mapKeyProvider(resource, null);
    }
}
