/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.prompting;

import net.java.truecommons.key.spec.AbstractKeyProvider;
import net.java.truecommons.key.spec.PersistentUnknownKeyException;
import net.java.truecommons.key.spec.UnknownKeyException;
import net.java.truecommons.key.spec.prompting.PromptingKey.Controller;
import net.java.truecommons.key.spec.prompting.PromptingKey.View;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;

/**
 * A key provider which prompts the user for a key for its protected resource.
 * The user is prompted via an instance of the {@link View} interface.
 * The view can then use the given {@link Controller} instance to ask for the
 * URI of the protected resource and get/set the secret key appropriately.
 *
 * @param  <K> the type of the prompting keys.
 * @see    PromptingKeyManager
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class PromptingKeyProvider<K extends PromptingKey<K>>
extends AbstractKeyProvider<K> {

    private final PromptingKeyManager<K> manager;
    private final URI uri;
    private final SharedKeyProvider<K> provider;

    PromptingKeyProvider(
            final PromptingKeyManager<K> manager,
            final URI uri,
            final SharedKeyProvider<K> provider) {
        this.manager = manager;
        this.uri = uri;
        this.provider = provider;
    }

    @Override
    public K getKeyForWriting() throws UnknownKeyException {
        return provider.getKeyCloneForWriting(this);
    }

    @Override
    public K getKeyForReading(boolean invalid) throws UnknownKeyException {
        return provider.getKeyCloneForReading(this, invalid);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link PromptingKeyProvider} clones the
     * given nullable key before setting it.
     */
    @Override
    public void setKey(@Nullable K key) { provider.setKeyClone(key); }

    @Nullable PersistentUnknownKeyException getException() {
        return provider.getException();
    }

    void setException(@Nullable PersistentUnknownKeyException exception) {
        provider.setException(exception);
    }

    /**
     * Resets the state of this key provider
     * if and only if prompting for the key has been cancelled.
     */
    void resetCancelledKey() { provider.resetCancelledKey(); }

    /**
     * Resets the state of this key provider
     * unconditionally.
     */
    void resetUnconditionally() { provider.resetUnconditionally(); }

    private View<K> getView() { return manager.getView(); }

    void promptKeyForWriting() throws UnknownKeyException {
        getView().promptKeyForWriting(new WriteController());
    }

    void promptKeyForReading(boolean invalid) throws UnknownKeyException {
        getView().promptKeyForReading(new ReadController(), invalid);
    }

    boolean isChangeRequested() { return provider.isChangeRequested(); }

    /**
     * Proxies access to the key for {@link View} implementations.
     */
    @NotThreadSafe
    private abstract class AbstractController implements Controller<K> {

        @Override
        public final URI getResource() { return uri; }

        @Override
        public final void setKeyClone(@Nullable K key) {
            provider.setKeyClone(key);
        }
    }

    /**
     * The controller to use when prompting for a key to write a protected
     * resource.
     */
    @NotThreadSafe
    private final class WriteController extends AbstractController {

        @Override
        public K getKeyClone() { return provider.getKeyClone(); }
    }

    /**
     * The controller to use when prompting for a key to read a protected
     * resource.
     */
    @NotThreadSafe
    private final class ReadController extends AbstractController {

        @Override
        public K getKeyClone() { throw new IllegalStateException(); }
    }
}
