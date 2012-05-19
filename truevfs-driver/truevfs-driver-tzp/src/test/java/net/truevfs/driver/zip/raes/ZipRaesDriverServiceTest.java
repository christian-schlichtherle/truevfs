/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes;

import net.truevfs.kernel.FsDriverProvider;
import net.truevfs.kernel.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class ZipRaesDriverServiceTest extends FsDriverServiceTestSuite {

    @Override
    protected String getExtensions() {
        return "tzp|zip.rae|zip.raes";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new ZipRaesDriverService();
    }
}
