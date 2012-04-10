/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import java.net.URI;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Proxies access to the key for {@link PromptingKeyProviderView}
 * implementations.
 *
 * @param  <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public interface PromptingKeyProviderController<K extends SafeKey<K>> {

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
     * Returns the protected resource's key.
     *
     * @return The protected resource's key.
     * @throws IllegalStateException if getting key is not legal in the
     *         current state.
     */
    @CheckForNull K getKey();

    /**
     * Sets the protected resource's key to a clone of the given key.
     *
     * @param  key The key to clone to use for the protected resource.
     * @throws IllegalStateException if setting key is not legal in the
     *         current state.
     */
    void setKey(@CheckForNull K key);

    /**
     * Requests to prompt the user for a new key upon the next call to
     * {@link PromptingKeyProvider#getWriteKey()}, provided that the key is
     * {@link PromptingKeyProvider#setKey set} by then.
     *
     * @param  changeRequested whether or not the user shall get prompted
     *         for a new key upon the next call to
     *         {@link PromptingKeyProvider#getWriteKey()}, provided that the
     *         key is {@link PromptingKeyProvider#setKey set} then.
     * @throws IllegalStateException if setting this property is illegal in the
     *         current state.
     */
    void setChangeRequested(boolean changeRequested);
}