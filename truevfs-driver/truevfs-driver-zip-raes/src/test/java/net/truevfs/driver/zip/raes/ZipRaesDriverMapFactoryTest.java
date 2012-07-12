/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes;

import net.truevfs.kernel.spec.FsDriverMapProvider;
import net.truevfs.kernel.spec.spi.FsDriverMapFactoryTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class ZipRaesDriverMapFactoryTest extends FsDriverMapFactoryTestSuite {

    @Override
    protected String getExtensions() {
        return "tzp|zip.rae|zip.raes";
    }

    @Override
    protected FsDriverMapProvider newDriverProvider() {
        return new ZipRaesDriverMapFactory();
    }
}
