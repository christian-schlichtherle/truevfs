/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import java.net.URI;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.AbstractKeyProvider;
import net.java.truevfs.key.spec.PersistentUnknownKeyException;
import net.java.truevfs.key.spec.UnknownKeyException;
import net.java.truevfs.key.spec.prompting.PromptingKey.Controller;
import net.java.truevfs.key.spec.prompting.PromptingKey.View;

/**
 * A key provider which prompts the user for a key for its protected resource.
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
@SuppressWarnings("PackageVisibleInnerClass")
public final class PromptingKeyProvider<K extends PromptingKey<K>>
extends AbstractKeyProvider<K> {

    private final PromptingKeyManager<K> manager;
    private final URI resource;
    private final SharedKeyProvider<K> provider;

    PromptingKeyProvider(
            final PromptingKeyManager<K> manager,
            final URI resource,
            final SharedKeyProvider<K> provider) {
        assert null != manager;
        assert null != resource;
        assert null != provider;
        this.manager = manager;
        this.resource = resource;
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

    K getKeyClone() { return provider.getKeyClone(); }

    void setKeyClone(@CheckForNull K key) { provider.setKeyClone(key); }

    @Override
    public void setKey(@CheckForNull K key) { setKeyClone(key); }

    @CheckForNull PersistentUnknownKeyException getException() {
        return provider.getException();
    }

    void setException(@CheckForNull PersistentUnknownKeyException exception) {
        provider.setException(exception);
    }

    /**
     * Resets the state of this key provider
     * if and only if prompting for a key has been cancelled.
     */
    public void resetCancelledKey() { provider.resetCancelledKey(); }

    /**
     * Resets the state of this key provider
     * unconditionally.
     */
    public void resetUnconditionally() { provider.reset(); }

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
    private abstract class BaseController implements Controller<K> {

        @Override
        public final URI getResource() { return resource; }

        @Override
        public final void setKeyClone(@CheckForNull K key) {
            provider.setKeyClone(key);
        }
    } // BaseController

    /**
     * The controller to use when promting for a key to write a protected
     * resource.
     */
    @NotThreadSafe
    private final class WriteController extends BaseController {
        @Override
        public K getKeyClone() { return provider.getKeyClone(); }
    } // WriteController

    /**
     * The controller to use when promting for a key to read a protected
     * resource.
     */
    @NotThreadSafe
    private final class ReadController extends BaseController {
        @Override
        public K getKeyClone() { throw new IllegalStateException(); }
    } // ReadController
}
