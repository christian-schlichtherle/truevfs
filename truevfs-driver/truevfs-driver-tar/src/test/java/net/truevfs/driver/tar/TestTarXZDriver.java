/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import org.tukaani.xz.LZMA2Options;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarXZDriver extends TarXZDriver {

    @Override
    public IoBufferPool<? extends IoBuffer<?>> getIoBufferPool() {
        return TestConfig.get().getIoBufferPool();
    }

    @Override
    public int getPreset() {
        return LZMA2Options.PRESET_MIN;
    }
}
