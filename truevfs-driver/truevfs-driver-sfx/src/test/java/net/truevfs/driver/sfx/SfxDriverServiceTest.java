/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.sfx;

import net.truevfs.kernel.spec.FsDriverProvider;
import net.truevfs.kernel.spec.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class SfxDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getExtensions() {
        return "exe";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new SfxDriverService();
    }
}
