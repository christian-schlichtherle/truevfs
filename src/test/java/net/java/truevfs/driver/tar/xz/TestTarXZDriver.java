/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.xz;

import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truecommons.cio.IoBufferPool;
import org.tukaani.xz.LZMA2Options;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarXZDriver extends TarXZDriver {
    @Override
    public IoBufferPool getPool() {
        return FsTestConfig.get().getPool();
    }

    @Override
    public int getPreset() {
        return LZMA2Options.PRESET_MIN;
    }
}
