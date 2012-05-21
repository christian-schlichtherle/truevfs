/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.it;

import net.truevfs.driver.tar.TarDriver;
import net.truevfs.kernel.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarPathIT extends TarPathITSuite<TarDriver> {

    @Override
    protected String getExtensionList() {
        return "tar";
    }

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver() {
            @Override
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}