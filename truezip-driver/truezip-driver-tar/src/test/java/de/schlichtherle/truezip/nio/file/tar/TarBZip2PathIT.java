/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.file.tar;

import de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * @author  Christian Schlichtherle
 */
public final class TarBZip2PathIT extends TarPathTestSuite<TarBZip2Driver> {

    @Override
    protected String getSuffixList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        class TestDriver extends TarBZip2Driver {
            TestDriver() {
                super(getTestConfig().getIOPoolProvider());
            }

            @Override
            public int getLevel() {
                return BZip2CompressorOutputStream.MIN_BLOCKSIZE;
            }
        } // TestDriver

        return new TestDriver();
    }
}