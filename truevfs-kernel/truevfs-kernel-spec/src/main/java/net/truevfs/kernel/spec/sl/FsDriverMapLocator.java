/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sl;

import de.schlichtherle.truecommons.services.Container;
import de.schlichtherle.truecommons.services.Locator;
import java.util.Collections;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.spi.FsDriverMapFactory;
import net.truevfs.kernel.spec.spi.FsDriverMapModifier;

/**
 * A container of the singleton immutable map of all known file system schemes
 * to file system drivers.
 * The map is populated by using a {@link Locator} to search for advertised
 * implementations of the factory service specification class
 * {@link FsDriverMapFactory}
 * and the modifier service specification class
 * {@link FsDriverMapModifier}.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsDriverMapLocator
implements Container<Map<FsScheme, FsDriver>> {

    /** The singleton instance of this class. */
    public static final FsDriverMapLocator SINGLETON = new FsDriverMapLocator();

    private FsDriverMapLocator() { }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return Boot.drivers;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<FsScheme, FsDriver> drivers
                = Collections.unmodifiableMap(
                    new Locator(FsDriverMapLocator.class)
                    .factory(FsDriverMapFactory.class, FsDriverMapModifier.class)
                    .get());
    }
}
