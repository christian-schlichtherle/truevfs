/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.test.TestConfig;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarBZip2Driver extends TarBZip2Driver {

    public TestTarBZip2Driver() {
        super(TestConfig.get().getIOPoolProvider());
    }

    @Override
    public int getLevel() {
        return BZip2CompressorOutputStream.MIN_BLOCKSIZE;
    }
}
