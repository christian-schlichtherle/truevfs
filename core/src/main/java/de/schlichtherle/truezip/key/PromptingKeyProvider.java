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

import java.util.Arrays;

/**
 * A "friendly" implementation of {@link KeyProvider} which prompts the user
 * for a key for its protected resource, enforcing a three seconds suspension
 * penalty if a wrong key was provided.
 * The user is prompted via an instance of the {@link PromptingKeyProviderUI}
 * user interface which is determined by the default instance of
 * {@link PromptingKeyManager} as returned by {@link KeyManager#getInstance}.
 * <p>
 * Like its base class, this class does not impose a certain run time type
 * of the key.
 * It is actually the user interface implementation which determines the run
 * time type of the key provided by {@link #getCreateKey} and
 * {@link #getOpenKey}.
 * Because the user interface implementation is determined by the singleton
 * {@link PromptingKeyManager}, it is ultimately at the discretion of
 * the key manager which type of keys are actually provided by this class.
 * <p>
 * Unlike its base class, instances of this class cannot get shared
 * among multiple protected resources because each instance has a unique
 * {@link #getResourceID() resource identifier} associated with it.
 * Each try to share a key provider of this class among multiple protected
 * resources with the singleton {@link KeyManager} will be prosecuted and
 * sentenced with an {@link IllegalStateException} or, at the discretion of
 * this class, some other {@link RuntimeException}.
 * <p>
 * This class is thread safe.
 *
 * @see PromptingKeyProviderUI
 * @see KeyProvider
 * @see PromptingKeyManager
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PromptingKeyProvider<K extends Cloneable>
        extends AbstractKeyProvider<K> {

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
    private String resourceID;

    private State state = State.RESET;

    private K key;

    /**
     * The user interface instance which is used to prompt the user for a key.
     */
    private PromptingKeyProviderUI<? super PromptingKeyProvider<? super K>> ui;

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    public final synchronized String getResourceID() {
        return resourceID;
    }

    /**
     * Returns the unique resource identifier (resource ID) of the protected
     * resource for which this key provider is used.
     * May be {@code null}.
     */
    final synchronized void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    /**
     * Returns the {@code key} property maintained by this key provider.
     * Client applications should not call this method directly
     * but rather call {@link #getOpenKey} or {@link #getCreateKey}:
     * It's intended to be used by subclasses and user interface classes only.
     *
     * @return The {@code key} property - may be {@code null}.
     */
    public final synchronized K getKey() {
        return key;
    }

    /**
     * Sets the {@code key} property maintained by this key provider.
     * Client applications should not call this method directly:
     * It's intended to be used by subclasses and user interface classes only.
     *
     * @param key The {@code key} property - may be {@code null}.
     */
    public final synchronized void setKey(final K key) {
        this.key = key;
    }

    /**
     * Returns the identifier which is used by the {@link PromptingKeyManager}
     * to look up an instance of the {@link PromptingKeyProviderUI} user
     * interface class which is then used to prompt the user for a key.
     * <p>
     * Subclasses which want to use a custom user interface should overwrite
     * this method to return the name of their respective class as the
     * identifier and provide a custom {@code PromptingKeyManager} which has
     * registered a {@code PromptingKeyProviderUI} class for this identifier.
     * <p>
     * The implementation in this class returns the simple name of this class,
     * {@code PromptingKeyProvider}.
     */
    protected String getUIClassID() {
        return "PromptingKeyProvider";
    }

    private synchronized
    PromptingKeyProviderUI<? super PromptingKeyProvider<? super K>>
    getUI() {
        return ui;
    }

    final synchronized void setUI(
    final PromptingKeyProviderUI<? super PromptingKeyProvider<? super K>> ui) {
        this.ui = ui;
    }

    private synchronized State getState() {
        return state;
    }

    private synchronized void setState(final State state) {
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
        synchronized (lock) {
            return getState().getCreateKey(this);
        }
    }

    /**
     * Prompts for the key which should be used to create a new protected
     * resource or entirely replace the contents of an already existing
     * protected resource.
     */
    private K promptCreateKey() throws UnknownKeyException {
        PromptingKeyManager.ensurePrompting();

        final K oldKey = getKey();
        getUI().promptCreateKey(this);
        wipe(oldKey);

        final K newKey = getKey();
        if (newKey != null) {
            setState(State.KEY_CHANGED);
            return newKey;
        } else {
            setState(State.CANCELLED);
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
    protected final K getOpenKeyImpl() throws UnknownKeyException {
        synchronized (lock) {
            return getState().getOpenKey(this);
        }
    }

    /**
     * Prompts for the key which should be used to open an existing protected
     * resource in order to access its contents.
     */
    private K promptOpenKey(final boolean invalid) throws UnknownKeyException {
        PromptingKeyManager.ensurePrompting();

        final K oldKey = getKey();
        final boolean changeKey = invalid
                ? getUI().promptInvalidOpenKey(this)
                : getUI().promptUnknownOpenKey(this);
        wipe(oldKey);

        final K newKey = getKey();
        if (newKey != null) {
            setState(changeKey ? State.KEY_CHANGE_REQUESTED : State.KEY_PROVIDED);
            return newKey;
        } else {
            setState(State.CANCELLED);
            throw new KeyPromptingCancelledException();
        }
    }

    /**
     * Called to indicate that authentication of the key returned by
     * {@link #getOpenKey()} has failed and to request an entirely different
     * key.
     * The user is prompted for a new key on the next call to
     * {@link #getOpenKey}.
     * Note that the user may actually not be prompted at the next call to
     * {@link #getOpenKey} again if prompting has been disabled by the
     * {@link PromptingKeyManager} or this provider is in a state where
     * calling this method does not make any sense.
     *
     * @see KeyProvider#invalidOpenKey
     */
    protected final void invalidOpenKeyImpl() {
        synchronized (lock) {
            getState().invalidOpenKey(this);
        }
    }

    /**
     * Resets this key provider if and only if prompting for a key has been
     * cancelled.
     * It is safe to call this method while another thread is actually
     * prompting for a key.
     */
    final void resetCancelledPrompt() {
        getState().resetCancelledPrompt(this);
    }

    /**
     * Resets the state of this provider, wipes the key and calls
     * {@link #onReset()}.
     */
    @Override
    public synchronized final void reset() {
        setState(State.RESET);
        wipe(getKey());
        onReset();
    }

    /** If the key is an array, the array is filled with zero values. */
    private static <K> void wipe(final K key) {
        if (key instanceof byte[])
            Arrays.fill((byte[]) key, (byte) 0);
        else if (key instanceof char[])
            Arrays.fill((char[]) key, (char) 0);
        else if (key instanceof short[])
            Arrays.fill((short[]) key, (short) 0);
        else if (key instanceof int[])
            Arrays.fill((int[]) key, 0);
        else if (key instanceof long[])
            Arrays.fill((long[]) key, (long) 0);
        else if (key instanceof float[])
            Arrays.fill((float[]) key, (float) 0);
        else if (key instanceof double[])
            Arrays.fill((double[]) key, (double) 0);
        else if (key instanceof boolean[])
            Arrays.fill((boolean[]) key, false);
        else if (key instanceof Object[])
            Arrays.fill((Object[]) key, null);
    }

    /**
     * This hook is run after {@link #reset()} has been called.
     * This method is called from the constructor in the class
     * {@link AbstractKeyProvider}.
     * The implementation in this class does nothing.
     * May be overwritten by subclasses.
     */
    protected void onReset() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@code PromptingKeyProider} throws an
     * {@link IllegalStateException} if this instance is already mapped for
     * another resource identifier.
     *
     * @throws IllegalStateException If this instance is already mapped for
     *         another resource identifier or mapping is prohibited
     *         by a constraint in a subclass.
     */
    // TODO: Make this redundant: It's not failsafe!
    @Override
    protected synchronized KeyProvider<?> addToKeyManager(final String resourceID)
    throws NullPointerException, IllegalStateException {
        final String oldResourceID = getResourceID();
        if (oldResourceID != null && !resourceID.equals(oldResourceID))
            throw new IllegalStateException(
                    "this provider is used for resource ID \"" + oldResourceID + "\"");
        final KeyProvider provider = super.addToKeyManager(resourceID);
        setResourceID(resourceID);

        return provider;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@code PromptingKeyProider} throws an
     * {@link IllegalStateException} if this instance is already mapped for
     * another resource identifier.
     *
     * @throws IllegalStateException If this instance is already mapped for
     *         another resource identifier or mapping is prohibited
     *         by a constraint in a subclass.
     */
    // TODO: Make this redundant: It's not failsafe!
    @Override
    protected synchronized KeyProvider<?> removeFromKeyManager(final String resourceID)
    throws NullPointerException, IllegalStateException {
        final String oldResourceID = getResourceID();
        if (!resourceID.equals(oldResourceID))
            throw new IllegalStateException(
                    "this provider is used for resource ID \"" + oldResourceID + "\"");
        final KeyProvider provider = super.removeFromKeyManager(resourceID);
        assert provider == null || provider == this : "";
        setResourceID(null);
        return provider;
    }

    //
    // Shared (flyweight) state member classes.
    //

    private abstract static class State {
        static final State RESET = new Reset();
        static final State KEY_INVALIDATED = new KeyInvalidated();
        static final State KEY_PROVIDED = new KeyProvided();
        static final State KEY_CHANGE_REQUESTED = new KeyChangeRequested();
        static final State KEY_CHANGED = new KeyChanged();
        static final State CANCELLED = new Cancelled();

        abstract <K extends Cloneable> K getCreateKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        abstract <K extends Cloneable> K getOpenKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException;

        <K extends Cloneable> void invalidOpenKey(PromptingKeyProvider<K> provider) {
        }

        <K extends Cloneable> void resetCancelledPrompt(PromptingKeyProvider<K> provider) {
        }
    }

    private static class Reset extends State {
        <K extends Cloneable> K getCreateKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            return provider.promptCreateKey();
        }

        <K extends Cloneable> K getOpenKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            return provider.promptOpenKey(false);
        }

        @Override
        <K extends Cloneable> void invalidOpenKey(PromptingKeyProvider<K> provider) {
        }
    }

    private static class KeyInvalidated extends Reset {
        @Override
        <K extends Cloneable> K getOpenKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            return provider.promptOpenKey(true);
        }
    }

    private static class KeyProvided extends State {
        <K extends Cloneable> K getCreateKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            return provider.getKey();
        }

        <K extends Cloneable> K getOpenKey(PromptingKeyProvider<K> provider) {
            return provider.getKey();
        }

        @Override
        <K extends Cloneable> void invalidOpenKey(PromptingKeyProvider<K> provider) {
            provider.setState(KEY_INVALIDATED);
        }
    }

    private static class KeyChangeRequested extends KeyProvided {
        @Override
        <K extends Cloneable> K getCreateKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            return provider.promptCreateKey();
        }
    }

    private static class KeyChanged extends KeyProvided {
        @Override
        <K extends Cloneable> void invalidOpenKey(PromptingKeyProvider<K> provider) {
        }
    }

    private static class Cancelled extends State {
        <K extends Cloneable> K getCreateKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            throw new KeyPromptingCancelledException();
        }

        <K extends Cloneable> K getOpenKey(PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            throw new KeyPromptingCancelledException();
        }

        @Override
        <K extends Cloneable> void invalidOpenKey(PromptingKeyProvider<K> provider) {
        }

        @Override
        <K extends Cloneable> void resetCancelledPrompt(PromptingKeyProvider<K> provider) {
            provider.reset();
        }
    }

    private static class PromptingLock { }
}
