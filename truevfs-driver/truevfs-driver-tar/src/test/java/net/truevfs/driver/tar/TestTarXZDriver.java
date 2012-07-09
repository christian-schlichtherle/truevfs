/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoPool;
import org.tukaani.xz.LZMA2Options;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarXZDriver extends TarXZDriver {

    @Override
    public IoPool<?> getIoPool() {
        return TestConfig.get().getIoPoolProvider().getIoPool();
    }

    @Override
    public int getPreset() {
        return LZMA2Options.PRESET_MIN;
    }
}
