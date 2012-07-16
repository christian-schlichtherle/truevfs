/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sl;

import de.schlichtherle.truecommons.services.Container;
import de.schlichtherle.truecommons.services.Locator;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.spi.FsManagerDecorator;
import net.truevfs.kernel.spec.spi.FsManagerFactory;

/**
 * Uses a {@link Locator} to resolve the singleton file system manager from
 * instances of the factory service class {@link FsManagerFactory} and the
 * decorator service class {@link FsManagerDecorator}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public class FsManagerLocator implements Container<FsManager> {

    /** The singleton instance of this class. */
    public static final FsManagerLocator SINGLETON = new FsManagerLocator();

    private FsManagerLocator() { }

    @Override
    public FsManager apply() { return Boot.manager; }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final FsManager manager = new Locator(FsManagerLocator.class)
                .factory(FsManagerFactory.class, FsManagerDecorator.class)
                .apply();
    }
}
