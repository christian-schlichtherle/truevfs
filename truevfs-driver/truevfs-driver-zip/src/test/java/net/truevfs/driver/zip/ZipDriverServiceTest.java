/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.spec.FsDriverProvider;
import net.truevfs.kernel.spec.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class ZipDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getExtensions() {
        return "zip|ear|jar|war";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new ZipDriverService();
    }
}
