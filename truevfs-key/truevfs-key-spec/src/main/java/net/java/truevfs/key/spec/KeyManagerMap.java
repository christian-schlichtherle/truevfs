/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.util.ServiceConfigurationError;

/**
 * A map of key classes to key managers.
 *
 * @author Christian Schlichtherle
 */
public interface KeyManagerMap {

    /**
     * Returns the key manager for the given secret key class.
     * <p>
     * This is a pure function - multiple calls must return the same value for
     * the same parameter.
     *
     * @param  <K> the type of the keys.
     * @param  type the class for the key type.
     * @return the key manager for the key class.
     * @throws ServiceConfigurationError if no appropriate key manager is
     *         available.
     */
    <K> KeyManager<K> manager(Class<K> type);
}
