/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl;

import global.namespace.service.wight.core.ServiceLocator;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapFactory;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A supplier of the singleton immutable map of all known file system schemes to file system drivers.
 * The map is populated by using a {@link ServiceLocator} to search for published implementations of the factory
 * service interface {@link FsDriverMapFactory} and the modifier service interface {@link FsDriverMapModifier}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsDriverMapLocator implements Supplier<Map<FsScheme, FsDriver>> {

    /**
     * The singleton instance of this class.
     */
    public static final FsDriverMapLocator SINGLETON = new FsDriverMapLocator();

    private FsDriverMapLocator() {
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return Lazy.drivers;
    }

    private static final class Lazy {
        static final Map<FsScheme, FsDriver> drivers = Collections.unmodifiableMap(
                new ServiceLocator().provider(FsDriverMapFactory.class, FsDriverMapModifier.class).get());
    }
}
