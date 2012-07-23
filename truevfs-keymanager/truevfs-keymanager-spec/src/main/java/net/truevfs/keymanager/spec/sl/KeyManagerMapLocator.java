/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.spec.sl;

import de.schlichtherle.truecommons.services.Locator;
import java.util.Collections;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.keymanager.spec.AbstractKeyManagerContainer;
import net.truevfs.keymanager.spec.KeyManager;
import net.truevfs.keymanager.spec.spi.KeyManagerMapFactory;
import net.truevfs.keymanager.spec.spi.KeyManagerMapModifier;

/**
 * A container of the singleton immutable map of all known file system schemes
 * to file system drivers.
 * The map is populated by using a {@link Locator} to search for advertised
 * implementations of the factory service specification class
 * {@link KeyManagerMapFactory}
 * and the modifier service specification class
 * {@link KeyManagerMapModifier}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class KeyManagerMapLocator extends AbstractKeyManagerContainer {

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
                    new Locator(KeyManagerMapLocator.class)
                    .factory(KeyManagerMapFactory.class, KeyManagerMapModifier.class)
                    .get());
    }
}
