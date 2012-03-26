/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key;

import java.net.URI;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A container for key providers for reading and writing protected resources.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param   <K> The type of the secret keys.
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public abstract class KeyManager<K> {

    /**
     * Returns the mapped key provider for the given protected resource or
     * {@code null} if no key provider is mapped yet.
     *
     * @param  resource the URI of the protected resource.
     * @return The mapped key provider for the given protected resource or
     *         {@code null} if no key provider is mapped yet.
     */
    @CheckForNull public abstract KeyProvider<K> get(URI resource);

    /**
     * Returns the mapped key provider for the given protected resource.
     * If no key provider is mapped yet, then a new key provider gets created
     * and returned.
     *
     * @param  resource the URI of the protected resource.
     * @return The mapped key provider for the given protected resource.
     */
    public abstract KeyProvider<K> make(URI resource);

    /**
     * Moves the mapped key provider from the URI {@code oldResource} to
     * {@code newResource}.
     * 
     * @param  oldResource the old URI of the protected resource.
     * @param  newResource the new URI of the protected resource.
     * @return The key provider which was previously mapped for the protected
     *         resource {@code newResource}.
     * @throws IllegalArgumentException if {@code oldResource} compares
     *         {@link URI#equals(Object) equal} to {@code newResource}.
     */
    @Nullable public abstract KeyProvider<K>
    move(URI oldResource, URI newResource);

    /**
     * Deletes the mapped key provider for the given protected resource.
     * It is an error to use the returned key provider.
     *
     * @param  resource the URI of the protected resource.
     * @return The key provider which was previously mapped for the protected
     *         resource.
     */
    @CheckForNull public abstract KeyProvider<K>
    delete(URI resource);

    /**
     * If and only if this key manager prompts users for keys, e.g. a password,
     * <em>and</em> prompting for the key for the given protected resources had
     * been cancelled by the user, then the key provider shall get reset to a
     * state so that the user will get prompted again the next time a key is
     * requested from the provider.
     * Otherwise, this method shall have no effect.
     * <p>
     * This method is typically used once a file system has been synced in
     * order to give the user a chance to enter the key the next time the file
     * system will get accessed again.
     * Otherwise the key provider would forever stay in its cancelled state.
     * 
     * @param resource 
     */
    public abstract void unlock(URI resource);

    /**
     * Returns a priority to help the key manager service locator.
     * The greater number wins!
     * 
     * @return {@code 0}, as by the implementation in the class
     *         {@link KeyManager}.
     */
    public int getPriority() {
        return 0;
    }
}