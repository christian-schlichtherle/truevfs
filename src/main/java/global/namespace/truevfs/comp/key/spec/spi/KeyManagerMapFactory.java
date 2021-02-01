/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.spec.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import global.namespace.truevfs.comp.key.spec.KeyManager;
import global.namespace.truevfs.comp.key.spec.sl.KeyManagerMapLocator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A service for creating maps of classes to nullable key managers.
 * Note that this class solely exists in order to support the
 * {@link KeyManagerMapLocator#SINGLETON}, which will use it to create the
 * manager map and subsequently modify it by the
 * {@link KeyManagerMapModifier} implementations found on the class path.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
@ServiceImplementation(priority = -100)
public class KeyManagerMapFactory implements Supplier<Map<Class<?>, KeyManager<?>>> {

    /**
     * Returns a new empty map for subsequent modification.
     *
     * @return A new empty map for subsequent modification.
     */
    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return new HashMap<>(32);
    }
}
