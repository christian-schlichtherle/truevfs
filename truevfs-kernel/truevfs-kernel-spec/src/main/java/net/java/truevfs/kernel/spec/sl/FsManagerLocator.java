/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.services.Container;
import net.java.truecommons.services.ServiceLocator;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;
import net.java.truevfs.kernel.spec.spi.FsManagerFactory;

/**
 * A container of the singleton file system manager.
 * The file system manager is created by using a {@link ServiceLocator} to search for
 * advertised implementations of the factory service specification class
 * {@link FsManagerFactory}
 * and the decorator service specification class
 * {@link FsManagerDecorator}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsManagerLocator implements Container<FsManager> {

    /** The singleton instance of this class. */
    public static final FsManagerLocator SINGLETON = new FsManagerLocator();

    private FsManagerLocator() { }

    @Override
    public FsManager get() { return Lazy.manager; }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final FsManager manager
                = new ServiceLocator(FsManagerLocator.class)
                .factory(FsManagerFactory.class, FsManagerDecorator.class)
                .get();
    }
}
