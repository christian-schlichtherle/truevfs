/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.test.TestConfig;
import org.tukaani.xz.LZMA2Options;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarXZDriver extends TarXZDriver {

    public TestTarXZDriver() {
        super(TestConfig.get().getIOPoolProvider());
    }

    @Override
    public int getPreset() {
        return LZMA2Options.PRESET_MIN;
    }
}
