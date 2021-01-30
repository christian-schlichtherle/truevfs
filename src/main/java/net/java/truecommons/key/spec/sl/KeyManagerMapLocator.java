/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.sl;

import java.util.Collections;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.key.spec.AbstractKeyManagerMap;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.spi.KeyManagerMapFactory;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;
import net.java.truecommons.services.ServiceLocator;

/**
 * A container of the singleton immutable map of all known file system schemes
 * to file system drivers.
 * The map is populated by using a {@link ServiceLocator} to search for advertised
 * implementations of the factory service specification class
 * {@link KeyManagerMapFactory}
 * and the modifier service specification class
 * {@link KeyManagerMapModifier}.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
public final class KeyManagerMapLocator extends AbstractKeyManagerMap {

    /** The singleton instance of this class. */
    public static final KeyManagerMapLocator
            SINGLETON = new KeyManagerMapLocator();

    private KeyManagerMapLocator() { }

    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return Lazy.managers;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final Map<Class<?>, KeyManager<?>> managers
                = Collections.unmodifiableMap(
                    new ServiceLocator(KeyManagerMapLocator.class)
                    .factory(KeyManagerMapFactory.class, KeyManagerMapModifier.class)
                    .get());
    }
}
