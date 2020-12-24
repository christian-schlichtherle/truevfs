/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerFactory;

/**
 * Creates a default file system manager.
 *
 * @author Christian Schlichtherle
 * @deprecated This class is reserved for exclusive use by the {@link net.java.truevfs.kernel.spec.sl.FsManagerLocator}
 * singleton!
 */
@Deprecated
public final class DefaultManagerFactory extends FsManagerFactory {

    @Override
    public FsManager get() {
        return new DefaultManager();
    }

    /**
     * @return {@code -100}.
     */
    @Override
    public int getPriority() {
        return -100;
    }
}
