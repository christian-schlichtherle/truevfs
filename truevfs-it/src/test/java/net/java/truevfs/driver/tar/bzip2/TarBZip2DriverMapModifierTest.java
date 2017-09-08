/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.bzip2;

import net.java.truevfs.driver.tar.bzip2.TarBZip2DriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

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
