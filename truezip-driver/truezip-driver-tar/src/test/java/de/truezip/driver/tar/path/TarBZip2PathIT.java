/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.path;

import de.truezip.driver.tar.TarBZip2Driver;
import de.truezip.kernel.cio.IOPool;
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
                return getTestConfig().getIOPoolProvider().get();
            }

            @Override
            public int getLevel() {
                return BZip2CompressorOutputStream.MIN_BLOCKSIZE;
            }
        } // TestDriver

        return new TestDriver();
    }
}