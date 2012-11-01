/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.net.URI;

/**
 * Manages the life cycle of key providers for accessing protected resources.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param  <K> The type of the keys.
 * @author Christian Schlichtherle
 */
public interface KeyManager<K> {

    /**
     * Returns a key provider for accessing the identified protected resource.
     *
     * @param  resource the URI of the protected resource.
     */
    KeyProvider<K> provider(URI resource);

    /**
     * Notifies this key manager that a protected resource has moved.
     *
     * @param oldResource the old URI of the protected resource.
     * @param newResource the new URI of the protected resource.
     */
    void move(URI oldResource, URI newResource);

    /**
     * Notifies this key manager that a protected resource has been deleted.
     * This implies {@link #release}.
     *
     * @param resource the URI of the protected resource.
     */
    void delete(URI resource);

    /**
     * Notifies this key manager that a protected resource has been released.
     * Clients should call this method only if they wish to reset or dispose
     * the key provider which may be associated with the protected resource.
     * Implementations may completely ignore this call or chose to reset or
     * dispose the associated key provider based on its state.
     * For example, an implementation may reset the key provider only if the
     * user had cancelled the prompting for the key.
     *
     * @param resource the URI of the protected resource.
     */
    void release(URI resource);
}
