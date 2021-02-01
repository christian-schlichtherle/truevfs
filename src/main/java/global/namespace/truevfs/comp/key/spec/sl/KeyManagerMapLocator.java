/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.spec.sl;

import global.namespace.service.wight.core.ServiceLocator;
import global.namespace.truevfs.comp.key.spec.AbstractKeyManagerMap;
import global.namespace.truevfs.comp.key.spec.KeyManager;
import global.namespace.truevfs.comp.key.spec.spi.KeyManagerMapFactory;
import global.namespace.truevfs.comp.key.spec.spi.KeyManagerMapModifier;

import java.util.Collections;
import java.util.Map;

/**
 * A container of the singleton immutable map of all known file system schemes
 * to file system drivers.
 * The map is populated by using a {@link ServiceLocator} to search for advertised
 * implementations of the factory service specification class
 * {@link KeyManagerMapFactory}
 * and the modifier service specification class
 * {@link KeyManagerMapModifier}.
 *
 * @author Christian Schlichtherle
 */
public final class KeyManagerMapLocator extends AbstractKeyManagerMap {

    /**
     * The singleton instance of this class.
     */
    public static final KeyManagerMapLocator
            SINGLETON = new KeyManagerMapLocator();

    private KeyManagerMapLocator() {
    }

    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return Lazy.managers;
    }

    /**
     * A static data utility class used for lazy initialization.
     */
    private static final class Lazy {
        static final Map<Class<?>, KeyManager<?>> managers
                = Collections.unmodifiableMap(
                new ServiceLocator().provider(KeyManagerMapFactory.class, KeyManagerMapModifier.class).get());
    }
}
