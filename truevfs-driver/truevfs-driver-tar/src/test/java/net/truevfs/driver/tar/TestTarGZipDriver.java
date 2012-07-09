/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import java.util.zip.Deflater;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoPool;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarGZipDriver extends TarGZipDriver {

    @Override
    public IoPool<?> getIoPool() {
        return TestConfig.get().getIoPoolProvider().getIoPool();
    }

    @Override
    public int getLevel() {
        return Deflater.BEST_SPEED;
    }
}
