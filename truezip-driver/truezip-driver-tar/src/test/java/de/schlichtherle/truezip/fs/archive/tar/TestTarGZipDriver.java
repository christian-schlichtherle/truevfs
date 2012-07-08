/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.test.TestConfig;
import java.util.zip.Deflater;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarGZipDriver extends TarGZipDriver {

    public TestTarGZipDriver() {
        super(TestConfig.get().getIOPoolProvider());
    }

    @Override
    public int getLevel() {
        return Deflater.BEST_SPEED;
    }
}
