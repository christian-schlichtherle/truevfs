/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.sl;

import global.namespace.service.wight.core.ServiceLocator;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.spi.FsManagerDecorator;
import global.namespace.truevfs.kernel.api.spi.FsManagerFactory;

import java.util.function.Supplier;

/**
 * A supplier of the singleton file system manager.
 * The file system manager is created by using a {@link ServiceLocator} to search for published implementations of the
 * factory service interface {@link FsManagerFactory} and the decorator service interface {@link FsManagerDecorator}.
 *
 * @author Christian Schlichtherle
 */
public final class FsManagerLocator implements Supplier<FsManager> {

    /**
     * The singleton instance of this class.
     */
    public static final FsManagerLocator SINGLETON = new FsManagerLocator();

    private FsManagerLocator() {
    }

    @Override
    public FsManager get() {
        return Lazy.manager;
    }

    private static final class Lazy {
        static final FsManager manager =
                new ServiceLocator().provider(FsManagerFactory.class, FsManagerDecorator.class).get();
    }
}
