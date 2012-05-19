/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.path;

import net.truevfs.driver.tar.TarBZip2Driver;
import net.truevfs.kernel.cio.IOPool;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * @author  Christian Schlichtherle
 */
public final class TarBZip2PathIT extends TarPathITSuite<TarBZip2Driver> {

    @Override
    protected String getExtensionList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        class TestDriver extends TarBZip2Driver {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }

            @Override
            public int getLevel() {
                return BZip2CompressorOutputStream.MIN_BLOCKSIZE;
            }
        } // TestDriver

        return new TestDriver();
    }
}