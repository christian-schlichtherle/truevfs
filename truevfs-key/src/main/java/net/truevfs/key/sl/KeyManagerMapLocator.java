/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.key.sl;

import de.schlichtherle.truecommons.services.Locator;
import java.util.Collections;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.key.AbstractKeyManagerProvider;
import net.truevfs.key.KeyManager;
import net.truevfs.key.spi.KeyManagerMapFactory;
import net.truevfs.key.spi.KeyManagerMapModifier;

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
public final class KeyManagerMapLocator extends AbstractKeyManagerProvider {

    /** The singleton instance of this class. */
    public static final KeyManagerMapLocator SINGLETON = new KeyManagerMapLocator();

    private KeyManagerMapLocator() { }

    @Override
    public Map<Class<?>, KeyManager<?>> getKeyManagers() {
        return Boot.managers;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<Class<?>, KeyManager<?>> managers
                = Collections.unmodifiableMap(
                    new Locator(KeyManagerMapLocator.class)
                    .factory(KeyManagerMapFactory.class, KeyManagerMapModifier.class)
                    .apply());
    }
}
