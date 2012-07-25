/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.bzip2;

import net.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class TarBZip2DriverMapModifierTest
extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "tar.bz2|tar.bzip2|tb2|tbz|tbz2";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new TarBZip2DriverMapModifier();
    }
}
