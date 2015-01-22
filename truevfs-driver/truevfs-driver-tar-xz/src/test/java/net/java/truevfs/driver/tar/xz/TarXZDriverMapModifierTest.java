/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.xz;

import net.java.truevfs.driver.tar.xz.TarXZDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class TarXZDriverMapModifierTest
extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "tar.xz|txz";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new TarXZDriverMapModifier();
    }
}
