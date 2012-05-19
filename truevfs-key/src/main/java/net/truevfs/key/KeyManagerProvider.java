/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.key;

import javax.annotation.CheckForNull;

/**
 * A provider for key managers for secret key classes.
 *
 * @author Christian Schlichtherle
 */
public interface KeyManagerProvider {

    /**
     * Returns the nullable key manager for the given secret key class.
     * <p>
     * This is a pure function - multiple calls must return the same value for
     * the same parameter.
     *
     * @param  <K> the type of the secret keys.
     * @param  type the class for the secret key type.
     * @return the nullable key manager for the given secret key class.
     */
    @CheckForNull <K> KeyManager<K> keyManager(Class<K> type);
}
