/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import java.util.ServiceConfigurationError;

/**
 * A provider for key managers for secret key classes.
 *
 * @author Christian Schlichtherle
 */
public interface KeyManagerProvider {

    /**
     * Returns the key manager for the given secret key class.
     * <p>
     * This is a pure function - multiple calls must return the same value for
     * the same parameter.
     *
     * @param  <K> the type of the secret keys.
     * @param  type the class for the secret key type.
     * @return the key manager for the given secret key class.
     * @throws ServiceConfigurationError if no appropriate key manager is
     *         available.
     */
    <K> KeyManager<K> keyManager(Class<K> type);
}
