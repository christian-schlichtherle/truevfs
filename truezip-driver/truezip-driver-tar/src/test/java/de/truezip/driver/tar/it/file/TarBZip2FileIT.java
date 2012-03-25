/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.it.file;

import de.truezip.driver.tar.TarBZip2Driver;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2FileIT extends TarFileITSuite<TarBZip2Driver> {

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
