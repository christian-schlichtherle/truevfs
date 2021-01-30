/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import java.net.URI;
import net.java.truecommons3.shed.ImplementationsShouldExtend;

/**
 * Manages the life cycle of key providers for writing and reading protected
 * resources.
 * When implementing a key manager, you should extend the
 * {@link AbstractKeyManager} class rather than directly implementing this
 * interface in order to maintain binary backwards compatibility even if this
 * interface is changed.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param <K> The type of the keys.
 * @see KeyManagerMap
 * @since TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ImplementationsShouldExtend(AbstractKeyManager.class)
public interface KeyManager<K> {

    /**
     * Returns a key provider for accessing the identified protected resource.
     *
     * @param  uri the URI of the protected resource.
     * @return a consistent (but not necessarily always the same) key provider
     *         for accessing the identified protected resource.
     */
    KeyProvider<K> provider(URI uri);

    /**
     * Notifies this key manager that a protected resource has been released.
     * Clients should call this method only if they wish to reset or dispose
     * the key provider which may be associated with the protected resource.
     * Implementations may completely ignore this call or chose to reset or
     * dispose the associated key provider based on its state.
     * For example, an implementation may reset the key provider only if the
     * user had cancelled the prompting for the key.
     *
     * @param uri the URI of the protected resource.
     */
    void release(URI uri);

    /**
     * Notifies this key manager that a protected resource has been linked.
     *
     * @param originUri the origin URI of the protected resource.
     * @param targetUri the target URI of the protected resource.
     */
    void link(URI originUri, URI targetUri);

    /**
     * Notifies this key manager that a protected resource has been unlinked.
     * This implies a {@link #release}.
     *
     * @param uri the URI of the protected resource.
     */
    void unlink(URI uri);
}
