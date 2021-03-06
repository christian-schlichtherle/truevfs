/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.bzip2;

import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truecommons.cio.IoBufferPool;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarBZip2Driver extends TarBZip2Driver {

    @Override
    public IoBufferPool getPool() {
        return FsTestConfig.get().getPool();
    }

    @Override
    public int getLevel() {
        return BZip2CompressorOutputStream.MIN_BLOCKSIZE;
    }
}
