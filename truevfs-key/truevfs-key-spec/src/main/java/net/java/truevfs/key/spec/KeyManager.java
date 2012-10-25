/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A manager of providers of secret keys for accessing protected resources.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param  <K> The type of the secret keys.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface KeyManager<K> {

    /**
     * Returns a key provider for accessing the identified protected resource.
     *
     * @param  resource the URI of the protected resource.
     */
    KeyProvider<K> access(URI resource);

    /**
     * Notifies this key manager that an {@linkplain #access accessed} resource
     * has changed its URI.
     *
     * @param oldResource the old URI of the protected resource.
     * @param newResource the new URI of the protected resource.
     */
    void move(URI oldResource, URI newResource);

    /**
     * Notifies this key manager that an {@linkplain #access accessed} resource
     * has been deleted.
     * This implies {@link #release}.
     *
     * @param resource the URI of the protected resource.
     */
    void delete(URI resource);

    /**
     * Notifies this key manager that an {@linkplain #access accessed} resource
     * has been released.
     * The implementation needs to consider that another access-release cycle
     * may start again later.
     *
     * @param resource the URI of the protected resource.
     */
    void release(URI resource);
}
