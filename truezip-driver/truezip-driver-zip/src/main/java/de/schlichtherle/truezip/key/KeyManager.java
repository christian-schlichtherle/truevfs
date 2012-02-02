/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import net.jcip.annotations.ThreadSafe;

/**
 * A container for key providers for reading and writing protected resources.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param   <K> The type of the secret keys.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public interface KeyManager<K> {

    /**
     * Returns the mapped key provider for the given protected resource.
     * If no key provider is mapped yet, a new key provider is created and
     * returned.
     *
     * @param  resource the URI of the protected resource.
     * @return The key provider mapped for the protected resource.
     */
    KeyProvider<K> getKeyProvider(URI resource);

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
    @Nullable KeyProvider<K> moveKeyProvider(URI oldResource, URI newResource);

    /**
     * Removes the mapped key provider for the given protected resource.
     * It is an error to use the returned key provider.
     *
     * @param  resource the URI of the protected resource.
     * @return The key provider which was previously mapped for the protected
     *         resource.
     */
    @CheckForNull KeyProvider<K> removeKeyProvider(URI resource);

    /**
     * Returns a priority to help the key manager service locator.
     * The greater number wins!
     * The default value should be {@code 0}.
     * 
     * @return A priority to help the key manager service locator.
     */
    public int getPriority();
}
