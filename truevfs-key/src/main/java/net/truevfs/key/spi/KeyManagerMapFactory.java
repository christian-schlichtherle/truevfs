/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.key.spi;

import de.schlichtherle.truecommons.services.FactoryService;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.key.KeyManager;

/**
 * A service for creating maps of classes to key managers.
 * Note that you can't subclass this class for customization.
 * It solely exists in order to support the 
 * {@link KeyManagerMapLocator#SINGLETON}, which will use it to create the root
 * of the driver map which gets subsequently decorated by the
 * {@link KeyManagerMapModifier} implementations found on the class path.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class KeyManagerMapFactory
extends FactoryService<Map<Class<?>, KeyManager<?>>> {

    /**
     * Returns a new empty map for subsequent modification.
     *
     * @return A new empty map for subsequent modification.
     */
    @Override
    public Map<Class<?>, KeyManager<?>> apply() {
        return new LinkedHashMap<>(32);
    }
}
