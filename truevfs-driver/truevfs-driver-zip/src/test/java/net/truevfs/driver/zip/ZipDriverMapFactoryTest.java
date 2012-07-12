/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.spec.FsDriverMapProvider;
import net.truevfs.kernel.spec.spi.FsDriverMapFactoryTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class ZipDriverMapFactoryTest extends FsDriverMapFactoryTestSuite {
    @Override
    protected String getExtensions() {
        return "zip";
    }

    @Override
    protected FsDriverMapProvider newDriverProvider() {
        return new ZipDriverMapFactory();
    }
}
