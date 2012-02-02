/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import java.util.ServiceConfigurationError;

/**
 * A service for key managers for secret key classes.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface KeyManagerProvider {

    /**
     * Returns the key manager for the given secret key class.
     * Subsequent calls must return the same key manager for the same secret
     * key class.
     *
     * @param  <K> the type of the secret key class.
     * @param  type the class for the secret key type.
     * @return the key manager for the given secret key class.
     * @throws ServiceConfigurationError if no appropriate key manager is
     *         available.
     */
    <K> KeyManager<K> get(Class<K> type);
}
