/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import java.util.ServiceConfigurationError;
import net.java.truecommons3.shed.ImplementationsShouldExtend;

/**
 * A map of key classes to key managers.
 * When implementing a key manager map, you should extend the
 * {@link AbstractKeyManagerMap} class rather than directly implementing this
 * interface in order to maintain binary backwards compatibility even if this
 * interface is changed.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @since TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ImplementationsShouldExtend(AbstractKeyManagerMap.class)
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
